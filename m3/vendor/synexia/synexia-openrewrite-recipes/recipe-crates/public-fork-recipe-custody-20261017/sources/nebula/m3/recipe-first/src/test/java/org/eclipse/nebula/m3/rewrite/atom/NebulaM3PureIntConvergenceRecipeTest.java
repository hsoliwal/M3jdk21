// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.atom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

final class NebulaM3PureIntConvergenceRecipeTest {
    @Test
    void recipeMetadataAndAuthorityAreExplicit() {
        List<Recipe> recipes =
                List.of(
                        new NebulaM3AtomizePureIntReturnRecipe(),
                        new NebulaM3PatternizePureIntAtomRecipe(),
                        new NebulaM3DocumentPureIntAtomRecipe(),
                        new NebulaM3PureIntConvergenceRecipe());

        for (Recipe recipe : recipes) {
            assertFalse(recipe.getDisplayName().isBlank());
            assertFalse(recipe.getDescription().isBlank());
            assertEquals(1, recipe.maxCycles());
            assertTrue(recipe.getTags().contains("file-local"));
            assertTrue(recipe.getTags().contains("behavior-contract-preserving"));
        }

        assertEquals(
                List.of(
                        NebulaM3AtomizePureIntReturnRecipe.class,
                        NebulaM3PatternizePureIntAtomRecipe.class,
                        NebulaM3DocumentPureIntAtomRecipe.class),
                new NebulaM3PureIntConvergenceRecipe().getRecipeList().stream()
                        .map(Object::getClass)
                        .toList());
    }

    @Test
    void phasesAreIndependentAndThenConverge() {
        String path = "src/main/java/example/Sample.java";
        String before = """
                package example;
                final class Sample {
                    private static int compute(int a, int b) {
                        return (a + b) * 31;
                    }
                }
                """;

        Map<String, String> atomized =
                apply(new NebulaM3AtomizePureIntReturnRecipe(), Map.of(path, before));
        String atom = atomized.get(path);
        assertTrue(atom.contains("int m3$pureIntAtom ="));
        assertFalse(atom.contains("M3-IOP:"));
        assertFalse(atom.contains("M3-ATOM:"));

        Map<String, String> patternized =
                apply(new NebulaM3PatternizePureIntAtomRecipe(), atomized);
        String pattern = patternized.get(path);
        assertTrue(pattern.contains("M3-IOP: PURE_INT_EXPRESSION"));
        assertFalse(pattern.contains("M3-ATOM:"));

        Map<String, String> documented =
                apply(new NebulaM3DocumentPureIntAtomRecipe(), patternized);
        String doc = documented.get(path);
        assertTrue(doc.contains("M3-ATOM: m3$pureIntAtom"));
        assertTrue(doc.contains("Pattern/IOP: PURE_INT_EXPRESSION"));

        assertTrue(apply(new NebulaM3PureIntConvergenceRecipe(), documented).isEmpty());
    }

    @Test
    void unsafeOrBroaderShapesRemainUntouched() {
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
        sources.put("src/main/java/negative/Instance.java", """
                package negative;
                final class Instance {
                    private int compute(int a, int b) { return a + b; }
                }
                """);
        assertTrue(apply(new NebulaM3PureIntConvergenceRecipe(), sources).isEmpty());
    }

    @Test
    void existingSemanticJavadocIsRecognizedAsFixedPoint() {
        String path = "src/main/java/example/Documented.java";
        String source = """
                package example;
                final class Documented {
                    /** M3-ATOM: m3$pureIntAtom; Pattern/IOP: PURE_INT_EXPRESSION. */
                    private static int compute(int a, int b) {
                        /* M3-IOP: PURE_INT_EXPRESSION */
                        int m3$pureIntAtom = a + b;
                        return m3$pureIntAtom;
                    }
                }
                """;

        assertTrue(
                apply(
                                new NebulaM3DocumentPureIntAtomRecipe(),
                                Map.of(path, source))
                        .isEmpty());
    }

    @Test
    void existingSemanticBlockCommentIsRecognizedAsFixedPoint() {
        String path = "src/main/java/example/BlockDocumented.java";
        String source = """
                package example;
                final class BlockDocumented {
                    /* M3-ATOM: m3$pureIntAtom; Pattern/IOP: PURE_INT_EXPRESSION. */
                    private static int compute(int a, int b) {
                        /* M3-IOP: PURE_INT_EXPRESSION */
                        int m3$pureIntAtom = a + b;
                        return m3$pureIntAtom;
                    }
                }
                """;

        assertTrue(
                apply(
                                new NebulaM3DocumentPureIntAtomRecipe(),
                                Map.of(path, source))
                        .isEmpty());
    }

