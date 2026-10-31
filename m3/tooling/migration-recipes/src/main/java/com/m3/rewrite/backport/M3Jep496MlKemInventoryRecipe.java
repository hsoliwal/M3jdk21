// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import java.util.List;
import java.util.Set;
import org.openrewrite.Recipe;

/**
 * Inventory-only JEP 496 ML-KEM donor/released-state packet.
 *
 * <p>This recipe materializes evidence and CI only. Product source remains untouched until the
 * released-state FILE atoms generated from this packet are separately reviewed.</p>
 */
public final class M3Jep496MlKemInventoryRecipe extends Recipe {
    public static final String IMPLEMENTATION_COMMIT =
            "13987b4244614d594dc8f94c288eddb6239a066f";
    public static final String JDK24_GA_COMMIT =
            "6705a9255d28f351950e7fbca9d05e73942a4e27";
    public static final String JEP497_COMMIT =
            "8b98f958dc1afedc02b9d9c98089d6cb1ca3a5b7";

    @Override
    public String getDisplayName() {
        return "Inventory JEP 496 ML-KEM released-state packet";
    }

    @Override
    public String getDescription() {
        return "Materializes the JEP 496 implementation denominator, JDK24-GA state, shared-owner "
                + "dependency closure, and FILE-atom generation workflow without changing JDK product source.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3",
                "jdk21",
                "jep-496",
                "ml-kem",
                "security",
                "inventory",
                "released-state",
                "candidate-only");
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new M3Jdk21HashPinnedTextSnapshotRecipe(
                        "jep496-mlkem-inventory-20261007"));
    }

    public boolean productSourceMutationAuthority() {
        return false;
    }

    public boolean donorSourceCopyAuthority() {
        return false;
    }

    public boolean promotionAuthority() {
        return false;
    }
}
