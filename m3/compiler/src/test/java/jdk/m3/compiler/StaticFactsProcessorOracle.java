// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.compiler;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import javax.annotation.processing.Processor;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/**
 * Processor oracle of the static-facts relocation (recipe static-use-plan-20261107, leaf 2; M3JDK-authored, test
 * scope). It compiles one fixed fixture corpus through the javac annotation processor named on the command line (the
 * donor class or the relocated class, reached by name only, so the same driver replays both) and prints, per corpus:
 * the compile result, every diagnostic (kind, file, line, column, code, message), every generated source file and the
 * META-INF/m3/compiler-facts.tsv ledger. The corpus covers the 44 loop shapes of the donor proof (one class each, one
 * javac task), overloads, instance / generic / varargs / nested / default-package methods, a local
 * class, CRLF and Unicode-escape sources, a 129-loop budget refusal, a misplaced annotation, an oversized source and a
 * compile without any annotated method.
 *
 * <p>The fixtures name the annotation package of the processor under test in their import, which is followed by a
 * comment padding the line to a fixed width, so every source offset the processor records is the same on both sides.
 * The dump masks exactly two declared differences: that package (printed as {@code <PKG>}) and the SHA-256 of each
 * fixture source text (printed as {@code SHA(<file>)} after the oracle checks that the processor wrote the SHA-256 of
 * the exact decoded source). Donor and relocated dumps must then be byte-identical. Lines end with a single LF.</p>
 *
 * <p>usage: java StaticFactsProcessorOracle &lt;processor class name&gt; [work directory]</p>
 */
public final class StaticFactsProcessorOracle {
    private static final int IMPORT_PAD = 64;
    private static final String TEMPLATE = "package fixture;\nimport <PKG>.StaticUsePlan.Analyze;\n"
            + "public final class %s {\n @Analyze public static int f(int n,int[] values) { int total=0; %s return total; }\n}\n";
    private static final String[] BODIES = {
        "for(int i=0;i<n;i++) { (i)--; }", "for(int i=0;i<n;i++) { (i)+=2; }", "for(int i=0;i<n;i++) { (i)=0; }",
        "for(int i=0;i<n;i++) { (n)++; }", "for(int i=0;i<n;i++) { (n)=8; }",
        "for(int i=0;i<values.length;i++) { (values)=new int[2]; }", "for(int i=0;i<n;i++) { ((i))--; }",
        "for(int i=0;i<n;i++) { ((i))+=2; }", "for(int i=0;i<n;i++) { ((i))=0; }", "for(int i=0;i<n;i++) { ((n))++; }",
        "for(int i=0;i<n;i++) { ((n))=8; }", "for(int i=0;i<values.length;i++) { ((values))=new int[2]; }",
        "for(int i=0;i<n;i++) { ((((i))))--; }", "for(int i=0;i<n;i++) { ((((i))))+=2; }",
        "for(int i=0;i<n;i++) { ((((i))))=0; }", "for(int i=0;i<n;i++) { ((((n))))++; }",
        "for(int i=0;i<n;i++) { ((((n))))=8; }", "for(int i=0;i<values.length;i++) { ((((values))))=new int[2]; }",
        "for(int i=0;i<n;i++) { ((((((((i))))))))--; }", "for(int i=0;i<n;i++) { ((((((((i))))))))+=2; }",
        "for(int i=0;i<n;i++) { ((((((((i))))))))=0; }", "for(int i=0;i<n;i++) { ((((((((n))))))))++; }",
        "for(int i=0;i<n;i++) { ((((((((n))))))))=8; }",
        "for(int i=0;i<values.length;i++) { ((((((((values))))))))=new int[2]; }",
        "for(int i=0;i<10;i++) total+=i;", "for(int i=0;i<n;i+=2) total+=i;",
        "for(int i=0;i<values.length;i++) total+=values[i];", "for(int i=10;i>=0;i--) total+=i;",
        "for(int i=0;i<10;i++) { if(n==i) break; total++; }", "for(int i=0;i<n;i++) { n--; }",
        "for(int i=0;i<n;i++) { i+=2; }", "for(int i=0;i<values.length;i++) { values=new int[0]; }",
        "for(int i=0;i<values.length;i++) { values[i]=7; }", "while(total<n) total++;", "for(int x:values) total+=x;",
        "for(long i=0;i<n;i++) total++;", "for(int i=0;i<=2147483647;i++) total++;",
        "String s=\"for(int i=0;i<10;i++)\"; // for(int i=0;i<10;i++)\n total=s.length();",
        "Runnable r=() -> { for(int i=0;i<10;i++) System.out.print(i); }; total=r.hashCode();",
        "for(int i=0;i<3;i++) for(int j=0;j<n;j++) total++;", "for(int i=0;i<10;i*=2) total++;",
        "for(int i=0;i<10;i++) { if(i==n) throw new IllegalArgumentException(); }",
        "for(int i=0;i<n;i++) { if(i==2) continue; total++; }", "int limit=n; for(int i=0;i<limit;i++) total++;"
    };

