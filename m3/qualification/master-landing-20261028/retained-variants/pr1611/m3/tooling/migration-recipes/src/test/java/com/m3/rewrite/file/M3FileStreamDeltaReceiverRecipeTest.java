// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.synexia.rewrite.M3HashPinnedJavaSnapshotRecipe;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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

    @Test
    void crateCarriesAppendOnlyRecipeMemory() throws IOException {
        String resource =
                "/com/synexia/rewrite/hash-pinned-java/"
                        + M3FileStreamDeltaReceiverRecipe.CRATE
                        + "/RECIPE_MEMORY.tsv";
        try (var input =
                M3FileStreamDeltaReceiverRecipeTest.class.getResourceAsStream(resource)) {
            assertTrue(input != null, "recipe memory must be packaged with the crate");
            String[] lines =
                    new String(input.readAllBytes(), StandardCharsets.UTF_8)
                            .strip()
                            .split("\\R");
            assertTrue(lines.length >= 3, "header plus initial/correction memory rows");
            assertEquals(
                    "utc\trecipe\ttrigger\tobserved\tcorrection\tcorrected_by\tguard\tlesson",
                    lines[0]);
            for (int index = 1; index < lines.length; index++) {
                assertEquals(8, lines[index].split("\\t", -1).length);
            }
        }
    }
}
