// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.atom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

final class M3AtomizePureIntReturnRecipeTest {
    @Test
    void tagsDeclareBothAtomizationAndPatternization() {
        var tags = new M3AtomizePureIntReturnRecipe().getTags();
        assertTrue(tags.contains("atomization"));
        assertTrue(tags.contains("patternization"));
        assertTrue(tags.contains("iop"));
        assertTrue(tags.contains("file-local"));
        assertTrue(tags.contains("behavior-contract-preserving"));
    }

    @Test
    void oneFileApplyChangesOnlyThePrivatePureLeafAndThenStops() {
        String path = "src/main/java/example/Sample.java";
        String before = """
                package example;

                final class Sample {
                    static int publicSurface(int a, int b) {
                        return compute(a, b);
                    }

                    private static int compute(int a, int b) {
                        return (a + b) * 31;
                    }
                }
                """;

        Map<String, String> after = apply(Map.of(path, before));
        assertEquals(1, after.size());
        String changed = after.get(path);
        assertNotEquals(before, changed);
        assertTrue(changed.contains("M3-IOP: PURE_INT_EXPRESSION"));
        assertTrue(changed.contains("int m3$pureIntAtom ="));
        assertTrue(changed.contains("return m3$pureIntAtom;"));
        assertTrue(changed.contains("static int publicSurface(int a, int b)"));
        assertTrue(apply(after).isEmpty());
    }

    @Test
    void unsafeOrBroaderShapesAreLeftUntouched() {
        Map<String, String> sources = new LinkedHashMap<>();
        sources.put("src/main/java/negative/PublicMethod.java", """
                package negative;
                final class PublicMethod {
                    public static int compute(int a, int b) { return a + b; }
                }
                """);
        sources.put("src/main/java/negative/Division.java", """
                package negative;
                final class Division {
                    private static int compute(int a, int b) { return a / b; }
                }
                """);
        sources.put("src/main/java/negative/Modulo.java", """
                package negative;
                final class Modulo {
                    private static int compute(int a, int b) { return a % b; }
                }
                """);
        sources.put("src/main/java/negative/FieldRead.java", """
                package negative;
                final class FieldRead {
                    private static int state = 3;
                    private static int compute(int a) { return a + state; }
                }
                """);
        sources.put("src/main/java/negative/Invocation.java", """
                package negative;
                final class Invocation {
                    private static int other(int a) { return a; }
                    private static int compute(int a) { return other(a) + 1; }
                }
                """);
        sources.put("src/main/java/negative/TwoStatements.java", """
                package negative;
                final class TwoStatements {
                    private static int compute(int a, int b) {
                        int c = a + b;
                        return c * 2;
                    }
                }
                """);
        sources.put("src/main/java/negative/InstanceMethod.java", """
                package negative;
                final class InstanceMethod {
                    private int compute(int a, int b) { return a + b; }
                }
                """);
        sources.put("src/main/java/negative/LongReturn.java", """
                package negative;
                final class LongReturn {
                    private static long compute(int a, int b) { return (long) a + b; }
                }
                """);
        sources.put("src/main/java/negative/NonIntParameter.java", """
                package negative;
                final class NonIntParameter {
                    private static int compute(long a, int b) { return (int) a + b; }
                }
                """);
        sources.put("src/main/java/negative/ReservedAtomName.java", """
                package negative;
                final class ReservedAtomName {
                    private static int compute(int m3$pureIntAtom, int b) {
                        return m3$pureIntAtom + b;
                    }
                }
                """);

        assertTrue(apply(sources).isEmpty());
    }

    @Test
    void hundredFileFullApplyCompilesExecutesAndReachesFixedPoint() throws Exception {
        Map<String, String> original = corpus(100);
        Map<String, String> transformed = apply(original);

        assertEquals(100, transformed.size());
        for (int i = 0; i < 100; i++) {
            String path = path(i);
            String source = transformed.get(path);
            assertTrue(source.contains("M3-IOP: PURE_INT_EXPRESSION"), path);
            assertTrue(source.contains("int m3$pureIntAtom ="), path);
            assertTrue(source.contains("return m3$pureIntAtom;"), path);
        }

        assertTrue(apply(transformed).isEmpty());
        compileAndExecute(transformed);
    }