    private final Processor[] processor = new Processor[1];
    private final String processorName;
    private final String annotationPackage;
    private final PrintStream out;
    private final Path work;
    private int checks;

    private StaticFactsProcessorOracle(String processorName, Path work, PrintStream out) throws ReflectiveOperationException {
        this.processorName = processorName;
        this.annotationPackage = Class.forName(processorName).getPackageName();
        this.work = work;
        this.out = out;
    }

    public static void main(String[] args) throws Exception {
        Path work = args.length > 1 ? Path.of(args[1]) : Files.createTempDirectory("m3-static-facts-oracle-");
        PrintStream stdout = new PrintStream(new BufferedOutputStream(System.out, 1 << 16), false, StandardCharsets.UTF_8);
        StaticFactsProcessorOracle oracle = new StaticFactsProcessorOracle(args[0], work, stdout);
        oracle.run();
        stdout.flush();
    }

    private void run() throws Exception {
        Map<String, String> loops = new LinkedHashMap<>();
        for (int i = 0; i < BODIES.length; i++) {
            String name = String.format(Locale.ROOT, "Case%02d", i);
            loops.put("fixture/" + name + ".java", String.format(Locale.ROOT, TEMPLATE, name, BODIES[i]));
        }
        corpus("loop-shapes", loops);
        Map<String, String> shapes = new LinkedHashMap<>();
        shapes.put("fixture/Overloads.java", "package fixture; import <PKG>.StaticUsePlan.Analyze;\n"
                + "public final class Overloads {\n @Analyze static void f(int n){ for(int i=0;i<n;i++){} }\n"
                + " @Analyze static void f(long n){ for(int i=0;i<4;i++){} }\n"
                + " @Analyze static <T> T g(T value, int[] a){ for(int i=a.length;i>0;i--){} return value; }\n"
                + " @Analyze static int v(int... a){ int t=0; for(int i=0;i<a.length;i+=3) t+=a[i]; return t; }\n"
                + " Overloads(int n){ for(int i=-5;i<=n;i++){} }\n"
                + " @Analyze int instance(int n){ class Local { int k(){ for(int i=0;i<9;i++){} return 1; } } return new Local().k()+n; }\n"
                + " static final class Nested { @Analyze static void h(int[] a){ for(int i=0;i<a.length;i++){ a[i]++; } } }\n}\n");
        shapes.put("DefaultPackage.java", "import <PKG>.StaticUsePlan.Analyze;\n"
                + "public final class DefaultPackage { @Analyze static int f(int n){ int t=0; for(int i=n;i>=-3;i-=2) t++; return t; } }\n");
        shapes.put("fixture/Crlf.java", "package fixture;\r\nimport <PKG>.StaticUsePlan.Analyze;\r\n"
                + "public final class Crlf {\r\n @Analyze static int f(int n) { int t=0; for(int i=0;i<n;i++) t++; return t; }\r\n}\r\n");
        shapes.put("fixture/Escapes.java", "package fixture;\nimport <PKG>.StaticUsePlan.Analyze;\n"
                + "public final class Escapes {\n // caf\u00e9 \u03bb\n @Analyze static String f(int n) { String s=\"\\u00e9\"; "
                + "for(int i=0;i<+7;i++) s+=i; return s+n; }\n}\n");
        corpus("method-shapes", shapes);
        corpus("budget", Map.of("fixture/Budget.java", String.format(Locale.ROOT, TEMPLATE, "Budget",
                "for(int i=0;i<1;i++) total++;".repeat(129))));
        corpus("misplaced", Map.of("fixture/Misplaced.java", "package fixture; import <PKG>.StaticUsePlan.Analyze;\n"
                + "public final class Misplaced { @Analyze int field; }\n"));
        corpus("oversized", Map.of("fixture/Oversized.java", String.format(Locale.ROOT, TEMPLATE, "Oversized",
                "/*" + "x".repeat(1_048_576) + "*/ for(int i=0;i<1;i++) total++;")));
        corpus("none", Map.of("fixture/Plain.java", "package fixture; public final class Plain { static int f(int n){ return n; } }\n"));
        line("CHECKS " + checks);
        line("RESULT PASS");
        deleteTree(work);
    }

    private void line(String text) {
        out.print(text);
        out.print('\n');
    }

    private void check(boolean truth, String what) {
        checks++;
        if (!truth) {
            out.flush();
            throw new AssertionError("processor oracle: " + what);
        }
    }

