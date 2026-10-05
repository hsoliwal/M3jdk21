// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.util.List;
import java.util.Set;
import org.openrewrite.Recipe;

/** Provider Callback Reconciliation: the frozen-current configuration of the existing Pcs owner. */
public final class Pcr extends Recipe {
    @Override
    public String getDisplayName() {
        return "Reconcile Pcr provider callback superset candidate";
    }

    @Override
    public String getDescription() {
        return "Retains landed collection repairs and produces the twenty-target current provider "
                + "candidate through the existing strict Pcs source envelope and snapshot recipe.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of("synexia", "m3", "openrewrite", "recipe-first", "candidate-only", "m3-capability:pcr");
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public boolean causesAnotherCycle() {
        return false;
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(Pcs.reconciled());
    }
}
