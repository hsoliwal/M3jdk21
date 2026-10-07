// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import java.util.List;
import java.util.Set;
import org.openrewrite.Recipe;

/**
 * Target-specific M3JDK21 receiver for JEP 497 ML-DSA.
 *
 * <p>The receiver is stacked on the reviewed JEP 496/SHAKE/named-key state. Feature-private
 * ML-DSA implementation classes use the released JDK24-GA FIPS-204-final donor state while shared
 * Java-21 owners receive only the bounded ML-DSA constants, OIDs and provider registrations.
 * Public NamedParameterSpec constants are an explicit opt-in Java-21 library API extension.</p>
 */
public final class M3Jep497MlDsaBackportRecipe extends Recipe {
    public static final String NAMED_KEY_PREREQUISITE =
            "3f53d571343792341481f4d15970cdc0bcd76a5e";
    public static final String SHAKE_XOF_PREREQUISITE =
            "c54fc08aa3c63e4b26dc5edb2436844dfd3bab7c";
    public static final String FEATURE_COMMIT =
            "8b98f958dc1afedc02b9d9c98089d6cb1ca3a5b7";
    public static final String ML_KEM_COEXISTENCE =
            "8c2b4f62714f26ab3bc4808c734502af632a1eef";
    public static final String FIPS_204_FINAL =
            "fb95a5394413dba7352a7ad2ebd39a3da42308a6";
    public static final String RELEASED_STATE =
            "6705a9255d28f351950e7fbca9d05e73942a4e27";

    @Override
    public String getDisplayName() {
        return "Prepare JEP 497 ML-DSA backport for M3JDK21";
    }

    @Override
    public String getDescription() {
        return "Replays eight exact Java product/security-test postimages plus three reduced pinned "
                + "FIPS-204 ACVP resources from the JEP 496 Java-21 receiver state, using "
                + "released ML-DSA implementation classes and bounded shared-owner adaptations.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3",
                "jdk21",
                "backport",
                "jep-497",
                "ml-dsa",
                "fips-204",
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
                new M3Jdk21HashPinnedSnapshotRecipe("jdk24-jep497-mldsa"),
                new M3Jdk21HashPinnedTextSnapshotRecipe("jdk24-jep497-mldsa-vectors"));
    }
}
