// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
package com.m3.rewrite.backport;

import java.util.List;
import org.openrewrite.Recipe;

/** Target-local receiver of the canonical Synexia TornadoVM compiler repair. */
public final class M3TornadoBulkCompilerRepairReceiverRecipe extends Recipe {
    public static final String CRATE = "m3-tornado-bulk-compiler-20261014";

    @Override
    public String getDisplayName() {
        return "Receive M3 TornadoVM bulk compiler repair";
    }

    @Override
    public String getDescription() {
        return "Receives one exact source-sealed TornadoVM adapter Maven compiler repair; "
                + "actual JUnit and device execution remain independent target proof gates.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(new M3Jdk21HashPinnedTextSnapshotRecipe(CRATE));
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    public boolean promotionAuthority() {
        return false;
    }
}
