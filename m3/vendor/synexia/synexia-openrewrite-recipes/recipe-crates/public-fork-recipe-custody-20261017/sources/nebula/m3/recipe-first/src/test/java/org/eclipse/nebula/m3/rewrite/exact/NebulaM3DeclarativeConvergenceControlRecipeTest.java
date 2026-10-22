// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.exact;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

final class NebulaM3DeclarativeConvergenceControlRecipeTest {
    private static final String DAG =
            "src/main/java/org/eclipse/nebula/m3/rewrite/convergence/"
                    + "NebulaM3FileConvergenceRecipeDag.java";

    @Test
    void exactDagPreimageGeneratesAdditionsAndThenReachesFixedPoint() {
        var context = context();
        SourceFile before =
                parse(
                        DAG,
                        resource(
                                "/org/eclipse/nebula/m3/rewrite/exact/declarative-convergence/"
                                        + "NebulaM3FileConvergenceRecipeDag.java.before.txt"),
                        context);

        var first =
                new NebulaM3DeclarativeConvergenceControlRecipe()
                        .run(new InMemoryLargeSourceSet(List.of(before)), context, 2);
        assertEquals(3, first.getChangeset().getAllResults().size());

        List<SourceFile> after =
                first.getChangeset().getAllResults().stream()
                        .map(result -> result.getAfter())
                        .toList();
        assertTrue(
                after.stream()
                        .anyMatch(
                                source ->
                                        source.getSourcePath()
                                                .toString()
                                                .endsWith("NebulaM3ConvergenceCatalog.java")));
        assertTrue(
                after.stream()
                        .anyMatch(
                                source ->
                                        source.getSourcePath()
                                                .toString()
                                                .endsWith("NebulaM3DeclarativeConvergenceTest.java")));

        var repeat =
                new NebulaM3DeclarativeConvergenceControlRecipe()
                        .run(new InMemoryLargeSourceSet(after), context, 2);
        assertTrue(repeat.getChangeset().getAllResults().isEmpty());
    }

    @Test
    void driftedDagFailsClosed() {
        var context = context();
        SourceFile drift =
                parse(
                        DAG,
                        """
                        package org.eclipse.nebula.m3.rewrite.convergence;
                        public final class NebulaM3FileConvergenceRecipeDag {}
                        """,
                        context);

        assertThrows(
                RuntimeException.class,
                () ->
                        new NebulaM3DeclarativeConvergenceControlRecipe()
                                .run(
                                        new InMemoryLargeSourceSet(List.of(drift)),
                                        context,
                                        2));
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(
                failure -> {
                    throw new IllegalStateException(
                            "OpenRewrite execution refused", failure);
                });
    }

    private static SourceFile parse(
            String path, String source, InMemoryExecutionContext context) {
        List<SourceFile> parsed =
                JavaParser.fromJavaVersion()
                        .build()
                        .parseInputs(
                                List.of(
                                        Parser.Input.fromString(
                                                Path.of(path), source)),
                                null,
                                context)
                        .toList();
        assertEquals(1, parsed.size());
        return parsed.getFirst();
    }

    private static String resource(String path) {
        try (var input =
                NebulaM3DeclarativeConvergenceControlRecipeTest.class
                        .getResourceAsStream(path)) {
            if (input == null) {
                throw new IllegalStateException("missing test resource: " + path);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException("cannot read test resource", failure);
        }
    }
}
