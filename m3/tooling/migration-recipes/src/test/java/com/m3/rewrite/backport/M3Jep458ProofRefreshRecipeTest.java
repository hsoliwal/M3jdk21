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

final class M3Jep458ProofRefreshRecipeTest {
    private static final String WORKFLOW =
            ".github/workflows/m3-jep458-current-master.yml";
    private static final String RECEIPT =
            "m3/backports/recipes/jep-458-current/CURRENT_TREE_RECEIPT.tsv";

    @Test
    void exactProofPreimagesTransformAndSecondPassIsFixedPoint() {
        var recipe = new M3Jep458ProofRefreshRecipe();
        List<SourceFile> before =
                List.of(
                        text(WORKFLOW, M3Jep458ProofRefreshRecipe.before(WORKFLOW)),
                        text(RECEIPT, M3Jep458ProofRefreshRecipe.before(RECEIPT)));

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
                M3Jep458ProofRefreshRecipe.after(WORKFLOW),
                find(after, WORKFLOW).printAll());
        assertEquals(
                M3Jep458ProofRefreshRecipe.after(RECEIPT),
                find(after, RECEIPT).printAll());
    }

    @Test
    void reviewedPostimagesAreAlreadyFixedPoint() {
        var recipe = new M3Jep458ProofRefreshRecipe();
        List<SourceFile> after =
                List.of(
                        text(WORKFLOW, M3Jep458ProofRefreshRecipe.after(WORKFLOW)),
                        text(RECEIPT, M3Jep458ProofRefreshRecipe.after(RECEIPT)));

        assertTrue(
                recipe.run(new InMemoryLargeSourceSet(after), context(), 1)
                        .getChangeset()
                        .getAllResults()
                        .isEmpty());
    }

    @Test
    void driftMissingAndDuplicateTargetsFailClosed() {
        var recipe = new M3Jep458ProofRefreshRecipe();

        assertThrows(
                RuntimeException.class,
                () ->
                        recipe.run(
                                        new InMemoryLargeSourceSet(
                                                List.of(
                                                        text(
                                                                WORKFLOW,
                                                                M3Jep458ProofRefreshRecipe.before(
                                                                                WORKFLOW)
                                                                        + "\n# drift\n"),
                                                        text(
                                                                RECEIPT,
                                                                M3Jep458ProofRefreshRecipe.before(
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
                                                                M3Jep458ProofRefreshRecipe.before(
                                                                        RECEIPT)))),
                                        context(),
                                        1)
                                .getChangeset()
                                .getAllResults());

        List<SourceFile> duplicate = new ArrayList<>();
        duplicate.add(text(WORKFLOW, M3Jep458ProofRefreshRecipe.before(WORKFLOW)));
        duplicate.add(text(WORKFLOW, M3Jep458ProofRefreshRecipe.before(WORKFLOW)));
        duplicate.add(text(RECEIPT, M3Jep458ProofRefreshRecipe.before(RECEIPT)));
        assertThrows(
                RuntimeException.class,
                () ->
                        recipe.run(
                                        new InMemoryLargeSourceSet(duplicate), context(), 1)
                                .getChangeset()
                                .getAllResults());
    }

    @Test
    void scopeIsProofOnlyAndProductSourceAuthorityStaysFalse() {
        var recipe = new M3Jep458ProofRefreshRecipe();

        assertEquals(Set.of(WORKFLOW, RECEIPT), M3Jep458ProofRefreshRecipe.targetPaths());
        assertFalse(recipe.productSourceMutationAuthority());
        assertFalse(recipe.promotionAuthority());
        assertTrue(recipe.getTags().contains("proof-refresh"));
        assertEquals(1, recipe.maxCycles());
    }

    @Test
    void postimagesBindCurrentMasterAndProofBranch() {
        String receipt = M3Jep458ProofRefreshRecipe.after(RECEIPT);
        String workflow = M3Jep458ProofRefreshRecipe.after(WORKFLOW);

        assertTrue(
                receipt.contains(
                        "base_commit\tf96b9a88e040cded3a5ffb96190e2acedee6c97d"));
        assertTrue(
                receipt.contains(
                        "proof_refresh_base\tf96b9a88e040cded3a5ffb96190e2acedee6c97d"));
        assertTrue(
                receipt.contains(
                        "proof_branch\tm3/jep458-proof-refresh-20261007"));
        assertTrue(workflow.contains("m3/jep458-proof-refresh-20261007"));
        assertTrue(workflow.contains("Refuse JEP 458 product-source drift in proof refresh"));
        assertTrue(workflow.contains("M3Jep458BackportRecipeTest"));
        assertTrue(workflow.contains("make JOBS=2 images"));
        assertTrue(
                workflow.contains(
                        "test/langtools/tools/javac/launcher test/langtools/tools/jdeps/listdeps/ListModuleDeps.java"));
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
