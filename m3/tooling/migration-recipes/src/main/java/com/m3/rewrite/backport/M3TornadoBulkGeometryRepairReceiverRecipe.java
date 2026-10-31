// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import java.util.List;
import org.openrewrite.Recipe;

/** Thin target receiver for the canonical Synexia TornadoVM bulk geometry regression. */
public final class M3TornadoBulkGeometryRepairReceiverRecipe extends Recipe {
    public static final String CRATE = "m3-tornado-geometry-20261013";

    @Override
    public String getDisplayName() {
        return "Receive the qualified TornadoVM bulk geometry repair";
    }

    @Override
    public String getDescription() {
        return "Applies one exact reviewed JUnit geometry fixture postimage; never loosens "
                + "BulkTask admission or replaces CPU/TornadoVM parity proof.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(new M3Jdk21HashPinnedSnapshotRecipe(CRATE));
    }

    @Override
    public int maxCycles() { return 1; }

    public boolean promotionAuthority() { return false; }
}
