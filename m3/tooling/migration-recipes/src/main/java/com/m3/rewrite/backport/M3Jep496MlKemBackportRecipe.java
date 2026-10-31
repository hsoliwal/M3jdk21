// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import java.util.List;
import java.util.Set;
import org.openrewrite.Recipe;

/**
 * Target-specific M3JDK21 receiver for JEP 496 ML-KEM.
 *
 * <p>The reusable hash-pinned mechanics remain canonical in Synexia; this class binds the exact
 * Java-21 SHAKE-branch preimages to reviewed ML-KEM product/test/vector postimages. Public
 * NamedParameterSpec constants are an explicit opt-in library API extension and therefore require
 * LIBRARY_API authority.</p>
 */
public final class M3Jep496MlKemBackportRecipe extends Recipe {
    public static final String NAMED_KEY_PREREQUISITE =
            "3f53d571343792341481f4d15970cdc0bcd76a5e";
    public static final String SHAKE_XOF_PREREQUISITE =
            "c54fc08aa3c63e4b26dc5edb2436844dfd3bab7c";
    public static final String FEATURE_COMMIT =
            "13987b4244614d594dc8f94c288eddb6239a066f";
    public static final String ACVP_MECHANICS_REFERENCE =
            "f400896822c2704d8e7c66afc1efa8a4fa91acb6";

    @Override
    public String getDisplayName() {
        return "Prepare JEP 496 ML-KEM backport for M3JDK21";
    }

    @Override
    public String getDescription() {
        return "Replays seven exact Java product/test postimages plus two pinned FIPS-203 vector "
                + "resources from the materialized SHAKE/XOF Java-21 receiver state.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3",
                "jdk21",
                "backport",
                "jep-496",
                "ml-kem",
                "fips-203",
                "security",
                "hash-pinned",
                "library-api",
                "candidate-only");
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new M3Jdk21HashPinnedSnapshotRecipe("jdk24-jep496-mlkem"),
                new M3Jdk21HashPinnedTextSnapshotRecipe("jdk24-jep496-mlkem-vectors"));
    }
}
