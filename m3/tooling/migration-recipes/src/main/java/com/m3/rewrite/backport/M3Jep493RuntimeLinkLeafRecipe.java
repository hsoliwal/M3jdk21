// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import java.util.List;
import java.util.Set;
import org.openrewrite.Recipe;

/**
 * Additive GA-final internal runtime-link leaf for JEP 493.
 *
 * <p>This recipe deliberately installs only the five Java classes that are absent from the
 * Java-21 target and still present at JDK 24 GA. Existing jlink owners, build wiring, resources and
 * tests are separate later atoms. The implementation-commit RuntimeImageLinkException source is
 * excluded because that path is absent at the final GA donor state.</p>
 */
public final class M3Jep493RuntimeLinkLeafRecipe extends Recipe {
    public static final String DONOR_TAG = "jdk-24+36";
    public static final String DONOR_COMMIT =
            "6705a9255d28f351950e7fbca9d05e73942a4e27";
    public static final String CRATE =
            "jdk24-jep493-runtimelink-leaf";

    @Override
    public String getDisplayName() {
        return "Add JEP 493 GA runtime-link implementation leaf";
    }

    @Override
    public String getDescription() {
        return "Adds only the five GA-final internal jdk.jlink runtime-link Java classes from "
                + "ABSENT Java-21 preimages; existing jlink/build integration remains separate.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3",
                "jdk21",
                "backport",
                "jep-493",
                "jdk.jlink",
                "runtime-image",
                "ga-final",
                "hash-pinned",
                "module",
                "candidate-only");
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(new M3Jdk21HashPinnedSnapshotRecipe(CRATE));
    }
}
