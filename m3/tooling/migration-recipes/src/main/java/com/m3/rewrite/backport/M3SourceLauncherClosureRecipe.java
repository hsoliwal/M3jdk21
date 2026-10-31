// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import java.util.List;
import org.openrewrite.Recipe;

/** M3JDK receiver of the exact Synexia source-built launcher bootstrap repair. */
public final class M3SourceLauncherClosureRecipe extends Recipe {
    public static final String CRATE = "m3-source-launcher-closure-20261013";

    @Override
    public String getDisplayName() {
        return "Receive M3 source-built launcher closure";
    }

    @Override
    public String getDescription() {
        return "Repairs exact shared recipe-custody dependency closure using the pinned Synexia "
                + "installer and test postimages; Maven/JUnit execution remains target-authoritative.";
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
