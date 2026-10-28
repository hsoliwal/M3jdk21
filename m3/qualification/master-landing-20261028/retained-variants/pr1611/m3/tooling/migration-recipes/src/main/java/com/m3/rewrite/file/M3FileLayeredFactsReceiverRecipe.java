// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.file;

import com.synexia.rewrite.M3HashPinnedJavaSnapshotRecipe;
import java.util.List;
import java.util.Set;
import org.openrewrite.Recipe;

/** M3JDK21 target receiver for one-pass layered M3File fact receipts. */
public final class M3FileLayeredFactsReceiverRecipe extends Recipe {
    public static final String CRATE = "m3-file-layered-facts-20261020";

    @Override public String getDisplayName() {
        return "Receive M3File layered fact receipts";
    }

    @Override public String getDescription() {
        return "Replays exact M3JDK21 postimages for one-pass parser/index fact receipts and "
                + "compact sorted downstream deltas.";
    }

    @Override public Set<String> getTags() {
        return Set.of("m3", "m3jdk21", "m3file", "precompute", "delta",
                "recipe-first", "hash-pinned", "target-receiver");
    }

    @Override public List<Recipe> getRecipeList() {
        return List.of(new M3HashPinnedJavaSnapshotRecipe(CRATE));
    }
}
