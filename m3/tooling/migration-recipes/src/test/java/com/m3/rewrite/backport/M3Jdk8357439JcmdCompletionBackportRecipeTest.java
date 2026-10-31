// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;

final class M3Jdk8357439JcmdCompletionBackportRecipeTest {
    private static final String COPY = "make/modules/jdk.jcmd/Copy.gmk";
    private static final String SCRIPT = "src/jdk.jcmd/share/conf/bash-completion/jcmd";

    @Test
    void recipeOwnsOneExactAdditiveTextCrate() {
        Recipe recipe = new M3Jdk8357439JcmdCompletionBackportRecipe();

        assertEquals(
                "8549d1896054dd230ba3038c83bce23b10dcda22",
                M3Jdk8357439JcmdCompletionBackportRecipe.UPSTREAM_COMMIT);
        assertEquals(1, recipe.getRecipeList().size());
        var text = assertInstanceOf(
                M3Jdk21HashPinnedTextSnapshotRecipe.class,
                recipe.getRecipeList().getFirst());
        assertEquals(M3Jdk8357439JcmdCompletionBackportRecipe.CRATE, text.getCrateName());
        assertEquals(1, recipe.maxCycles());
        assertTrue(recipe.getTags().contains("additive"));
        assertTrue(recipe.getTags().contains("candidate-only"));
    }

    @Test
    void absentJdk21PreimagesGenerateBothReviewedFilesThenReachFixedPoint() {
        Recipe recipe = new M3Jdk8357439JcmdCompletionBackportRecipe();

        var first = recipe.run(
                new InMemoryLargeSourceSet(List.of()),
                context(),
                1);
        List<Result> changes = first.getChangeset().getAllResults();

        assertEquals(2, changes.size());
        assertTrue(changes.stream().allMatch(result -> result.getBefore() == null));
        assertTrue(changes.stream().anyMatch(result ->
                COPY.equals(normalized(result.getAfter().getSourcePath()))
                        && result.getAfter().printAll().contains(
                                "COPY_JCMD_BASH_COMPLETION")));
        assertTrue(changes.stream().anyMatch(result ->
                SCRIPT.equals(normalized(result.getAfter().getSourcePath()))
                        && result.getAfter().printAll().contains(
                                "complete -o nosort -F _jcmd_completion jcmd")));

        List<SourceFile> after = changes.stream().map(Result::getAfter).toList();
        assertTrue(recipe.run(
                        new InMemoryLargeSourceSet(after),
                        context(),
                        1)
                .getChangeset()
                .getAllResults()
                .isEmpty());
    }

    @Test
    void unexpectedExistingTargetFailsClosedInsteadOfOverwriting() {
        SourceFile drift = PlainText.builder()
                .sourcePath(Path.of(COPY))
                .text("# unexpected local file\n")
                .build();

        assertThrows(
                RuntimeException.class,
                () -> new M3Jdk8357439JcmdCompletionBackportRecipe()
                        .run(
                                new InMemoryLargeSourceSet(List.of(drift)),
                                context(),
                                1)
                        .getChangeset()
                        .getAllResults());
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(error -> {
            throw new AssertionError(error);
        });
    }

    private static String normalized(Path path) {
        return path.toString().replace('\\', '/');
    }
}
