// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.a3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.text.PlainText;

class M3A3ChallengeCategoryRecipeTest {

    @Test
    void exactBatchTransformsAndSecondRunIsFixedPoint() {
        var errors = new ArrayList<Throwable>();
        var context = new InMemoryExecutionContext(errors::add);
        List<SourceFile> before = List.of(
                javaSource(
                        M3A3ChallengeCategoryRecipe.PLAN,
                        M3A3ChallengeCategoryRecipe.resource("A3Plan.java.before"),
                        context),
                javaSource(
                        M3A3ChallengeCategoryRecipe.TEST,
                        M3A3ChallengeCategoryRecipe.resource("A3PlanTest.java.before"),
                        context),
                PlainText.builder()
                        .sourcePath(Path.of(M3A3ChallengeCategoryRecipe.DOC))
                        .text(M3A3ChallengeCategoryRecipe.resource("a3.md.before"))
                        .build());

        var first =
                new M3A3ChallengeCategoryRecipe()
                        .run(new InMemoryLargeSourceSet(before), context);
        assertTrue(errors.isEmpty(), errors.toString());
        var changes = first.getChangeset().getAllResults();
        assertEquals(4, changes.size());

        List<SourceFile> after = changes.stream()
                .map(result -> result.getAfter())
                .toList();
        assertTrue(
                after.stream()
                        .anyMatch(
                                source ->
                                        canonical(source)
                                                        .equals(M3A3ChallengeCategoryRecipe.CATALOGUE)
                                                && source.printAll().contains(
                                                        "LEETCODE\t1")
                                                && source.printAll().contains(
                                                        "HACKERRANK\t2")
                                                && source.printAll().contains(
                                                        "GEEKSFORGEEKS\t3")));

        var repeat =
                new M3A3ChallengeCategoryRecipe()
                        .run(new InMemoryLargeSourceSet(after), context);
        assertTrue(errors.isEmpty(), errors.toString());
        assertTrue(repeat.getChangeset().getAllResults().isEmpty());
    }

    @Test
    void postimagesAreHashPinnedAndChallengeAuthorityRemainsFalse() {
        assertEquals(
                "8a3df20715ea9c1372146f0c56e73b8daabe8619",
                M3A3ChallengeCategoryRecipe.gitBlob(
                        M3A3ChallengeCategoryRecipe.resource("A3Plan.java.after")));
        assertEquals(
                "200365eeffcf3578d7b7c19f41076992a894c2df",
                M3A3ChallengeCategoryRecipe.gitBlob(
                        M3A3ChallengeCategoryRecipe.resource("A3PlanTest.java.after")));
        assertEquals(
                "89bbe2d551196579c3126587366bb8499ae780d0",
                M3A3ChallengeCategoryRecipe.gitBlob(
                        M3A3ChallengeCategoryRecipe.resource(
                                "CHALLENGE_CATEGORY_EVIDENCE.tsv.after")));
        assertEquals(
                "075316c1fcfb692af366e5e287d70107f8ad6527",
                M3A3ChallengeCategoryRecipe.gitBlob(
                        M3A3ChallengeCategoryRecipe.resource("a3.md.after")));

        var recipe = new M3A3ChallengeCategoryRecipe();
        assertFalse(recipe.challengeSolutionCopyAuthority());
        assertFalse(recipe.sourceMutationAuthority());
        assertFalse(recipe.promotionAuthority());
    }

    @Test
    void driftAndPartialBatchFailClosed() {
        var context = new InMemoryExecutionContext();
        SourceFile drift = javaSource(
                M3A3ChallengeCategoryRecipe.PLAN,
                M3A3ChallengeCategoryRecipe.resource("A3Plan.java.before")
                        + "\n// drift\n",
                context);
        assertThrows(
                RuntimeException.class,
                () ->
                        new M3A3ChallengeCategoryRecipe()
                                .run(
                                        new InMemoryLargeSourceSet(List.of(drift)),
                                        context));

        SourceFile onlyPlan = javaSource(
                M3A3ChallengeCategoryRecipe.PLAN,
                M3A3ChallengeCategoryRecipe.resource("A3Plan.java.before"),
                context);
        assertThrows(
                RuntimeException.class,
                () ->
                        new M3A3ChallengeCategoryRecipe()
                                .run(
                                        new InMemoryLargeSourceSet(List.of(onlyPlan)),
                                        context));
    }

    private static SourceFile javaSource(
            String path, String text, InMemoryExecutionContext context) {
        try (var parsed =
                JavaParser.fromJavaVersion()
                        .build()
                        .parseInputs(
                                List.of(Parser.Input.fromString(Path.of(path), text)),
                                null,
                                context)) {
            List<SourceFile> files = parsed.toList();
            assertEquals(1, files.size());
            return files.getFirst();
        }
    }

    private static String canonical(SourceFile source) {
        return source.getSourcePath().normalize().toString().replace('\\', '/');
    }
}
