// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;

final class M3Jep485ProofRefreshRecipeTest {
    private static final String WORKFLOW =
            ".github/workflows/m3-jep485-stream-gatherers.yml";
    private static final String RECEIPT =
            "m3/backports/recipes/j485/CURRENT_TREE_RECEIPT.tsv";

    @Test
    void exactPreimagesTransformAndSecondPassIsFixedPoint() {
        var recipe = new M3Jep485ProofRefreshRecipe();
        List<SourceFile> before =
                List.of(
                        text(WORKFLOW, M3Jep485ProofRefreshRecipe.before(WORKFLOW)),
                        text(RECEIPT, M3Jep485ProofRefreshRecipe.before(RECEIPT)));

        var first =
                recipe.run(new InMemoryLargeSourceSet(before), context(), 1)
                        .getChangeset()
                        .getAllResults();
        assertEquals(2, first.size());

        List<SourceFile> after =
                first.stream().map(result -> result.getAfter()).toList();
        assertTrue(
                recipe.run(new InMemoryLargeSourceSet(after), context(), 1)
                        .getChangeset()
                        .getAllResults()
                        .isEmpty());
        assertEquals(
                M3Jep485ProofRefreshRecipe.after(WORKFLOW),
                find(after, WORKFLOW).printAll());
        assertEquals(
                M3Jep485ProofRefreshRecipe.after(RECEIPT),
                find(after, RECEIPT).printAll());
    }

    @Test
    void reviewedPostimagesAreAlreadyFixedPoint() {
        var recipe = new M3Jep485ProofRefreshRecipe();
        List<SourceFile> after =
                List.of(
                        text(WORKFLOW, M3Jep485ProofRefreshRecipe.after(WORKFLOW)),
                        text(RECEIPT, M3Jep485ProofRefreshRecipe.after(RECEIPT)));

        assertTrue(
                recipe.run(new InMemoryLargeSourceSet(after), context(), 1)
                        .getChangeset()
                        .getAllResults()
                        .isEmpty());
    }

    @Test
    void driftMissingAndDuplicateTargetsFailClosed() {
        var recipe = new M3Jep485ProofRefreshRecipe();

        assertThrows(
                RuntimeException.class,
                () ->
                        recipe.run(
                                        new InMemoryLargeSourceSet(
                                                List.of(
                                                        text(
                                                                WORKFLOW,
                                                                M3Jep485ProofRefreshRecipe.before(
                                                                                WORKFLOW)
                                                                        + "\n# drift\n"),
                                                        text(
                                                                RECEIPT,
                                                                M3Jep485ProofRefreshRecipe.before(
                                                                        RECEIPT)))),
                                        context(),
                                        1)
                                .getChangeset()
                                .getAllResults());

        assertThrows(
                RuntimeException.class,
                () ->
                        recipe.run(
                                        new InMemoryLargeSourceSet(
                                                List.of(
                                                        text(
                                                                RECEIPT,
                                                                M3Jep485ProofRefreshRecipe.before(
                                                                        RECEIPT)))),
                                        context(),
                                        1)
                                .getChangeset()
                                .getAllResults());

        List<SourceFile> duplicate = new ArrayList<>();
        duplicate.add(text(WORKFLOW, M3Jep485ProofRefreshRecipe.before(WORKFLOW)));
        duplicate.add(text(WORKFLOW, M3Jep485ProofRefreshRecipe.before(WORKFLOW)));
        duplicate.add(text(RECEIPT, M3Jep485ProofRefreshRecipe.before(RECEIPT)));
        assertThrows(
                RuntimeException.class,
                () ->
                        recipe.run(
                                        new InMemoryLargeSourceSet(duplicate), context(), 1)
                                .getChangeset()
                                .getAllResults());
    }

    @Test
    void postimagesBindCurrentMasterAndRepairReceiptReadbackExpressions() {
        String workflow = M3Jep485ProofRefreshRecipe.after(WORKFLOW);
        String receipt = M3Jep485ProofRefreshRecipe.after(RECEIPT);

        assertFalse(workflow.contains("\\${{"));
        assertTrue(workflow.contains("M3Jep485ProofRefreshRecipeTest"));
        assertTrue(workflow.contains("${{ github.event_name == 'push'"));
        assertTrue(workflow.contains("BRANCH_NAME: ${{ github.ref_name }}"));
        assertTrue(workflow.contains("${{ contains(github.event.head_commit.message"));

        assertTrue(
                receipt.contains(
                        "proof_base\t3799df0b88d169064b610481400ec4edacec1d2e"));
        assertTrue(receipt.contains("proof_requested\t2026-10-13"));
        assertTrue(
                receipt.contains(
                        "proof_branch\tm3/jep485-proof-refresh-20261013"));
        assertTrue(receipt.contains("product_targets\t7"));
        assertTrue(receipt.contains("product_blob_matches\t7"));
        assertTrue(receipt.contains("previous_failed_run\t37581932480"));
        assertTrue(
                receipt.contains(
                        "previous_failures\tPRIVATE_SYNEXIA_FETCH_PLUS_STALE_STRING_MINDEX_BOOTSTRAP"));
        assertTrue(receipt.contains("promotion\tNOT_AUTHORIZED"));
    }

    @Test
    void scopeIsProofOnly() {
        var recipe = new M3Jep485ProofRefreshRecipe();

        assertEquals(Set.of(WORKFLOW, RECEIPT), M3Jep485ProofRefreshRecipe.targetPaths());
        assertFalse(recipe.productSourceMutationAuthority());
        assertFalse(recipe.publicApiMutationAuthority());
        assertFalse(recipe.promotionAuthority());
        assertTrue(recipe.getTags().contains("proof-refresh"));
        assertEquals(1, recipe.maxCycles());
    }

    private static PlainText text(String path, String source) {
        return PlainText.builder().sourcePath(Path.of(path)).text(source).build();
    }

    private static SourceFile find(List<SourceFile> files, String path) {
        return files.stream()
                .filter(
                        file ->
                                file.getSourcePath()
                                        .toString()
                                        .replace('\\', '/')
                                        .equals(path))
                .findFirst()
                .orElseThrow();
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(
                error -> {
                    throw new AssertionError(error);
                });
    }
}
