// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import java.util.List;
import org.openrewrite.Recipe;

/**
 * Target-local receiving adapter, reusing existing JDK OpenRewrite hash-pinned engines.
 * Synexia retains canonical authoring/provenance authority.
 */
public final class M3XmxReceiverRecipe extends Recipe {
    public static final String CRATE = "m3-xmx-receiver";

    @Override public String getDisplayName() {
        return "Receive actual-Xmx pressure proof from Synexia";
    }

    @Override public String getDescription() {
        return "Adds a target-only JUnit oracle for the existing M3ObjectResidency heap owner "
                + "and a pending Synexia evidence receipt, without changing runtime semantics.";
    }

    @Override public List<Recipe> getRecipeList() {
        return List.of(
                new M3Jdk21HashPinnedSnapshotRecipe(CRATE),
                new M3Jdk21HashPinnedTextSnapshotRecipe(CRATE));
    }

    @Override public int maxCycles() { return 1; }

    public boolean promotionAuthority() { return false; }
}
