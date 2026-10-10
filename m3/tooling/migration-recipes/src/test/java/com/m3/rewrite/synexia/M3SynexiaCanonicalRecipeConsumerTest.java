// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.synexia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.m3.rewrite.scope.M3EditScope;
import com.m3.rewrite.scope.M3RecipeScopeRegistry;
import com.synexia.rewrite.atom.M3PureIntConvergenceRecipe;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.config.Environment;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

final class M3SynexiaCanonicalRecipeConsumerTest {
    @Test
    void borrowedCanonicalRecipeTransformsAndThenStops() {
        String source = """
                package example;
                final class Sample {
                    private static int compute(int a, int b) {
                        return (a + b) * 31;
                    }
                }
                """;

        Recipe recipe = new M3PureIntConvergenceRecipe();
        SourceFile first = apply(recipe, "src/main/java/example/Sample.java", source);
        String changed = first.printAll();

        assertTrue(changed.contains("int m3$pureIntAtom ="));
        assertTrue(changed.contains("M3-IOP: PURE_INT_EXPRESSION"));
        assertTrue(changed.contains("M3-ATOM: m3$pureIntAtom"));
        assertEquals(changed, apply(recipe, "src/main/java/example/Sample.java", changed).printAll());

        var policy = M3RecipeScopeRegistry.require(M3PureIntConvergenceRecipe.class);
        assertEquals(M3EditScope.FILE, policy.minimumScope());
        assertTrue(policy.fileLocalMechanical(List.of("src/main/java/example/Sample.java")));
    }

    @Test
    void targetNamedRecipeIsOnlyAThinActivationOfSynexiaOwner() {
        Recipe named = Environment.builder().scanYamlResources().build()
                .activateRecipes("com.m3.rewrite.synexia.PureIntConvergence");

        assertEquals(1, named.getRecipeList().size());
        assertTrue(containsTaggedRecipe(named, M3PureIntConvergenceRecipe.class));
    }

    private static boolean containsTaggedRecipe(Recipe recipe, Class<? extends Recipe> expected) {
        if (expected.isInstance(recipe)) {
            return !recipe.getTags().isEmpty();
        }
        return recipe.getRecipeList().stream()
                .anyMatch(child -> containsTaggedRecipe(child, expected));
    }

    private static SourceFile apply(Recipe recipe, String path, String source) {
        var context = new InMemoryExecutionContext(error -> {
            throw new AssertionError(error);
        });
        List<SourceFile> parsed;
        try (var stream = JavaParser.fromJavaVersion().build().parseInputs(
                List.of(Parser.Input.fromString(Path.of(path), source)), null, context)) {
            parsed = stream.toList();
        }
        assertEquals(1, parsed.size());
        var run = recipe.run(new InMemoryLargeSourceSet(parsed), context, 5);
        var changes = run.getChangeset().getAllResults();
        if (changes.isEmpty()) {
            return parsed.getFirst();
        }
        assertEquals(1, changes.size());
        return changes.getFirst().getAfter();
    }
}
