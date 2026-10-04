// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import java.util.List;
import java.util.Set;
import org.openrewrite.Recipe;

/**
 * Source-sealed control-plane recipe for the candidate-only backport DAG export.
 *
 * <p>The child owns only UTF-8 control/evidence files. It cannot mutate JDK product sources and
 * carries no promotion authority.</p>
 */
public final class M3BackportRecipeDagExportRecipe extends Recipe {
    public static final String CRATE = "backport-recipe-dag-export";

    @Override
    public String getDisplayName() {
        return "Install candidate-only M3JDK21 backport recipe DAG export";
    }

    @Override
    public String getDescription() {
        return "Replays the reviewed backport DAG exporter, unit proof, documentation and workflow "
                + "only from exact source preimages or absent additive targets.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3",
                "jdk21",
                "backport",
                "recipe-dag",
                "openrewrite",
                "candidate-only",
                "control-plane");
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(new M3Jdk21HashPinnedTextSnapshotRecipe(CRATE));
    }

    public boolean productSourceMutationAuthority() {
        return false;
    }

    public boolean promotionAuthority() {
        return false;
    }
}
