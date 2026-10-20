// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.synexia.rewrite.M3HashPinnedJavaSnapshotRecipe;
import org.junit.jupiter.api.Test;

class M3FileStreamDeltaReceiverRecipeTest {
    @Test
    void delegatesToOneHashPinnedCrate() {
        M3FileStreamDeltaReceiverRecipe receiver = new M3FileStreamDeltaReceiverRecipe();
        assertEquals(1, receiver.getRecipeList().size());
        M3HashPinnedJavaSnapshotRecipe recipe =
                assertInstanceOf(
                        M3HashPinnedJavaSnapshotRecipe.class,
                        receiver.getRecipeList().getFirst());
        assertEquals(M3FileStreamDeltaReceiverRecipe.CRATE, recipe.getCrateName());
    }
}
