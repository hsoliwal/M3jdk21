// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import java.util.List;
import java.util.Set;
import org.openrewrite.Recipe;

/** Source-sealed receiver for the JEP 475 Late Barrier Expansion for G1 inventory packet. */
public final class M3Jep475InventoryReceiverRecipe extends Recipe {
    @Override
    public String getDisplayName() {
        return "Receive JEP 475 G1 late-barrier inventory";
    }

    @Override
    public String getDescription() {
        return "Replays the reviewed JEP 475 exact-path inventory, policy, packet plan, test, and "
                + "CI workflow from hash-pinned text postimages; no JDK product source is changed.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3", "jdk21", "jep475", "g1", "c2", "backport", "inventory",
                "candidate-only", "fail-closed");
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new M3Jdk21HashPinnedTextSnapshotRecipe(
                        "jdk21-jep475-late-barrier-inventory"));
    }
}