    private String mask(String text, Map<String, String> shaToFile) {
        String masked = text.replace(annotationPackage, "<PKG>");
        for (Map.Entry<String, String> sha : shaToFile.entrySet()) masked = masked.replace(sha.getKey(), "SHA(" + sha.getValue() + ")");
        return masked;
    }

    private void corpus(String label, Map<String, String> templates) throws Exception {
        Path root = work.resolve(label);
        Path src = root.resolve("src");
        Path classes = root.resolve("classes");
        Path generated = root.resolve("generated");
        Files.createDirectories(classes);
        Files.createDirectories(generated);
        Map<String, String> shaToFile = new LinkedHashMap<>();
        List<Path> inputs = new ArrayList<>();
        for (Map.Entry<String, String> template : templates.entrySet()) {
            String text = template.getValue().replace("<PKG>.StaticUsePlan.Analyze;", annotationPackage
                    + ".StaticUsePlan.Analyze;/*" + " ".repeat(IMPORT_PAD - annotationPackage.length()) + "*/");
            check(!text.contains("<PKG>"), label + ": every template names the annotation through its import");
            Path file = src.resolve(template.getKey());
            Files.createDirectories(file.getParent());
            Files.writeString(file, text, StandardCharsets.UTF_8);
            inputs.add(file);
            shaToFile.put(sha256(text), template.getKey());
        }
        inputs.sort(Comparator.comparing(Path::toString));
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        boolean success;
        try (StandardJavaFileManager files = compiler.getStandardFileManager(diagnostics, Locale.ROOT, StandardCharsets.UTF_8)) {
            JavaCompiler.CompilationTask task = compiler.getTask(null, files, diagnostics,
                    List.of("--release", "21", "-Xlint:all", "-implicit:none", "-classpath", System.getProperty("java.class.path"),
                            "-d", classes.toString(), "-s", generated.toString()),
                    null, files.getJavaFileObjectsFromPaths(inputs));
            task.setLocale(Locale.ROOT);
            processor[0] = (Processor) Class.forName(processorName).getConstructor().newInstance();
            task.setProcessors(List.of(processor[0]));
            success = task.call();
        }
        line("CORPUS " + label + " files=" + templates.size() + " success=" + success);
        for (Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics()) {
            String file = d.getSource() == null ? "-" : src.relativize(Path.of(d.getSource().toUri())).toString().replace('\\', '/');
            line("DIAG " + d.getKind() + " " + file + ":" + d.getLineNumber() + ":" + d.getColumnNumber() + " " + d.getCode()
                    + " " + mask(d.getMessage(Locale.ROOT), shaToFile).replace("\r", "\\r").replace("\n", "\\n"));
        }
        List<Path> outputs;
        try (Stream<Path> walk = Files.walk(generated)) {
            outputs = walk.filter(Files::isRegularFile).sorted(Comparator.comparing(Path::toString)).toList();
        }
        for (Path output : outputs) {
            String text = Files.readString(output, StandardCharsets.UTF_8);
            String sha = text.replaceAll("(?s).*sourceSha256=\"([0-9a-f]{64})\".*", "$1");
            check(shaToFile.containsKey(sha), label + ": companion hash is the SHA-256 of an exact fixture source");
            line("GENERATED " + generated.relativize(output).toString().replace('\\', '/') + " sha=SHA(" + shaToFile.get(sha) + ")");
            for (String row : mask(text, shaToFile).split("\n", -1)) line("| " + row);
            String classFile = generated.relativize(output).toString().replace(".java", ".class");
            check(Files.isRegularFile(classes.resolve(classFile)) == success, label + ": companion class compiled iff success");
        }
        Path ledger = classes.resolve("META-INF/m3/compiler-facts.tsv");
        if (Files.isRegularFile(ledger)) {
            // The ledger is a sorted set whose order follows the raw source hash: check that order on the raw rows,
            // then print the masked rows in masked order (the hash is one of the two declared differences).
            List<String> raw = Files.readAllLines(ledger, StandardCharsets.UTF_8);
            List<String> body = raw.subList(1, raw.size());
            check(body.stream().sorted().toList().equals(body), label + ": ledger rows sorted");
            line("LEDGER rows=" + body.size());
            line("| " + mask(raw.get(0), shaToFile));
            body.stream().map(row -> mask(row, shaToFile)).sorted().forEach(row -> line("| " + row));
        } else {
            line("LEDGER absent");
        }
        check(!Files.isRegularFile(ledger) || success, label + ": no ledger after an error");
    }

    private static String sha256(String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException missing) {
            throw new IllegalStateException(missing);
        }
    }

    private static void deleteTree(Path root) throws IOException {
        if (!Files.exists(root)) return;
        try (Stream<Path> walk = Files.walk(root)) {
            for (Path path : walk.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
        }
    }
}
