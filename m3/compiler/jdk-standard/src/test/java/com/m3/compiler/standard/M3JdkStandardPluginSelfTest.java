// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
package com.m3.compiler.standard;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

/**
 * Real javac runs of {@link M3JdkStandardPlugin} over a clean and a dirty fixture.
 *
 * <p>The dirty fixture breaks every rule once (whitespace three times) and also declares the
 * parameters {@code arg} and {@code argument}, which must pass: only reflection names of the form
 * {@code arg<digits>} are refused. Error mode must fail the dirty compile and pass the clean one.
 */
public final class M3JdkStandardPluginSelfTest {
    private static final System.Logger LOG =
            System.getLogger(M3JdkStandardPluginSelfTest.class.getName());

    private static final String CLEAN = """
            package com.example.clean;

            import java.util.List;

            /** A fixture that meets the standard. */
            public final class CleanType<E> {
                private static final int MAX_SIZE = 4;
                private final List<E> elements;

                /**
                 * Creates the fixture.
                 *
                 * @param elements the elements to hold
                 */
                public CleanType(List<E> elements) {
                    this.elements = elements;
                }

                /**
                 * Returns whether the fixture is full.
                 *
                 * @return {@code true} at {@code MAX_SIZE} elements
                 */
                public boolean isFull() {
                    int size = elements.size();
                    return size >= MAX_SIZE;
                }
            }
            """;

    private static final String DIRTY = "package Bad_Pkg;\n\nimport java.util.*;\n\n"
            + "public class bad_type<TT> {\n"
            + "    static final int lowerConstant = 1;\n"
            + "    private int Bad_Field;\n\n"
            + "    public void Bad_Method(int arg0, int arg, int argument) {\n"
            + "        int Bad_Local = arg0 + arg + argument;\t// tab\n"
            + "        Bad_Field = Bad_Local; \n"
            + "        Bad_Field = lowerConstant;\r\n"
            + "        String longLine = \"" + "x".repeat(90) + "\";\n"
            + "    }\n}\n";

    private static final Map<String, Integer> DIRTY_RULES = new TreeMap<>(Map.ofEntries(
            Map.entry("PACKAGE_NAME", 1), Map.entry("WILDCARD_IMPORT", 1),
            Map.entry("TYPE_NAME", 1), Map.entry("TYPE_PARAMETER_NAME", 1),
            Map.entry("CONSTANT_NAME", 1), Map.entry("FIELD_NAME", 1),
            Map.entry("METHOD_NAME", 1), Map.entry("PARAMETER_NAME", 1),
            Map.entry("LOCAL_NAME", 1), Map.entry("WHITESPACE", 3), Map.entry("LINE_LENGTH", 1)));

    private M3JdkStandardPluginSelfTest() {
    }

    /**
     * Runs the fixtures and fails with an {@link AssertionError} on the first mismatch.
     *
     * @param args unused
     * @throws IOException if the scratch directory cannot be written
     */
    public static void main(String[] args) throws IOException {
        Path work = Files.createTempDirectory("m3-jdk-standard");
        Path report = work.resolve("report.tsv");
        Map<String, Integer> clean = rules(compile(work, "CleanType", CLEAN, "mode=warning"));
        check(clean.isEmpty(), "clean fixture has findings " + clean);
        Run dirty = compile(work, "bad_type", DIRTY, "mode=warning", "report=" + report);
        check(rules(dirty).equals(DIRTY_RULES), "dirty rules " + rules(dirty));
        check(dirty.diagnostics().stream().map(M3JdkStandardPluginSelfTest::message)
                .noneMatch(m -> m.matches(".*parameter arg(ument)? .*")), "arg/argument refused");
        long rows = Files.readAllLines(report).size() - 1L;
        int findings = DIRTY_RULES.values().stream().mapToInt(Integer::intValue).sum();
        check(rows == findings, "report rows " + rows + ", findings " + findings);
        check(!compile(work, "bad_type", DIRTY).success()
                && compile(work, "CleanType", CLEAN).success(), "error mode must fail only dirty");
        LOG.log(System.Logger.Level.INFO, "M3_JDK_STANDARD_SELFTEST PASS rules="
                + DIRTY_RULES.size() + " findings=" + rows);
    }

    private record Run(boolean success, List<Diagnostic<? extends JavaFileObject>> diagnostics) {
    }

    private static Run compile(Path work, String name, String source, String... options) {
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> sink = new DiagnosticCollector<>();
        String plugin = ("-Xplugin:M3JdkStandard " + String.join(" ", options)).trim();
        List<String> flags = List.of("-proc:none", "-d", work.toString(), "-processorpath",
                System.getProperty("java.class.path"), plugin);
        List<JavaFileObject> units = List.of(source(name, source));
        boolean ok = javac.getTask(null, null, sink, flags, null, units).call();
        return new Run(ok, sink.getDiagnostics().stream()
                .filter(d -> message(d).contains("[M3JdkStandard ")).collect(Collectors.toList()));
    }

    private static JavaFileObject source(String name, String text) {
        URI uri = URI.create("string:///" + name + ".java");
        return new SimpleJavaFileObject(uri, JavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                return text;
            }
        };
    }

    private static Map<String, Integer> rules(Run run) {
        Function<Diagnostic<? extends JavaFileObject>, String> rule =
                d -> message(d).replaceFirst("(?s).*\\[M3JdkStandard ([A-Z_]+)\\].*", "$1");
        return run.diagnostics().stream().collect(Collectors.groupingBy(rule, TreeMap::new,
                Collectors.reducing(0, d -> 1, Integer::sum)));
    }

    private static String message(Diagnostic<? extends JavaFileObject> diagnostic) {
        return diagnostic.getMessage(null);
    }

    private static void check(boolean condition, String failure) {
        if (!condition) {
            throw new AssertionError("M3_JDK_STANDARD_SELFTEST FAIL " + failure);
        }
    }
}
