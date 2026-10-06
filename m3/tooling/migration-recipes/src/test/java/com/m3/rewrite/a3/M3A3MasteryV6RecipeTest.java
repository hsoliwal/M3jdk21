// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.a3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

final class M3A3MasteryV6RecipeTest {
    private static final String OWNER =
            """
            package com.m3.a3;
            public final class A3 {
                private A3() {}
            }
            """;

    @Test
    void createOnlyDeliveryInstallsExactGateAndProofThenReachesFixedPoint() {
        var context =
                new InMemoryExecutionContext(
                        error -> {
                            throw new AssertionError(error);
                        });
        SourceFile owner = parse("src/main/java/com/m3/a3/A3.java", OWNER, context);
        M3A3MasteryV6Recipe recipe = new M3A3MasteryV6Recipe();

        var first =
                recipe.run(
                        new InMemoryLargeSourceSet(List.of(owner)),
                        context);
        var changes = first.getChangeset().getAllResults();
        assertEquals(2, changes.size());
        assertEquals(
                recipe.targetPaths(),
                changes.stream()
                        .map(
                                result ->
                                        result.getAfter()
                                                .getSourcePath()
                                                .toString()
                                                .replace('\\', '/'))
                        .collect(java.util.stream.Collectors.toSet()));

        List<SourceFile> converged =
                java.util.stream.Stream.concat(
                                java.util.stream.Stream.of(owner),
                                changes.stream().map(org.openrewrite.Result::getAfter))
                        .toList();
        var second =
                new M3A3MasteryV6Recipe()
                        .run(
                                new InMemoryLargeSourceSet(converged),
                                context);
        assertTrue(second.getChangeset().getAllResults().isEmpty());

        assertTrue(
                M3A3MasteryV6Recipe.candidateSource(
                                "src/main/java/com/m3/a3/A3Gate.java")
                        .contains("class A3Gate"));
        assertTrue(
                M3A3MasteryV6Recipe.candidateSource(
                                "src/test/java/com/m3/a3/A3GateTest.java")
                        .contains("masteredApplyDelegatesToExistingA3Apply"));
        assertFalse(recipe.sourceMutationAuthority());
        assertFalse(recipe.donorSourceCopyAuthority());
        assertFalse(recipe.replacementAuthority());
        assertFalse(recipe.promotionAuthority());
    }

    @Test
    void foreignExistingGateFailsClosedAndForeignModuleDoesNothing() {
        var context = new InMemoryExecutionContext();
        SourceFile owner = parse("src/main/java/com/m3/a3/A3.java", OWNER, context);
        SourceFile drift =
                parse(
                        "src/main/java/com/m3/a3/A3Gate.java",
                        """
                        package com.m3.a3;
                        public final class A3Gate {}
                        """,
                        context);

        assertThrows(
                IllegalStateException.class,
                () ->
                        new M3A3MasteryV6Recipe()
                                .run(
                                        new InMemoryLargeSourceSet(
                                                List.of(owner, drift)),
                                        context)
                                .getChangeset()
                                .getAllResults());

        SourceFile foreign =
                parse(
                        "src/main/java/other/Other.java",
                        "package other; final class Other {}\n",
                        context);
        var run =
                new M3A3MasteryV6Recipe()
                        .run(
                                new InMemoryLargeSourceSet(List.of(foreign)),
                                context);
        assertTrue(run.getChangeset().getAllResults().isEmpty());
    }

    private static SourceFile parse(
            String path,
            String source,
            InMemoryExecutionContext context) {
        try (var parsed =
                JavaParser.fromJavaVersion()
                        .build()
                        .parseInputs(
                                List.of(
                                        Parser.Input.fromString(
                                                Path.of(path), source)),
                                null,
                                context)) {
            List<SourceFile> files = parsed.toList();
            assertEquals(1, files.size());
            return files.getFirst();
        }
    }
}
