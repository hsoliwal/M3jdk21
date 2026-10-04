// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.m3.rewrite.atom.M3AtomizePureIntReturnRecipe;
import com.m3.rewrite.atom.M3DocumentPureIntAtomRecipe;
import com.m3.rewrite.atom.M3InventoryPureIntAtomCandidates;
import com.m3.rewrite.atom.M3PatternizePureIntAtomRecipe;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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

final class M3Java21ConvergenceRecipeTest {
    @Test
    void orderedPassDagIsExplicitAndFileLocal() {
        var recipe = new M3Java21ConvergenceRecipe();
        List<Recipe> children = recipe.getRecipeList();

        assertEquals(4, children.size());
        assertEquals(M3InventoryPureIntAtomCandidates.class, children.get(0).getClass());
        assertEquals(M3AtomizePureIntReturnRecipe.class, children.get(1).getClass());
        assertEquals(M3PatternizePureIntAtomRecipe.class, children.get(2).getClass());
        assertEquals(M3DocumentPureIntAtomRecipe.class, children.get(3).getClass());

        assertTrue(recipe.getTags().contains("multi-pass"));
        assertTrue(recipe.getTags().contains("java21"));
        assertTrue(recipe.getTags().contains("file-local"));
        assertTrue(recipe.getTags().contains("behavior-contract-preserving"));
    }

    @Test
    void namedAbsorptionRecipeIsSavedAsTheSameTrustedDag() throws IOException {
        try (var input =
                M3Java21ConvergenceRecipeTest.class.getResourceAsStream(
                        "/META-INF/rewrite/m3-absorption.yml")) {
            assertTrue(input != null);
            String recipe = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(recipe.contains("name: com.m3.rewrite.M3Jdk21AbsorptionFirst"));
            assertTrue(recipe.contains("- com.m3.rewrite.M3Java21ConvergenceRecipe"));
        }
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
            SourceFile file = change.getAfter();
            after.put(file.getSourcePath().toString().replace('\\', '/'), file.printAll());
        });
        return after;
    }
}
