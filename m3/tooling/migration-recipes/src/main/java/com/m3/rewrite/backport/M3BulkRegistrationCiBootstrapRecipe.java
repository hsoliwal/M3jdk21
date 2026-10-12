// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
package com.m3.rewrite.backport;

import java.util.List;
import org.openrewrite.Recipe;

/** Thin receiver for Synexia's sealed M3JDK bulk-registration workflow repair. */
public final class M3BulkRegistrationCiBootstrapRecipe extends Recipe {
    public static final String CRATE = "m3-bulk-registration-ci-20261012";

    @Override
    public String getDisplayName() {
        return "Receive bulk registration CI bootstrap";
    }

    @Override
    public String getDescription() {
        return "Replays the pinned one-file workflow correction; preserves Maven and JDK proof.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(new M3Jdk21HashPinnedTextSnapshotRecipe(CRATE));
    }

    @Override
    public int maxCycles() { return 1; }

    public boolean promotionAuthority() { return false; }
}
