// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
package jdk.m3.compiler.standard;

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
 * Real javac runs over clean, convention-dirty and semantic-name-quality fixtures.
 *
 * <p>The convention-dirty fixture breaks every original rule once (whitespace three times). The
 * quality fixture independently proves placeholder, promotional, versioned, repeated, overlong
 * and over-tokenized names. Error mode must fail both dirty fixtures and pass the clean one.
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

                static final class M3UTF16Lane {
                }

                static final class M3IPv6 {
                }

                int readUTF16Lane() {
                    return 0;
                }

                int readIPv6() {
                    return 0;
                }

                int openHyperlink() {
                    return 0;
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

    private static final String QUALITY = """
            package com.example.quality;

            public final class QualityFixture {
                static final class Helper {
                }

                static final class M3PersistentMemoryMappedInferenceWeightRepositoryCoordinator {
                }

                static final class M3VectorStoreV2 {
                }

                static final class M3UltraCache {
                }

                static final class M3SchemaSnapshot20261031 {
                }

                void doStuff() {
                }

                void calculateDeterministicSegmentedAddressProjectionForGeneration() {
                }

                void loadLoad() {
                }

                void buildMapRunPlanTaskGraphNode() {
                }

                void optimizedRead() {
                }

                void loadVersion2() {
                }

                void snapshot20261031() {
                }
            }
            """;

    private static final String LEGACY_CONTRACT = """
            package com.example.legacy;

            public interface LegacyContract {
                void doStuff();
            }
            """;

    private static final String LEGACY_OVERRIDE = """
            package com.example.legacy;

            public final class LegacyImplementation implements LegacyContract {
                @Override
                public void doStuff() {
                }
            }
            """;

    private static final String LEGACY = """
            package jdk.internal.m3.legacy;

            /** A fixture that keeps Synexia names. */
            public final class M3Thing {
                /** A legacy constant. */
                public static final int M3_LIMIT = 1;

                private M3Thing() {
                }
            }
            """;

    private static final String SCHEMED = """
            package jdk.internal.m3.example;

            /** A fixture in the M3JDK 21 naming scheme. */
            public final class M3Thing {
                /** A constant. */
                public static final int M3_LIMIT = 1;

                private M3Thing() {
                }
            }
            """;

    private static final Map<String, Integer> DIRTY_RULES = new TreeMap<>(Map.ofEntries(
            Map.entry("PACKAGE_NAME", 1), Map.entry("WILDCARD_IMPORT", 1),
            Map.entry("TYPE_NAME", 1), Map.entry("TYPE_PARAMETER_NAME", 1),
            Map.entry("CONSTANT_NAME", 1), Map.entry("FIELD_NAME", 1),
            Map.entry("METHOD_NAME", 1), Map.entry("PARAMETER_NAME", 1),
            Map.entry("LOCAL_NAME", 1), Map.entry("WHITESPACE", 3), Map.entry("LINE_LENGTH", 1)));
    private static final Map<String, Integer> QUALITY_RULES = Map.of("NAME_QUALITY", 12);

    private M3JdkStandardPluginSelfTest() {
    }

    /**
     * Runs the fixtures and fails with an {@link AssertionError} on the first mismatch.
     *
     * @param args unused
     * @throws IOException if the scratch directory cannot be written
     */
    public static void main(String[] args) throws IOException {
        Path work = Files.createTempDirectory("m3jdk-standard");
        Path conventionReport = work.resolve("convention.tsv");
        Path qualityReport = work.resolve("quality.tsv");
        Map<String, Integer> clean = rules(compile(work, "CleanType", CLEAN, "mode=warning"));
        check(clean.isEmpty(), "clean fixture has findings " + clean);
        check(compilePlain(work, "LegacyContract", LEGACY_CONTRACT),
                "legacy contract did not compile");
        Run legacyOverride = compile(work, "LegacyImplementation", LEGACY_OVERRIDE,
                "mode=warning");
        check(legacyOverride.success() && rules(legacyOverride).isEmpty(),
                "inherited override was not preserved " + rules(legacyOverride));

        Run dirty = compile(work, "bad_type", DIRTY, "mode=warning",
                "report=" + conventionReport);
        check(rules(dirty).equals(DIRTY_RULES), "dirty rules " + rules(dirty));
        check(dirty.diagnostics().stream().map(M3JdkStandardPluginSelfTest::message)
                .noneMatch(m -> m.matches(".*parameter arg(ument)? .*")), "arg/argument refused");

        Run quality = compile(work, "QualityFixture", QUALITY, "mode=warning",
                "report=" + qualityReport);
        check(rules(quality).equals(QUALITY_RULES), "quality rules " + rules(quality));
        requireMessage(quality, "type Helper: is a placeholder");
        requireMessage(quality, "type M3UltraCache: contains promotional wording");
        requireMessage(quality, "type M3VectorStoreV2: carries a version/date suffix");
        requireMessage(quality, "type M3SchemaSnapshot20261031: carries a version/date suffix");
        requireMessage(quality, "method optimizedRead: contains promotional wording");
        requireMessage(quality, "method loadVersion2: carries a version/date suffix");
        requireMessage(quality, "method snapshot20261031: carries a version/date suffix");
        requireMessage(quality, "method loadLoad: repeats adjacent word Load");
        requireMessage(quality, "characters; limit 48");
        requireMessage(quality, "characters; limit 40");
        requireMessage(quality, "semantic words; limit 6");

        long conventionRows = Files.readAllLines(conventionReport).size() - 1L;
        long qualityRows = Files.readAllLines(qualityReport).size() - 1L;
        int conventionFindings = DIRTY_RULES.values().stream().mapToInt(Integer::intValue).sum();
        int qualityFindings = QUALITY_RULES.values().stream().mapToInt(Integer::intValue).sum();
        check(conventionRows == conventionFindings,
                "convention rows " + conventionRows + ", findings " + conventionFindings);
        check(qualityRows == qualityFindings,
                "quality rows " + qualityRows + ", findings " + qualityFindings);
        check(!compile(work, "bad_type", DIRTY).success()
                && !compile(work, "QualityFixture", QUALITY).success()
                && compile(work, "CleanType", CLEAN).success(),
                "error mode must fail both dirty fixtures and pass clean");
        schemeChecks(work);
        LOG.log(System.Logger.Level.INFO, "M3JDK_STANDARD_SELFTEST PASS rules="
                + (DIRTY_RULES.size() + QUALITY_RULES.size()) + " findings="
                + (conventionRows + qualityRows));
    }

    private record Run(boolean success, List<Diagnostic<? extends JavaFileObject>> diagnostics) {
    }

    /** scheme=m3: the Synexia fixture breaks it three times, the M3 fixture never; opt-in. */
    private static void schemeChecks(Path work) {
        Map<String, Integer> legacy =
                rules(compile(work, "M3Thing", LEGACY, "mode=warning", "scheme=m3"));
        check(legacy.equals(Map.of("NAME_SCHEME", 3)), "scheme=m3 over Synexia names " + legacy);
        Map<String, Integer> schemed =
                rules(compile(work, "M3Thing", SCHEMED, "mode=warning", "scheme=m3"));
        check(schemed.isEmpty(), "scheme=m3 flagged M3 names " + schemed);
        Map<String, Integer> off = rules(compile(work, "M3Thing", LEGACY, "mode=warning"));
        check(off.isEmpty(), "the scheme must be opt-in " + off);
    }

    private static Run compile(Path work, String name, String source, String... options) {
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> sink = new DiagnosticCollector<>();
        String plugin = ("-Xplugin:M3JdkStandard " + String.join(" ", options)).trim();
        List<String> flags = List.of("-proc:none", "-d", work.toString(), "-classpath",
                work.toString(), "-processorpath", System.getProperty("java.class.path"), plugin);
        List<JavaFileObject> units = List.of(source(name, source));
        boolean ok = javac.getTask(null, null, sink, flags, null, units).call();
        return new Run(ok, sink.getDiagnostics().stream()
                .filter(d -> message(d).contains("[M3JdkStandard ")).collect(Collectors.toList()));
    }

    private static boolean compilePlain(Path work, String name, String text) {
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        List<String> flags = List.of("-proc:none", "-d", work.toString());
        return javac.getTask(null, null, null, flags, null, List.of(source(name, text))).call();
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

    private static void requireMessage(Run run, String expected) {
        check(run.diagnostics().stream().map(M3JdkStandardPluginSelfTest::message)
                .anyMatch(message -> message.contains(expected)), "missing message " + expected);
    }

    private static void check(boolean condition, String failure) {
        if (!condition) {
            throw new AssertionError("M3JDK_STANDARD_SELFTEST FAIL " + failure);
        }
    }
}
