// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import java.util.List;
import java.util.Set;
import org.openrewrite.Recipe;

/**
 * First additive Java-21-compatible implementation tranche for JEP 493.
 *
 * <p>This recipe installs only the final JDK 24 GA runtime-link core classes whose target
 * preimages are absent on the JDK 21 baseline. It deliberately does not modify JlinkTask,
 * ImageFileCreator, build configuration, resources, or tests; those remain later composition
 * passes with their own exact preimages and proof.</p>
 */
public final class M3Jep493RuntimeLinkCoreRecipe extends Recipe {
    public static final String UPSTREAM_IMPLEMENTATION_COMMIT =
            "2ec358082f0896480bdbfcb289b4ba2bff0dd828";
    public static final String DONOR_GA = "jdk-24+36";
    public static final String CRATE = "jdk24-jep493-runtime-link-core";

    @Override
    public String getDisplayName() {
        return "Add JEP 493 runtime-link core";
    }

    @Override
    public String getDescription() {
        return "Adds the five final-GA JEP 493 runtime-link Java classes from exact ABSENT "
                + "preimages without yet wiring jlink behavior.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3",
                "jdk21",
                "backport",
                "jep-493",
                "jlink",
                "runtime-image",
                "hash-pinned",
                "module-scope",
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
