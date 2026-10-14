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

final class M3Jep475InventoryReceiverRecipeTest {
    private static final String WORKFLOW =
            ".github/workflows/m3-jep475-late-barrier-g1-inventory.yml";
    private static final String PATHS =
            "m3/backports/recipes/jep-475-late-barrier-g1/INTEGRATION_PATHS.txt";
    private static final String POLICY =
            "m3/backports/recipes/jep-475-late-barrier-g1/MATERIALIZATION_POLICY.tsv";
    private static final String PLAN =
            "m3/backports/recipes/jep-475-late-barrier-g1/PACKET_PLAN.tsv";
    private static final String README =
            "m3/backports/recipes/jep-475-late-barrier-g1/README.md";
    private static final String TEST =
            "m3/backports/test_jep475_inventory_policy.py";

    @Test
    void additivePacketGeneratesSixTargetsAndReachesFixedPoint() {
        var wrapper = new M3Jep475InventoryReceiverRecipe();
        assertEquals(1, wrapper.maxCycles());
        assertEquals(1, wrapper.getRecipeList().size());

        var receiver =
                (M3Jdk21HashPinnedTextSnapshotRecipe) wrapper.getRecipeList().getFirst();
        assertEquals("jdk21-jep475-late-barrier-inventory", receiver.getCrateName());
        assertEquals(List.of(WORKFLOW, PATHS, POLICY, PLAN, README, TEST), receiver.targetPaths());

        var first =
                wrapper.run(new InMemoryLargeSourceSet(List.of()), context(), 1);
        var changes = first.getChangeset().getAllResults();
        assertEquals(6, changes.size());
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
    void divergentPreexistingTargetFailsClosed() {
        var wrapper = new M3Jep475InventoryReceiverRecipe();
        var drift =
                PlainText.builder()
                        .sourcePath(Path.of(POLICY))
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
    void packetIsEvidenceOnly() {
        var wrapper = new M3Jep475InventoryReceiverRecipe();

        assertTrue(wrapper.getTags().contains("candidate-only"));
        assertTrue(wrapper.getDescription().contains("no JDK product source is changed"));
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(
                error -> {
                    throw new AssertionError(error);
                });
    }
}
