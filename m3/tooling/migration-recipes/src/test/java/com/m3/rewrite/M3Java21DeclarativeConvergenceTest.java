// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.openrewrite.java.Assertions.java;

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
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

/** Official-resource proof for the declarative A3 Java-21 convergence DAG. */
final class M3Java21DeclarativeConvergenceTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources(M3Java21ConvergenceCatalog.RECIPE);
    }

    @Test
    void resourceRecipeLeavesUnadmittedMethodsUntouched() {
        rewriteRun(
                java(
                        """
                        package example;
                        final class NoChange {
                            static int divide(int a, int b) {
                                return a / b;
                            }
                        }
                        """));
    }

    @Test
    void declarativeDagMatchesRetainedJavaCompatibilityOracleAndReachesFixedPoint() {
        String path = "src/main/java/example/Sample.java";
        String before =
                """
                package example;
                final class Sample {
                    private static int compute(int a, int b) {
                        return (a + b) * 31;
                    }
                }
                """;

        Map<String, String> declarative =
                apply(M3Java21ConvergenceCatalog.activate(), Map.of(path, before));
        Map<String, String> compatibility =
                apply(new M3Java21ConvergenceRecipe(), Map.of(path, before));

        assertEquals(compatibility, declarative);
        String after = declarative.get(path);
        assertTrue(after.contains("int m3$pureIntAtom ="));
        assertTrue(after.contains("M3-IOP: PURE_INT_EXPRESSION"));
        assertTrue(after.contains("M3-ATOM: m3$pureIntAtom"));
        assertTrue(apply(M3Java21ConvergenceCatalog.activate(), declarative).isEmpty());
    }

    private static Map<String, String> apply(
            Recipe recipe, Map<String, String> sources) {
        var context =
                new InMemoryExecutionContext(
                        error -> {
                            throw new AssertionError(error);
                        });
        List<Parser.Input> inputs = new ArrayList<>(sources.size());
        sources.forEach(
                (path, source) ->
                        inputs.add(
                                Parser.Input.fromString(
                                        Path.of(path), source)));
        List<SourceFile> parsed =
                JavaParser.fromJavaVersion()
                        .build()
                        .parseInputs(inputs, null, context)
                        .toList();

        var result =
                recipe.run(
                        new InMemoryLargeSourceSet(parsed),
                        context,
                        8);
        Map<String, String> after = new LinkedHashMap<>();
        result.getChangeset()
                .getAllResults()
                .forEach(
                        change -> {
                            SourceFile file = change.getAfter();
                            after.put(
                                    file.getSourcePath()
                                            .toString()
                                            .replace('\\', '/'),
                                    file.printAll());
                        });
        return after;
    }
}
