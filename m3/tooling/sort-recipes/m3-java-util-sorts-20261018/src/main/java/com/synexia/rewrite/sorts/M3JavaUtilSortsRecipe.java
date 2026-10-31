// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
package com.synexia.rewrite.sorts;

import com.synexia.rewrite.M3HashPinnedJavaSnapshotRecipe;
import com.synexia.rewrite.M3HashPinnedTextSnapshotRecipe;
import java.util.List;
import java.util.Set;
import org.openrewrite.Recipe;

/** Task-bound composition; the canonical Java/text engines retain all mutation authority. */
public final class M3JavaUtilSortsRecipe extends Recipe {
    public static final String CRATE = "m3-java-util-sorts-20261018";
    public static final String TARGET_BASE = "2f2beb2eff7a88f1f7c0012fd17a78a6f0a00b49";
    public static final String ENGINE_BASE = "140c263a90f9803b9c46103c63355a286e0515f0";

    @Override public String getDisplayName() {
        return "M3JDK21 additive java.util sorting candidate";
    }

    @Override public String getDescription() {
        return "Composes the existing canonical hash-pinned Java and text recipes over the "
                + "reviewed sorting packet, preserving existing JDK owners and receiving stages.";
    }

    @Override public Set<String> getTags() {
        return Set.of("synexia", "m3", "jdk21", "sorting", "recipe-first", "candidate-only");
    }

    @Override public int maxCycles() { return 1; }

    @Override public List<Recipe> getRecipeList() {
        return List.of(new M3HashPinnedJavaSnapshotRecipe(CRATE),
                new M3HashPinnedTextSnapshotRecipe(CRATE));
    }
}
