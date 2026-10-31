// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.file;

import com.synexia.rewrite.M3HashPinnedJavaSnapshotRecipe;
import java.util.List;
import java.util.Set;
import org.openrewrite.Recipe;

/** M3JDK21 target receiver for the hash-pinned M3File stream/delta substrate. */
public final class M3FileStreamDeltaReceiverRecipe extends Recipe {
    public static final String CRATE = "m3-file-shadow-stream-delta-20261020";

    @Override public String getDisplayName() {
        return "Receive M3File monitored shadow streams and delta substrate";
    }

    @Override public String getDescription() {
        return "Replays the exact reviewed M3JDK21 file-stream postimages from a hash-pinned "
                + "target-specific crate. Reusable donor/algorithm authority remains in Synexia.";
    }

    @Override public Set<String> getTags() {
        return Set.of("m3", "m3jdk21", "m3file", "shadow-stream", "delta",
                "recipe-first", "hash-pinned", "target-receiver");
    }

    @Override public List<Recipe> getRecipeList() {
        return List.of(new M3HashPinnedJavaSnapshotRecipe(CRATE));
    }
}
