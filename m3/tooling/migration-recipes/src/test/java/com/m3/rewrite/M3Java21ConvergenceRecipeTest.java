// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.m3.rewrite.atom.M3AtomizePureIntReturnRecipe;
import com.m3.rewrite.atom.M3DocumentPureIntAtomRecipe;
import com.m3.rewrite.atom.M3InventoryPureIntAtomCandidates;
import com.m3.rewrite.atom.M3PatternizePureIntAtomRecipe;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.RemoveUnusedImports;
import org.openrewrite.java.tree.J;
import org.openrewrite.text.PlainText;

final class M3Java21ConvergenceRecipeTest {
    @Test
    void orderedPassDagIsExplicitAndFileLocal() {
        var recipe = new M3Java21ConvergenceRecipe();
        List<Recipe> children = recipe.getRecipeList();

        assertEquals(5, children.size());
        assertEquals(M3InventoryPureIntAtomCandidates.class, children.get(0).getClass());
        assertEquals(M3AtomizePureIntReturnRecipe.class, children.get(1).getClass());
        assertEquals(M3PatternizePureIntAtomRecipe.class, children.get(2).getClass());
        assertEquals(M3DocumentPureIntAtomRecipe.class, children.get(3).getClass());
        assertEquals(RemoveUnusedImports.class, children.get(4).getClass());

        assertTrue(recipe.getTags().contains("multi-pass"));
        assertTrue(recipe.getTags().contains("java21"));
        assertTrue(recipe.getTags().contains("file-local"));
        assertTrue(recipe.getTags().contains("behavior-contract-preserving"));
    }

    @Test
    void oneTrustedDagConvergesAndSecondRunIsFixedPoint() {
        String path = "src/main/java/example/Sample.java";
        String before = """
                package example;
                final class Sample {
                    private static int compute(int a, int b) {
                        return (a + b) * 31;
                    }
                }
                """;

        Map<String, String> first = apply(new M3Java21ConvergenceRecipe(), Map.of(path, before));
        assertEquals(1, first.size());
        String after = first.get(path);
        assertTrue(after.contains("int m3$pureIntAtom ="));
        assertTrue(after.contains("M3-IOP: PURE_INT_EXPRESSION"));
        assertTrue(after.contains("M3-ATOM: m3$pureIntAtom"));
        assertTrue(apply(new M3Java21ConvergenceRecipe(), first).isEmpty());
    }

    @Test
    void catalogueCleanupRetainsCapturedLambdaAndMethodReference() {
        String path = "src/main/java/example/Functions.java";
        String before = """
                package example;

                import java.util.Set;
                import java.util.function.IntUnaryOperator;

                final class Functions {
                    static final IntUnaryOperator ABS = Math::abs;

                    static IntUnaryOperator shift(int offset) {
                        return value -> value + offset;
                    }
                }
                """;
        String expected = before.replace("import java.util.Set;\n", "");
        Map<String, String> after = apply(new M3Java21ConvergenceRecipe(), Map.of(path, before));
        assertEquals(Map.of(path, expected), after);
        assertTrue(apply(new M3Java21ConvergenceRecipe(), after).isEmpty());
    }

    @Test
    void catalogueCleanupPreservesUsedImportOrder() {
        String path = "src/main/java/example/Lists.java";
        String before = """
                package example;

                import java.util.List;
                import java.util.ArrayList;
                import java.util.Set;

                final class Lists {
                    static List<String> copy(List<String> input) {
                        return new ArrayList<>(input);
                    }
                }
                """;
        String expected = before.replace("import java.util.Set;\n", "");
        Map<String, String> after = apply(new M3Java21ConvergenceRecipe(), Map.of(path, before));
        assertEquals(Map.of(path, expected), after);
        assertTrue(apply(new M3Java21ConvergenceRecipe(), after).isEmpty());
    }

    @Test
    void catalogueCleanupPreservesCommentsAndLiterals() {
        String path = "src/main/java/example/Literals.java";
        String before = """
                package example;

                import java.util.Set;
                import java.util.function.IntUnaryOperator;

                /** The text import java.util.Set; is documentation, not a source import. */
                final class Literals {
                    // import java.util.Set; stays data here.
                    static final String TEXT = "import java.util.Set;";
                    static final IntUnaryOperator IDENTITY = value -> value;
                }
                """;
        String expected = before.replace("\nimport java.util.Set;\n", "\n");
        Map<String, String> after = apply(new M3Java21ConvergenceRecipe(), Map.of(path, before));
        assertEquals(Map.of(path, expected), after);
        assertTrue(apply(new M3Java21ConvergenceRecipe(), after).isEmpty());
    }

    @Test
    void unknownTypesPreventGuessedImportRemoval() {
        String path = "src/main/java/example/Unresolved.java";
        String before = """
                package example;

                import java.util.Set;

                final class Unresolved {
                    MissingType value;
                }
                """;
        assertTrue(apply(new M3Java21ConvergenceRecipe(), Map.of(path, before)).isEmpty());
    }

    @Test
    void catalogueCleanupPreservesNativeAndCheckedExceptionContracts() {
        String path = "src/main/java/example/NativeApi.java";
        String before = """
                package example;

                import java.io.IOException;
                import java.nio.ByteBuffer;
                import java.util.Set;

                final class NativeApi {
                    static native String read(ByteBuffer input) throws IOException;
                }
                """;
        String expected = before.replace("import java.util.Set;\n", "");
        Map<String, String> after = apply(new M3Java21ConvergenceRecipe(), Map.of(path, before));
        assertEquals(Map.of(path, expected), after);
        assertTrue(apply(new M3Java21ConvergenceRecipe(), after).isEmpty());
    }

    @Test
    void nonJavaSourcesAreNotAJavaTransformationSurface() {
        SourceFile nativeSource = PlainText.builder()
                .sourcePath(Path.of("src/hotspot/share/prims/probe.cpp"))
                .text("// import java.util.Set;\nint value() { return 1; }\n")
                .build();
        var context = new InMemoryExecutionContext(error -> {
            throw new AssertionError(error);
        });
        var run = new M3Java21ConvergenceRecipe().run(
                new InMemoryLargeSourceSet(List.of(nativeSource)), context, 8);
        assertTrue(run.getChangeset().getAllResults().isEmpty());
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

        var result = recipe.run(new InMemoryLargeSourceSet(parsed), context, 8);
        Map<String, String> after = new LinkedHashMap<>();
        result.getChangeset().getAllResults().forEach(change -> {
            SourceFile original = change.getBefore();
            SourceFile file = change.getAfter();
            assertNotNull(original);
            assertNotNull(file);
            assertTrue(file instanceof J.CompilationUnit);
            assertEquals(original.getId(), file.getId());
            assertEquals(original.getSourcePath(), file.getSourcePath());
            after.put(file.getSourcePath().toString().replace('\\', '/'), file.printAll());
        });
        return after;
    }
}
