// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite;

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

/** Official resource-level proof for Nebula's declarative FILE-local convergence recipe. */
final class NebulaM3DeclarativeConvergenceTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources(NebulaM3ConvergenceCatalog.RECIPE);
    }

    @Test
    void unadmittedJavaIsUnchanged() {
        rewriteRun(
                java(
                        """
                        package example;
                        final class NoChange {
                            private static int divide(int a, int b) {
                                return a / b;
                            }
                        }
                        """));
    }

    @Test
    void declarativeDagMatchesRetainedJavaCompositeAndReachesFixedPoint() {
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
                apply(NebulaM3ConvergenceCatalog.activate(), Map.of(path, before));
        Map<String, String> compatibility =
                apply(new NebulaM3Java21ConvergenceRecipe(), Map.of(path, before));

        assertEquals(compatibility, declarative);
        String after = declarative.get(path);
        assertTrue(after.contains("m3$pureIntAtom"));
        assertTrue(after.contains("M3-IOP: PURE_INT_EXPRESSION"));
        assertTrue(after.contains("M3-ATOM:"));
        assertTrue(apply(NebulaM3ConvergenceCatalog.activate(), declarative).isEmpty());
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

        var run =
                recipe.run(
                        new InMemoryLargeSourceSet(parsed),
                        context,
                        8);
        Map<String, String> after = new LinkedHashMap<>();
        run.getChangeset()
                .getAllResults()
                .forEach(
                        result -> {
                            SourceFile file = result.getAfter();
                            after.put(
                                    file.getSourcePath()
                                            .toString()
                                            .replace('\\', '/'),
                                    file.printAll());
                        });
        return after;
    }
}
