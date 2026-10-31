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

class M3FileLayeredFactsReceiverRecipeTest {
    @Test
    void delegatesToLayeredFactsCrate() {
        M3FileLayeredFactsReceiverRecipe receiver = new M3FileLayeredFactsReceiverRecipe();
        assertEquals(1, receiver.getRecipeList().size());
        M3HashPinnedJavaSnapshotRecipe recipe =
                assertInstanceOf(
                        M3HashPinnedJavaSnapshotRecipe.class,
                        receiver.getRecipeList().getFirst());
        assertEquals(M3FileLayeredFactsReceiverRecipe.CRATE, recipe.getCrateName());
    }

    @Test
    void correctionIsRecordedWithExecutableGuard() throws IOException {
        String resource =
                "/com/synexia/rewrite/hash-pinned-java/"
                        + M3FileLayeredFactsReceiverRecipe.CRATE
                        + "/RECIPE_MEMORY.tsv";
        try (var input =
                M3FileLayeredFactsReceiverRecipeTest.class.getResourceAsStream(resource)) {
            assertTrue(input != null);
            String text = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(text.contains("M3_FILE_FACT_IDENTICAL_DUPLICATE_SHOULD_CONVERGE"));
            assertTrue(text.contains("identicalFactReceiptsConvergeButConflictsFail"));
        }
    }
}
