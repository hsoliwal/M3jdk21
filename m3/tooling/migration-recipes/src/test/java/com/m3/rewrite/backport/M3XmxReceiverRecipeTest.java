// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.junit.jupiter.api.Test;

final class M3XmxReceiverRecipeTest {
    @Test
    void exactTargetOnlyOracleAndPendingReceipt() {
        var recipe = new M3XmxReceiverRecipe();
        assertEquals(2, recipe.getRecipeList().size());
        assertFalse(recipe.promotionAuthority());

        assertEquals(List.of(
                "m3/collections/src/test/java/com/m3/collections/M3ObjectResidencyRuntimePressureTest.java"),
                new M3Jdk21HashPinnedSnapshotRecipe(M3XmxReceiverRecipe.CRATE).targetPaths());
        assertEquals(List.of(
                "m3/compatibility/synexia-xmx-memory-receiver-20261014.tsv"),
                new M3Jdk21HashPinnedTextSnapshotRecipe(M3XmxReceiverRecipe.CRATE).targetPaths());
    }
}