    private static Map<String, String> corpus(int count) {
        Map<String, String> sources = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) {
            int bucket = i % 10;
            String expression = expression(bucket);
            sources.put(path(i), """
                    package corpus.p%d;

                    public final class C%d {
                        private C%d() {}

                        public static int eval(int a, int b) {
                            return compute(a, b);
                        }

                        private static int compute(int a, int b) {
                            return %s;
                        }
                    }
                    """.formatted(i % 10, i, i, expression));
        }
        return sources;
    }

    private static String path(int i) {
        return "src/main/java/corpus/p" + (i % 10) + "/C" + i + ".java";
    }

    private static String expression(int bucket) {
        return switch (bucket) {
            case 0 -> "a + b";
            case 1 -> "a - b";
            case 2 -> "a * 31 + b";
            case 3 -> "(a << 3) ^ b";
            case 4 -> "(a >>> 2) + (b << 1)";
            case 5 -> "~a + b";
            case 6 -> "(a & 255) | (b << 8)";
            case 7 -> "(a ^ b) * 17";
            case 8 -> "-a + +b";
            case 9 -> "(a * b) ^ (a + 7)";
            default -> throw new IllegalArgumentException("bucket");
        };
    }

    private static int expected(int bucket, int a, int b) {
        return switch (bucket) {
            case 0 -> a + b;
            case 1 -> a - b;
            case 2 -> a * 31 + b;
            case 3 -> (a << 3) ^ b;
            case 4 -> (a >>> 2) + (b << 1);
            case 5 -> ~a + b;
            case 6 -> (a & 255) | (b << 8);
            case 7 -> (a ^ b) * 17;
            case 8 -> -a + +b;
            case 9 -> (a * b) ^ (a + 7);
            default -> throw new IllegalArgumentException("bucket");
        };
    }

    private static Map<String, String> apply(Map<String, String> sources) {
        var context = context();
        var parsed = parse(sources, context);
        var result = new M3AtomizePureIntReturnRecipe()
                .run(new InMemoryLargeSourceSet(parsed), context, 3);
        Map<String, String> after = new LinkedHashMap<>();
        result.getChangeset().getAllResults().forEach(change -> {
            SourceFile file = change.getAfter();
            after.put(normalized(file.getSourcePath()), file.printAll());
        });
        return after;
    }

    private static List<SourceFile> parse(
            Map<String, String> sources, InMemoryExecutionContext context) {
        List<Parser.Input> inputs = new ArrayList<>(sources.size());
        sources.forEach((path, source) ->
                inputs.add(Parser.Input.fromString(Path.of(path), source)));
        return JavaParser.fromJavaVersion()
                .build()
                .parseInputs(inputs, null, context)
                .toList();
    }

    private static void compileAndExecute(Map<String, String> sources) throws Exception {
        var compiler = ToolProvider.getSystemJavaCompiler();
        assertFalse(compiler == null, "tests require a full JDK");

        Path root = Files.createTempDirectory("m3-atom-corpus-");
        Path sourceRoot = root.resolve("src");
        Path outputRoot = root.resolve("classes");
        Files.createDirectories(outputRoot);

        List<Path> javaFiles = new ArrayList<>(sources.size());
        for (var entry : sources.entrySet()) {
            Path relative = Path.of(entry.getKey())
                    .subpath(3, Path.of(entry.getKey()).getNameCount());
            Path javaFile = sourceRoot.resolve(relative);
            Files.createDirectories(javaFile.getParent());
            Files.writeString(javaFile, entry.getValue());
            javaFiles.add(javaFile);
        }

        try (var fileManager = compiler.getStandardFileManager(null, null, null)) {
            var units = fileManager.getJavaFileObjectsFromPaths(javaFiles);
            boolean compiled = compiler.getTask(
                    null,
                    fileManager,
                    null,
                    List.of("--release", "21", "-d", outputRoot.toString()),
                    null,
                    units).call();
            assertTrue(compiled, "100-file transformed corpus must compile");
        }

        try (var loader = new URLClassLoader(
                new URL[] {outputRoot.toUri().toURL()},
                M3AtomizePureIntReturnRecipeTest.class.getClassLoader())) {
            for (int i = 0; i < 100; i++) {
                int a = i * 3 - 50;
                int b = i * 7 + 11;
                Class<?> type = Class.forName(
                        "corpus.p" + (i % 10) + ".C" + i,
                        true,
                        loader);
                Object actual = type.getMethod("eval", int.class, int.class)
                        .invoke(null, a, b);
                assertEquals(expected(i % 10, a, b), ((Integer) actual).intValue());
            }
        }
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(error -> {
            throw new AssertionError(error);
        });
    }

    private static String normalized(Path path) {
        return path.toString().replace('\\', '/');
    }
}
