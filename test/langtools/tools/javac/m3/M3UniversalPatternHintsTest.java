/*
 * @test
 * @summary M3AST compiler hints apply to ordinary Java sources and preserve bytecode
 * @run main M3UniversalPatternHintsTest
 */
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/** Source-built M3JDK 21 integration test. No @Sortable, annotation processor or runtime flags. */
public class M3UniversalPatternHintsTest {
    private static final String SOURCE = """
            public class OrdinaryTask implements Runnable {
                private int value;
                @Override public void run() {
                    if (value >= 0) {
                        for (int i = 0; i < 3; i++) {
                            value += new Helper().apply(i);
                            synchronized (this) { value++; }
                        }
                    }
                }
            }
            """;
    private static final String HELPER = """
            public class Helper { public int apply(int value) { return value + 1; } }
            """;
    private static int checks;

    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("m3-javac-pattern-hints-");
        try {
            Path first = root.resolve("OrdinaryTask.java");
            Path helper = root.resolve("Helper.java");
            Files.writeString(first, SOURCE);
            Files.writeString(helper, HELPER);
            Path baseline = root.resolve("base");
            Path candidate = root.resolve("candidate");
            Path reverse = root.resolve("reverse");
            Path report = root.resolve("hints.tsv");
            Path reverseReport = root.resolve("hints-reverse.tsv");
            Run control = compile(List.of(first, helper), baseline, null);
            check(control.success(), "normal Java compilation must pass: " + control.diagnostics());
            Run withHints = compile(List.of(first, helper), candidate,
                    "-Xplugin:M3AST lossless hints=both hintfile=" + report);
            check(withHints.success(), "M3AST compilation must pass: " + withHints.diagnostics());
            check(withHints.diagnostics().contains("[M3-MOP:CANDIDATE]"),
                    "ordinary Java sources must receive compiler-origin hints");
            check(Files.isRegularFile(report), "compilation receipt emitted after successful build");
            String text = Files.readString(report);
            check(text.startsWith("M3_COMPILER_PATTERN_HINTS_V1\t"), "versioned receipt");
            check(text.contains("M\t") && text.contains("T\t"),
                    "type and source-positioned method facts emitted");
            check(text.contains("N\t2\t"), "both unannotated top-level types covered");
            int hashAt = text.lastIndexOf("SHA256\t");
            check(hashAt > 0 && hash(text.substring(0, hashAt))
                            .equals(text.substring(hashAt + 7).strip()),
                    "whole compiler report is SHA-256 sealed");
            check(Arrays.equals(Files.readAllBytes(baseline.resolve("OrdinaryTask.class")),
                            Files.readAllBytes(candidate.resolve("OrdinaryTask.class"))),
                    "opt-in hint extraction does not change bytecode");
            Run changedOrder = compile(List.of(helper, first), reverse,
                    "-Xplugin:M3AST lossless hints=receipts hintfile=" + reverseReport);
            check(changedOrder.success(), "reverse input compile must pass");
            check(!changedOrder.diagnostics().contains("[M3-MOP:CANDIDATE]"),
                    "receipts mode must not emit notes");
            check(text.equals(Files.readString(reverseReport)),
                    "compiler file scheduling order must not affect source report root");
            Path broken = root.resolve("Broken.java");
            Files.writeString(broken, "class Broken { void m() { doesNotExist(); } }");
            Path refusal = root.resolve("broken-hints.tsv");
            Run failure = compile(List.of(broken), root.resolve("broken"),
                    "-Xplugin:M3AST lossless hints=both hintfile=" + refusal);
            check(!failure.success() && !Files.exists(refusal),
                    "failed compilation never publishes a valid M3 hint report");
            System.out.println("M3_UNIVERSAL_PATTERN_HINTS_PASS checks=" + checks);
        } finally {
            try (var paths = Files.walk(root)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList())
                    Files.deleteIfExists(path);
            }
        }
    }

    private static Run compile(List<Path> sources, Path classes, String plugin) throws IOException {
        Files.createDirectories(classes);
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        check(javac != null, "real javac required");
        DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();
        var arguments = new java.util.ArrayList<>(List.of(
                "--release", "21", "-proc:none", "-d", classes.toString()));
        if (plugin != null) arguments.add(plugin);
        boolean ok;
        try (StandardJavaFileManager manager = javac.getStandardFileManager(
                collector, null, StandardCharsets.UTF_8)) {
            ok = Boolean.TRUE.equals(javac.getTask(new StringWriter(), manager, collector,
                    arguments, null, manager.getJavaFileObjectsFromPaths(sources)).call());
        }
        StringBuilder output = new StringBuilder();
        for (Diagnostic<? extends JavaFileObject> diagnostic : collector.getDiagnostics()) {
            output.append(diagnostic.getKind()).append(':')
                    .append(diagnostic.getMessage(null)).append('\n');
        }
        return new Run(ok, output.toString());
    }

    private static String hash(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
    }
    private static void check(boolean good, String message) {
        checks++;
        if (!good) throw new AssertionError(message);
    }
    private record Run(boolean success, String diagnostics) { }
}
