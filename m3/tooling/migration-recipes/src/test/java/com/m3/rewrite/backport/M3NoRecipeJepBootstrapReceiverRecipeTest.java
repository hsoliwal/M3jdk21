// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;

final class M3NoRecipeJepBootstrapReceiverRecipeTest {
    private static final String WORKFLOW =
            ".github/workflows/m3-high-risk-jep-recipe-bootstrap.yml";
    private static final String AUTHORITY =
            "m3/backports/HIGH_RISK_IMPLEMENTATION_AUTHORITY.tsv";
    private static final String DOC =
            "m3/backports/HIGH_RISK_RECIPE_BOOTSTRAP.md";
    private static final String SCRIPT =
            "m3/backports/high_risk_implementation_authority.py";
    private static final String TEST =
            "m3/backports/test_high_risk_implementation_authority.py";

    @Test
    void additiveBatchControlPlaneGeneratesAndReachesFixedPoint() {
        var wrapper = new M3NoRecipeJepBootstrapReceiverRecipe();
        assertEquals(1, wrapper.maxCycles());
        assertEquals(1, wrapper.getRecipeList().size());

        var receiver =
                (M3Jdk21HashPinnedTextSnapshotRecipe) wrapper.getRecipeList().getFirst();
        assertEquals("jdk21-no-recipe-jep-bootstrap", receiver.getCrateName());
        assertEquals(List.of(WORKFLOW, AUTHORITY, DOC, SCRIPT, TEST), receiver.targetPaths());

        var first = wrapper.run(new InMemoryLargeSourceSet(List.of()), context(), 1);
        var changes = first.getChangeset().getAllResults();
        assertEquals(5, changes.size());
        assertTrue(changes.stream().allMatch(change -> change.getBefore() == null));

        List<SourceFile> after =
                changes.stream()
                        .map(
                                change -> {
                                    assertNotNull(change.getAfter());
                                    return change.getAfter();
                                })
                        .toList();

        assertTrue(
                wrapper.run(new InMemoryLargeSourceSet(after), context(), 1)
                        .getChangeset()
                        .getAllResults()
                        .isEmpty());
    }

    @Test
    void divergentExistingAuthorityLedgerFailsClosed() {
        var wrapper = new M3NoRecipeJepBootstrapReceiverRecipe();
        var drift =
                PlainText.builder()
                        .sourcePath(Path.of(AUTHORITY))
                        .text("drift\n")
                        .build();

        assertThrows(
                RuntimeException.class,
                () ->
                        wrapper.run(
                                        new InMemoryLargeSourceSet(List.of(drift)),
                                        context(),
                                        1)
                                .getChangeset()
                                .getAllResults());
    }

    @Test
    void receiverIsAuthorityOnlyAndCandidateOnly() {
        var wrapper = new M3NoRecipeJepBootstrapReceiverRecipe();

        assertTrue(wrapper.getTags().contains("candidate-only"));
        assertTrue(wrapper.getDescription().contains("without editing JDK product source"));
        assertTrue(wrapper.getDescription().contains("compatibility/promotion"));
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(
                error -> {
                    throw new AssertionError(error);
                });
    }
}
