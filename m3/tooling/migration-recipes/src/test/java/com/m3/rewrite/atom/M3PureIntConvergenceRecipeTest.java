// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.atom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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

final class M3PureIntConvergenceRecipeTest {
    private static final String ATOMIZED_WITHOUT_METADATA = """
            package example;
            final class Sample {
                private static int compute(int a, int b) {
                    int m3$pureIntAtom = (a + b) * 31;
                    return m3$pureIntAtom;
                }
            }
            """;

    @Test
    void compositeOrderAndMetadataAreExplicit() {
        var recipe = new M3PureIntConvergenceRecipe();
        List<Recipe> children = recipe.getRecipeList();
        assertEquals(3, children.size());
        assertEquals(M3AtomizePureIntReturnRecipe.class, children.get(0).getClass());
        assertEquals(M3PatternizePureIntAtomRecipe.class, children.get(1).getClass());
        assertEquals(M3DocumentPureIntAtomRecipe.class, children.get(2).getClass());
        assertTrue(recipe.getTags().contains("atomization"));
        assertTrue(recipe.getTags().contains("patternization"));
        assertTrue(recipe.getTags().contains("iop"));
        assertTrue(recipe.getTags().contains("documentation"));
    }

    @Test
    void patternizerAndDocumenterConvergeIndependentlyAndThenStop() {
        String path = "src/main/java/example/Sample.java";
        Map<String, String> source = Map.of(path, ATOMIZED_WITHOUT_METADATA);

        Map<String, String> patternized = apply(new M3PatternizePureIntAtomRecipe(), source);
        assertEquals(1, patternized.size());
        assertTrue(patternized.get(path).contains("M3-IOP: PURE_INT_EXPRESSION"));
        assertTrue(apply(new M3PatternizePureIntAtomRecipe(), patternized).isEmpty());

        Map<String, String> documented = apply(new M3DocumentPureIntAtomRecipe(), patternized);
        assertEquals(1, documented.size());
        assertTrue(documented.get(path).contains("M3-ATOM: m3$pureIntAtom"));
        assertTrue(documented.get(path).contains("Pattern/IOP: PURE_INT_EXPRESSION"));
        assertTrue(apply(new M3DocumentPureIntAtomRecipe(), documented).isEmpty());
    }

    @Test
    void nonAtomizedSourceIsNotPatternizedOrDocumented() {
        Map<String, String> source = Map.of(
                "src/main/java/example/NotAtomized.java",
                """
                package example;
                final class NotAtomized {
                    private static int compute(int a, int b) {
                        return (a + b) * 31;
                    }
                }
                """);
        assertTrue(apply(new M3PatternizePureIntAtomRecipe(), source).isEmpty());
        assertTrue(apply(new M3DocumentPureIntAtomRecipe(), source).isEmpty());
    }

    @Test
    void compositeConvergesCompilesExecutesAndReachesFixedPoint() throws Exception {
        String path = "src/main/java/example/Converged.java";
        String before = """
                package example;
                public final class Converged {
                    private Converged() {}
                    public static int eval(int a, int b) {
                        return compute(a, b);
                    }
                    private static int compute(int a, int b) {
                        return (a + b) * 31;
                    }
                }
                """;

        Map<String, String> after = apply(new M3PureIntConvergenceRecipe(), Map.of(path, before));
        assertEquals(1, after.size());
        String changed = after.get(path);
        assertNotEquals(before, changed);
        assertTrue(changed.contains("int m3$pureIntAtom ="));
        assertTrue(changed.contains("M3-IOP: PURE_INT_EXPRESSION"));
        assertTrue(changed.contains("M3-ATOM: m3$pureIntAtom"));
        assertTrue(apply(new M3PureIntConvergenceRecipe(), after).isEmpty());
        compileAndExecute(after, 7, 11, (7 + 11) * 31);
    }

    private static Map<String, String> apply(Recipe recipe, Map<String, String> sources) {
        var context = new InMemoryExecutionContext(error -> {
            throw new AssertionError(error);
        });
        List<Parser.Input> inputs = new ArrayList<>(sources.size());
        sources.forEach((path, source) -> inputs.add(Parser.Input.fromString(Path.of(path), source)));
        List<SourceFile> parsed = JavaParser.fromJavaVersion()
                .build()
                .parseInputs(inputs, null, context)
                .toList();
        var result = recipe.run(new InMemoryLargeSourceSet(parsed), context, 5);
        Map<String, String> after = new LinkedHashMap<>();
        result.getChangeset().getAllResults().forEach(change -> {
            SourceFile file = change.getAfter();
            after.put(file.getSourcePath().toString().replace('\\', '/'), file.printAll());
        });
        return after;
    }

    private static void compileAndExecute(
            Map<String, String> sources, int a, int b, int expected) throws Exception {
        var compiler = ToolProvider.getSystemJavaCompiler();
        assertFalse(compiler == null, "tests require a full JDK");

        Path root = Files.createTempDirectory("m3-convergence-");
        Path sourceRoot = root.resolve("src");
        Path outputRoot = root.resolve("classes");
        Files.createDirectories(outputRoot);

        List<Path> javaFiles = new ArrayList<>();
        for (var entry : sources.entrySet()) {
            Path relative = Path.of(entry.getKey()).subpath(3, Path.of(entry.getKey()).getNameCount());
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
            assertTrue(compiled, "converged source must compile as Java 21");
        }

        try (var loader = new URLClassLoader(
                new URL[] {outputRoot.toUri().toURL()},
                M3PureIntConvergenceRecipeTest.class.getClassLoader())) {
            Class<?> type = Class.forName("example.Converged", true, loader);
            Object actual = type.getMethod("eval", int.class, int.class).invoke(null, a, b);
            assertEquals(expected, ((Integer) actual).intValue());
        }
    }
}
