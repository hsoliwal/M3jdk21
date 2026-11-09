// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.compiler;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.annotation.processing.Processor;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/**
 * Processor oracle of the mat sortable relocation (recipe mat-sortable-processor-20261107; M3JDK-authored, test scope).
 * It runs one fixed fixture corpus of mat sortables through the javac annotation processor named on the command line
 * (the donor class or the relocated class, reached by name only, so the same driver replays both) with
 * {@code -proc:only} and prints, per corpus: the result, every diagnostic (kind, file, line, column, code, message) and
 * every generated companion source. The fixtures name the annotation package and prefix of the processor under test
 * (the donor's annotation package with its bare names, or {@code jdk.m3.collections} with the {@code M3} names), both
 * derived from {@link Processor#getSupportedAnnotationTypes()}; the import line is padded by a comment to a fixed
 * width and every fixture annotation stands on its own line, so every source position agrees on both sides.
 *
 * <p>Declared masking (DECISION.tsv S7): the annotation package and prefix are printed as {@code <PKG>} and
 * {@code <P>}; the donor's capability companions ({@code *M3Capabilities.java}) and its
 * {@code invalid M3 capability schema} diagnostics belong to the held emitter lane and are left out; the donor's
 * generated {@code stableSignalOrder} block (held with its owner MatIntOrder) is removed from the transcript. Donor and
 * relocated transcripts must then be byte-identical. {@code -proc:only} stops javac after annotation processing, so the
 * generated companions are compared as source; the contract compiles and runs them on the relocated side.</p>
 *
 * <p>usage: java MatSortableProcessorOracle &lt;processor class name&gt; [work directory]</p>
 */
public final class MatSortableProcessorOracle {
    private static final int IMPORT_PAD = 48;
    private static final Pattern STABLE_SIGNAL_ORDER = Pattern.compile(
            "    /\\*\\* Stable primitive rank by signal; not canonical object identity\\. \\*/\n"
            + "    public static int\\[\\] stableSignalOrder\\([^\n]*\\) \\{\n"
            + "        return [^\n]*\\.ascending\\(signals\\(values\\)\\)\\.rows\\(\\);\n    \\}\n");
    private static final String HELD_EMITTER_DIAGNOSTIC = "invalid M3 capability schema: ";
    private static final String HELD_EMITTER_SUFFIX = "M3Capabilities.java";
    private static final String HEADER = "package probe;\nimport <PKG>.*;\n";

    private final String processorName;
    private final String annotationPackage;
    private final String prefix;
    private final PrintStream out;
    private final Path work;
    private int checks;

    private MatSortableProcessorOracle(String processorName, Path work, PrintStream out) throws ReflectiveOperationException {
        this.processorName = processorName;
        Processor probe = (Processor) Class.forName(processorName).getConstructor().newInstance();
        String sortable = probe.getSupportedAnnotationTypes().stream()
                .filter(name -> name.endsWith("Sortable") && !name.contains("facets"))
                .sorted().findFirst().orElseThrow();
        this.annotationPackage = sortable.substring(0, sortable.lastIndexOf('.'));
        this.prefix = sortable.substring(annotationPackage.length() + 1, sortable.length() - "Sortable".length());
        this.work = work;
        this.out = out;
    }

    public static void main(String[] args) throws Exception {
        Path work = args.length > 1 ? Path.of(args[1]) : Files.createTempDirectory("m3-mat-sortable-oracle-");
        PrintStream stdout = new PrintStream(new BufferedOutputStream(System.out, 1 << 16), false, StandardCharsets.UTF_8);
        MatSortableProcessorOracle oracle = new MatSortableProcessorOracle(args[0], work, stdout);
        oracle.run();
        stdout.flush();
    }

    private void run() throws Exception {
        Map<String, String> positives = new LinkedHashMap<>();
        positives.put("probe/Person.java", HEADER + "@<P>Sortable public record Person(\n"
                + "    @<P>SortField(priority=0) String lastName,\n    @<P>SortField(priority=1) String firstName,\n"
                + "    int age, double score) {\n  @<P>SortSignal public double relevance() { return score; }\n"
                + "  @<P>SortDistance public double distance(Person other) throws IllegalArgumentException {\n"
                + "    return Math.abs(score-other.score);\n  }\n"
                + "  String decoy() { return \"@<P>SortField(priority=99)\"; }\n  String regex() { return \"(class|@<P>Sortable)\"; }\n}\n");
        positives.put("probe/Bean.java", HEADER + "@<P>Sortable public class Bean {\n  @<P>SortField(priority=0) final int age;\n"
                + "  final String name;\n  public Bean(int age,String name) {this.age=age;this.name=name;}\n"
                + "  @<P>SortField(priority=1) String label() {return name;}\n"
                + "  @<P>SortSignal double score() throws IllegalStateException {return age*0.5;}\n}\n");
        positives.put("probe/Descending.java", HEADER + "@<P>Sortable public record Descending(\n"
                + "    @<P>SortField(priority=0, descending=true) long rank,\n    @<P>SortField(priority=1) int tie) { }\n");
        positives.put("probe/ScoreOnly.java", HEADER + "@<P>Sortable public final class ScoreOnly {\n  private final double score;\n"
                + "  public ScoreOnly(double score) { this.score = score; }\n  @<P>SortSignal public double score() { return score; }\n}\n");
        positives.put("probe/AllComponents.java", HEADER + "@<P>Sortable public record AllComponents(java.time.Instant when,"
                + " java.util.UUID id, java.math.BigDecimal amount, java.math.BigInteger count, java.time.LocalDate day,"
                + " java.time.LocalDateTime stamp) { }\n");
        positives.put("probe/Wrappers.java", HEADER + "@<P>Sortable public record Wrappers(\n"
                + "    @<P>SortField(priority=3) Boolean flag, @<P>SortField(priority=1) Byte small,\n"
                + "    @<P>SortField(priority=2, descending=true) Short medium, @<P>SortField(priority=0) Character letter,\n"
                + "    @<P>SortField(priority=4) Integer number, @<P>SortField(priority=5) Long wide,\n"
                + "    @<P>SortField(priority=6) Float narrow, @<P>SortField(priority=7, descending=true) Double real,\n"
                + "    @<P>SortField(priority=8) Level level) {\n  public enum Level { LOW, HIGH }\n}\n");
        positives.put("probe/Primitives.java", HEADER + "@<P>Sortable public record Primitives(\n"
                + "    @<P>SortField(priority=0) boolean flag, @<P>SortField(priority=1) byte small,\n"
                + "    @<P>SortField(priority=2) short medium, @<P>SortField(priority=3, descending=true) char letter,\n"
                + "    @<P>SortField(priority=4) int number, @<P>SortField(priority=5) long wide,\n"
                + "    @<P>SortField(priority=6) float narrow, @<P>SortField(priority=7) double real) { }\n");
        positives.put("probe/Outer.java", HEADER + "public final class Outer {\n  @<P>Sortable public static final class Inner {\n"
                + "    @<P>SortField(priority=0) public final int rank = 0;\n"
                + "    @<P>SortSignal public double weight() { return rank; }\n  }\n"
                + "  @<P>Sortable public record Pair(@<P>SortField(priority=1) String key, @<P>SortField(priority=0) int ordinal) { }\n}\n");
        positives.put("probe/Declared.java", HEADER + "@<P>Sortable\n@<P>SortDependency({\"probe.Repository#load()\", \"probe.Cache\"})\n"
                + "public class Declared {\n  @<P>SortField(priority=0) public final int rank;\n"
                + "  @<P>SortDependency({\"probe.FieldIndex#scan()\"})\n  public final String name;\n"
                + "  public Declared(int rank, String name) { this.rank=rank; this.name=name; }\n"
                + "  @<P>SortDependency({\"probe.Service#fetch(int)\"})\n  public int hint() { return rank; }\n"
                + "  public String decoy() { return \"probe.Decoy#never()\"; }\n}\n");
        positives.put("DefaultPackage.java", "import <PKG>.*;\n@<P>Sortable public record DefaultPackage("
                + "@<P>SortField(priority=0) int id, String note) {\n  @<P>SortSignal public double rank() { return id; }\n}\n");
        corpus("positives", positives);
        corpus("crlf", Map.of("probe/Crlf.java", "package probe;\r\nimport <PKG>.*;\r\n@<P>Sortable public record Crlf(\r\n"
                + "    @<P>SortField(priority=0) String key,\r\n    @<P>SortField(priority=1, descending=true) int weight) { }\r\n"));
        String[][] negatives = {
            {"private-field", "@<P>Sortable public class X { @<P>SortField(priority=0) private int age; }"},
            {"duplicate-priority", "@<P>Sortable public class X { @<P>SortField(priority=0) int age; @<P>SortField(priority=0) int score; }"},
            {"int-signal", "@<P>Sortable public class X { @<P>SortSignal int score() { return 1; } }"},
            {"object-field", "@<P>Sortable public class X { @<P>SortField(priority=0) Object opaque; }"},
            {"distance-string", "@<P>Sortable public class X { @<P>SortDistance double dist(String s) { return 1; } @<P>SortField(priority=0) int n; }"},
            {"generic", "@<P>Sortable public class X<T> { @<P>SortField(priority=0) int n; }"},
            {"abstract", "@<P>Sortable public abstract class X { @<P>SortField(priority=0) int n; }"},
            {"enum", "@<P>Sortable public enum X { A, B }"},
            {"interface", "@<P>Sortable public interface X { int n(); }"},
            {"private-nested", "public class X { @<P>Sortable private static class Y { @<P>SortField(priority=0) int n; } }"},
            {"static-field", "@<P>Sortable public class X { @<P>SortField(priority=0) static int n; }"},
            {"field-method-parameter", "@<P>Sortable public class X { @<P>SortField(priority=0) int n(int k) { return k; } }"},
            {"two-signals", "@<P>Sortable public class X { @<P>SortSignal double a() { return 1; } @<P>SortSignal double b() { return 2; } }"},
            {"signal-parameter", "@<P>Sortable public class X { @<P>SortSignal double a(int k) { return k; } }"},
            {"checked-throws-signal", "@<P>Sortable public class X { @<P>SortSignal double score() throws java.io.IOException { return 1.0; } }"},
            {"checked-throws-field", "@<P>Sortable public class X { @<P>SortField(priority=0) double key() throws java.io.IOException { return 1.0; } }"},
            {"checked-throws-distance", "@<P>Sortable public class X { @<P>SortField(priority=0) int n; @<P>SortDistance double distance(X other) throws java.io.IOException { return 1.0; } }"},
            {"no-columns", "@<P>Sortable public class X { int n; }"},
            {"empty-record", "@<P>Sortable public record X() { }"},
            {"dependency-empty", "@<P>SortDependency({})\n@<P>Sortable public class X { @<P>SortField(priority=0) int value; }"},
            {"dependency-blank", "@<P>SortDependency({\"  \"})\n@<P>Sortable public class X { @<P>SortField(priority=0) int value; }"},
            {"dependency-duplicate", "@<P>SortDependency({\"probe.A#go()\", \"probe.A#go()\"})\n@<P>Sortable public class X { @<P>SortField(priority=0) int value; }"},
            {"dependency-overflow", "@<P>SortDependency({" + descriptors(65) + "})\n@<P>Sortable public class X { @<P>SortField(priority=0) int value; }"},
            {"member-budget", "@<P>Sortable public class X { @<P>SortSignal double score(){return 0.0;}" + fields(0, 129) + " }"},
            {"schema-size", "@<P>Sortable public class X { @<P>SortSignal double score(){return 0.0;}" + longFields(100) + " }"},
        };
        for (String[] negative : negatives) {
            corpus("negative-" + negative[0], Map.of("probe/X.java", HEADER + oneAnnotationPerLine(negative[1]) + "\n"));
        }
        corpus("none", Map.of("probe/Plain.java", "package probe; public final class Plain { static int f(int n){ return n; } }\n"));
        line("CHECKS " + checks);
        line("RESULT PASS");
        deleteTree(work);
    }

    /**
     * Every annotation on its own line, so the positions javac reports for the annotated declarations do not depend on
     * the width of the annotation name under test (the donor's and the relocated names differ by their prefix).
     */
    private static String oneAnnotationPerLine(String source) {
        return source.replaceAll("(@<P>\\w+(?:\\([^)]*\\))?) ", "$1\n");
    }

    private static String descriptors(int count) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0) text.append(", ");
            text.append("\"probe.Edge#e").append(i).append("()\"");
        }
        return text.toString();
    }

    private static String fields(int from, int toExclusive) {
        StringBuilder text = new StringBuilder();
        for (int i = from; i < toExclusive; i++) text.append(" int field").append(i).append(';');
        return text.toString();
    }

    private static String longFields(int count) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < count; i++) {
            text.append(" int ").append("declaration".repeat(18)).append(i).append(';');
        }
        return text.toString();
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

    private String mask(String text) {
        return text.replace(annotationPackage + "." + prefix + "Sort", "<PKG>.<P>Sort")
                .replace(annotationPackage + ".", "<PKG>.").replace("@" + prefix + "Sort", "@<P>Sort");
    }

    private String fixture(String template) {
        String header = "import " + annotationPackage + ".*;";
        String padded = header + "/*" + " ".repeat(Math.max(0, IMPORT_PAD - header.length())) + "*/";
        String text = template.replace("import <PKG>.*;", padded).replace("@<P>Sort", "@" + prefix + "Sort");
        check(!text.contains("<PKG>") && !text.contains("<P>"), "every template names the annotations through its import");
        return text;
    }

    private void corpus(String label, Map<String, String> templates) throws Exception {
        Path root = work.resolve(label);
        Path src = root.resolve("src");
        Path classes = root.resolve("classes");
        Path generated = root.resolve("generated");
        Files.createDirectories(classes);
        Files.createDirectories(generated);
        List<Path> inputs = new ArrayList<>();
        for (Map.Entry<String, String> template : templates.entrySet()) {
            Path file = src.resolve(template.getKey());
            Files.createDirectories(file.getParent());
            Files.writeString(file, fixture(template.getValue()), StandardCharsets.UTF_8);
            inputs.add(file);
        }
        inputs.sort(Comparator.comparing(Path::toString));
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        boolean success;
        try (StandardJavaFileManager files = compiler.getStandardFileManager(diagnostics, Locale.ROOT, StandardCharsets.UTF_8)) {
            JavaCompiler.CompilationTask task = compiler.getTask(null, files, diagnostics,
                    List.of("--release", "21", "-proc:only", "-implicit:none", "-classpath", System.getProperty("java.class.path"),
                            "-d", classes.toString(), "-s", generated.toString()),
                    null, files.getJavaFileObjectsFromPaths(inputs));
            task.setLocale(Locale.ROOT);
            task.setProcessors(List.of((Processor) Class.forName(processorName).getConstructor().newInstance()));
            success = task.call();
        }
        line("CORPUS " + label + " files=" + templates.size() + " success=" + success);
        for (Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics()) {
            String message = d.getMessage(Locale.ROOT);
            if (message.startsWith(HELD_EMITTER_DIAGNOSTIC)) continue;
            String file = d.getSource() == null ? "-" : src.relativize(Path.of(d.getSource().toUri())).toString().replace('\\', '/');
            line("DIAG " + d.getKind() + " " + file + ":" + d.getLineNumber() + ":" + d.getColumnNumber() + " " + d.getCode()
                    + " " + mask(message).replace("\r", "\\r").replace("\n", "\\n"));
        }
        List<Path> outputs;
        try (Stream<Path> walk = Files.walk(generated)) {
            outputs = walk.filter(Files::isRegularFile).sorted(Comparator.comparing(Path::toString)).toList();
        }
        int companions = 0;
        for (Path output : outputs) {
            String name = generated.relativize(output).toString().replace('\\', '/');
            if (name.endsWith(HELD_EMITTER_SUFFIX)) continue;
            companions++;
            String text = Files.readString(output, StandardCharsets.UTF_8);
            check(text.endsWith("}\n"), label + ": companion " + name + " is complete");
            String masked = STABLE_SIGNAL_ORDER.matcher(mask(text)).replaceAll("");
            line("GENERATED " + name + " lines=" + masked.lines().count());
            for (String row : masked.split("\n", -1)) line("| " + row);
        }
        check(success == !label.startsWith("negative-"), label + ": admitted corpora succeed, negative corpora are refused");
        line("END " + label + " companions=" + companions);
    }

    private static void deleteTree(Path root) throws IOException {
        if (!Files.exists(root)) return;
        try (Stream<Path> walk = Files.walk(root)) {
            for (Path path : walk.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
        }
    }
}