    @Test
    void unrelatedMethodCommentStillReceivesSemanticDocumentation() {
        String path = "src/main/java/example/UnrelatedComment.java";
        String source = """
                package example;
                final class UnrelatedComment {
                    /* unrelated */
                    private static int compute(int a, int b) {
                        /* M3-IOP: PURE_INT_EXPRESSION */
                        int m3$pureIntAtom = a + b;
                        return m3$pureIntAtom;
                    }
                }
                """;

        Map<String, String> changed =
                apply(
                        new NebulaM3DocumentPureIntAtomRecipe(),
                        Map.of(path, source));

        assertEquals(1, changed.size());
        assertTrue(changed.get(path).contains("M3-ATOM: m3$pureIntAtom"));
        assertTrue(changed.get(path).contains("/* unrelated */"));
    }

    @Test
    void compactMethodWithoutLeadingNewlineUsesItsExistingWhitespaceAsIndent() {
        String path = "src/main/java/example/Compact.java";
        String source =
                "package example; final class Compact { private static int compute(int a, int b) { "
                        + "/* M3-IOP: PURE_INT_EXPRESSION */ int m3$pureIntAtom = a + b; "
                        + "return m3$pureIntAtom; } }";

        Map<String, String> changed =
                apply(
                        new NebulaM3DocumentPureIntAtomRecipe(),
                        Map.of(path, source));

        assertEquals(1, changed.size());
        assertTrue(changed.get(path).contains("M3-ATOM: m3$pureIntAtom"));
    }

    @Test
    void oneHundredIndependentFilesCompileExecuteAndReachFixedPoint() throws Exception {
        Map<String, String> original = corpus(100);
        Map<String, String> transformed =
                apply(new NebulaM3PureIntConvergenceRecipe(), original);

        assertEquals(100, transformed.size());
        for (String source : transformed.values()) {
            assertTrue(source.contains("int m3$pureIntAtom ="));
            assertTrue(source.contains("M3-IOP: PURE_INT_EXPRESSION"));
            assertTrue(source.contains("M3-ATOM: m3$pureIntAtom"));
        }
        assertTrue(apply(new NebulaM3PureIntConvergenceRecipe(), transformed).isEmpty());
        compileAndExecute(transformed);
    }

    private static Map<String, String> corpus(int count) {
        Map<String, String> sources = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) {
            sources.put(path(i), """
                    package corpus.p%d;
                    public final class C%d {
                        private C%d() {}
                        public static int eval(int a, int b) { return compute(a, b); }
                        private static int compute(int a, int b) { return %s; }
                    }
                    """.formatted(i % 10, i, i, expression(i % 10)));
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

    private static Map<String, String> apply(
            Recipe recipe, Map<String, String> sources) {
        var context = new InMemoryExecutionContext(error -> {
            throw new AssertionError(error);
        });
        List<Parser.Input> inputs = new ArrayList<>(sources.size());
        sources.forEach((path, source) ->
                inputs.add(Parser.Input.fromString(Path.of(path), source)));
        List<SourceFile> parsed = JavaParser.fromJavaVersion()
                .build()
                .parseInputs(inputs, null, context)
                .toList();
        var run = recipe.run(new InMemoryLargeSourceSet(parsed), context);
        Map<String, String> after = new LinkedHashMap<>();
        run.getChangeset().getAllResults().forEach(change -> {
            SourceFile file = change.getAfter();
            if (file == null) throw new AssertionError("unexpected deletion");
            after.put(file.getSourcePath().toString().replace('\\', '/'), file.printAll());
        });
        return after;
    }

    private static void compileAndExecute(Map<String, String> sources) throws Exception {
        var compiler = ToolProvider.getSystemJavaCompiler();
        assertFalse(compiler == null, "tests require a full JDK");

        Path root = Files.createTempDirectory("nebula-m3-atom-corpus-");
        Path sourceRoot = root.resolve("src");
        Path outputRoot = root.resolve("classes");
        Files.createDirectories(outputRoot);
        List<Path> javaFiles = new ArrayList<>();

        for (var entry : sources.entrySet()) {
            Path sourcePath = Path.of(entry.getKey());
            Path relative = sourcePath.subpath(3, sourcePath.getNameCount());
            Path javaFile = sourceRoot.resolve(relative);
            Files.createDirectories(javaFile.getParent());
            Files.writeString(javaFile, entry.getValue());
            javaFiles.add(javaFile);
        }

        try (var manager = compiler.getStandardFileManager(null, null, null)) {
            boolean compiled = compiler.getTask(
                    null,
                    manager,
                    null,
                    List.of("--release", "21", "-d", outputRoot.toString()),
                    null,
                    manager.getJavaFileObjectsFromPaths(javaFiles)).call();
            assertTrue(compiled);
        }

        try (var loader = new URLClassLoader(
                new URL[] {outputRoot.toUri().toURL()},
                NebulaM3PureIntConvergenceRecipeTest.class.getClassLoader())) {
            for (int i = 0; i < 100; i++) {
                int a = i * 3 - 50;
                int b = i * 7 + 11;
                Class<?> type = Class.forName(
                        "corpus.p" + (i % 10) + ".C" + i, true, loader);
                int actual = ((Integer) type.getMethod("eval", int.class, int.class)
                        .invoke(null, a, b)).intValue();
                assertEquals(expected(i % 10, a, b), actual);
            }
        }
    }
}
