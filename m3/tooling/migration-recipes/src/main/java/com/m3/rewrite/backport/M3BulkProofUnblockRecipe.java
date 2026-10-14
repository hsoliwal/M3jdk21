// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import java.util.List;
import org.openrewrite.Recipe;

/**
 * Target-local thin receiver for the canonical Synexia bulk proof compiler-gate packet.
 */
public final class M3BulkProofUnblockRecipe extends Recipe {
    public static final String CRATE = "m3-bulk-proof-unblock-20261014";

    @Override
    public String getDisplayName() {
        return "Receive M3 bulk proof compiler gate repair";
    }

    @Override
    public String getDescription() {
        return "Replays exact source-sealed Java test literals and comment-only ASCII JDK fixes. "
                + "No scheduler, JDK semantic, JNI or promotion authority.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(new M3Jdk21HashPinnedSnapshotRecipe(CRATE));
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public boolean causesAnotherCycle() {
        return false;
    }

    public boolean promotionAuthority() {
        return false;
    }
}
