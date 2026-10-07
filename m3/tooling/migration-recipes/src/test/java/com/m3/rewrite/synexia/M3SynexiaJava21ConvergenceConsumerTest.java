// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.synexia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.m3.rewrite.M3Java21ConvergenceCatalog;
import com.m3.rewrite.M3Java21ConvergenceRecipe;
import com.m3.rewrite.scope.M3ContractMode;
import com.m3.rewrite.scope.M3EditScope;
import com.m3.rewrite.scope.M3RecipeScopeRegistry;
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

final class M3SynexiaJava21ConvergenceConsumerTest {
    @Test
    void canonicalAndHistoricalCompatibilityRecipeProduceTheSameNormalForm() {
        String path = "src/main/java/example/Sample.java";
        String before = """
                package example;

                import java.util.List;

                final class Sample {
                    private static int compute(int a, int b) {
                        return (a + b) * 31;
                    }
                }
                """;

        Map<String, String> legacy =
                apply(new M3Java21ConvergenceRecipe(), Map.of(path, before));
        Map<String, String> canonical =
                apply(new com.synexia.rewrite.M3Java21FileConvergenceRecipe(), Map.of(path, before));

        assertEquals(legacy, canonical);
        assertTrue(canonical.get(path).contains("int m3$pureIntAtom ="));
        assertTrue(canonical.get(path).contains("M3-IOP: PURE_INT_EXPRESSION"));
        assertTrue(canonical.get(path).contains("M3-ATOM: m3$pureIntAtom"));
        assertTrue(!canonical.get(path).contains("import java.util.List;"));
        assertTrue(
                apply(new com.synexia.rewrite.M3Java21FileConvergenceRecipe(), canonical)
                        .isEmpty());
    }

    @Test
    void declarativeCatalogUsesCanonicalSynexiaRecipeBehavior() {
        String path = "src/main/java/example/Declarative.java";
        String before = """
                package example;

                import java.util.Set;

                final class Declarative {
                    private static int compute(int a, int b) {
                        return (a ^ b) + 1;
                    }
                }
                """;

        Map<String, String> declarative =
                apply(M3Java21ConvergenceCatalog.activate(), Map.of(path, before));
        Map<String, String> canonical =
                apply(new com.synexia.rewrite.M3Java21FileConvergenceRecipe(), Map.of(path, before));

        assertEquals(canonical, declarative);
    }

    @Test
    void missingTypeAttributionRemainsFailClosed() {
        String path = "src/main/java/example/Partial.java";
        String before = """
                package example;

                import java.util.Set;

                final class Partial {
                    MissingType value;
                }
                """;
        assertTrue(
                apply(new com.synexia.rewrite.M3Java21FileConvergenceRecipe(),
                        Map.of(path, before)).isEmpty());
        assertTrue(
                apply(M3Java21ConvergenceCatalog.activate(), Map.of(path, before)).isEmpty());
    }

    @Test
    void targetRegistryAdmitsCanonicalRecipeOnlyAtFileScope() {
        var policy = M3RecipeScopeRegistry.require(
                com.synexia.rewrite.M3Java21FileConvergenceRecipe.class);
        List<String> target = List.of("src/main/java/example/Only.java");
        assertEquals(M3EditScope.FILE, policy.resolve(target));
        assertEquals(
                M3ContractMode.BEHAVIOR_AND_CONTRACT_PRESERVING,
                policy.contractMode());
        assertTrue(policy.fileLocalMechanical(target));
    }

    private static Map<String, String> apply(Recipe recipe, Map<String, String> sources) {
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

        var result = recipe.run(new InMemoryLargeSourceSet(parsed), context, 8);
        Map<String, String> after = new LinkedHashMap<>();
        result.getChangeset().getAllResults().forEach(change -> {
            SourceFile file = change.getAfter();
            after.put(file.getSourcePath().toString().replace('\\', '/'), file.printAll());
        });
        return after;
    }
}
