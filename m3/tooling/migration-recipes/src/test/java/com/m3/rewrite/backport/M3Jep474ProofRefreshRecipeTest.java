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

final class M3Jep474ProofRefreshRecipeTest {
    private static final String WORKFLOW =
            ".github/workflows/m3-jep474-zgc-equivalence.yml";
    private static final String RECEIPT =
            "m3/backports/recipes/jep-474-generational-zgc/CURRENT_TREE_RECEIPT.tsv";

    @Test
    void exactPreimagesTransformAndSecondPassIsFixedPoint() {
        var recipe = new M3Jep474ProofRefreshRecipe();
        List<SourceFile> before =
                List.of(
                        text(WORKFLOW, M3Jep474ProofRefreshRecipe.before(WORKFLOW)),
                        text(RECEIPT, M3Jep474ProofRefreshRecipe.before(RECEIPT)));

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
                M3Jep474ProofRefreshRecipe.after(WORKFLOW),
                find(after, WORKFLOW).printAll());
        assertEquals(
                M3Jep474ProofRefreshRecipe.after(RECEIPT),
                find(after, RECEIPT).printAll());
    }

    @Test
    void reviewedPostimagesAreAlreadyFixedPoint() {
        var recipe = new M3Jep474ProofRefreshRecipe();
        List<SourceFile> after =
                List.of(
                        text(WORKFLOW, M3Jep474ProofRefreshRecipe.after(WORKFLOW)),
                        text(RECEIPT, M3Jep474ProofRefreshRecipe.after(RECEIPT)));

        assertTrue(
                recipe.run(new InMemoryLargeSourceSet(after), context(), 1)
                        .getChangeset()
                        .getAllResults()
                        .isEmpty());
    }

    @Test
    void driftMissingAndDuplicateTargetsFailClosed() {
        var recipe = new M3Jep474ProofRefreshRecipe();

        assertThrows(
                RuntimeException.class,
                () ->
                        recipe.run(
                                        new InMemoryLargeSourceSet(
                                                List.of(
                                                        text(
                                                                WORKFLOW,
                                                                M3Jep474ProofRefreshRecipe.before(
                                                                                WORKFLOW)
                                                                        + "\n# drift\n"),
                                                        text(
                                                                RECEIPT,
                                                                M3Jep474ProofRefreshRecipe.before(
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
                                                                M3Jep474ProofRefreshRecipe.before(
                                                                        RECEIPT)))),
                                        context(),
                                        1)
                                .getChangeset()
                                .getAllResults());

        List<SourceFile> duplicate = new ArrayList<>();
        duplicate.add(text(WORKFLOW, M3Jep474ProofRefreshRecipe.before(WORKFLOW)));
        duplicate.add(text(WORKFLOW, M3Jep474ProofRefreshRecipe.before(WORKFLOW)));
        duplicate.add(text(RECEIPT, M3Jep474ProofRefreshRecipe.before(RECEIPT)));
        assertThrows(
                RuntimeException.class,
                () ->
                        recipe.run(
                                        new InMemoryLargeSourceSet(duplicate), context(), 1)
                                .getChangeset()
                                .getAllResults());
    }

    @Test
    void proofRefreshRepairsObservedAndLatentHarnessDefects() {
        String workflow = M3Jep474ProofRefreshRecipe.after(WORKFLOW);
        String receipt = M3Jep474ProofRefreshRecipe.after(RECEIPT);

        assertTrue(workflow.contains("m3/jep474-proof-refresh-20261007"));
        assertTrue(workflow.contains("actions/checkout@v5"));
        assertTrue(workflow.contains("actions/setup-java@v5"));
        assertTrue(
                workflow.contains(
                        "printf 'UPSTREAM=%s\\n' \"$RUNNER_TEMP/upstream-jdk\" >> \"$GITHUB_ENV\""));
        assertTrue(
                workflow.contains(
                        "printf 'M3_JAVA=%s\\n' \"$JAVA_BIN\" >> \"$GITHUB_ENV\""));
        assertFalse(workflow.contains("UPSTREAM=%s\\\\n"));
        assertFalse(workflow.contains("M3_JAVA=%s\\\\n"));
        assertTrue(workflow.contains("mkdir -p target"));

        assertTrue(
                receipt.contains(
                        "proof_refresh_base\t80b8534ad1e45002185fb3eeab233f98d3a7c0bd"));
        assertTrue(receipt.contains("previous_failed_run\t37409218603"));
        assertTrue(
                receipt.contains(
                        "previous_failure\tGITHUB_ENV_LITERAL_BACKSLASH_N_PATH"));
    }

    @Test
    void scopeIsProofOnly() {
        var recipe = new M3Jep474ProofRefreshRecipe();
        assertEquals(Set.of(WORKFLOW, RECEIPT), M3Jep474ProofRefreshRecipe.targetPaths());
        assertFalse(recipe.productSourceMutationAuthority());
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
