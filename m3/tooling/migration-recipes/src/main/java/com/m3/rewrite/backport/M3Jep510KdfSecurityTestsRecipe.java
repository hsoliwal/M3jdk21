// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import java.util.List;
import java.util.Set;
import org.openrewrite.Recipe;

/**
 * Hash-pinned JEP 510 security/jtreg test tranche.
 *
 * <p>The tests are exact GPLv2 OpenJDK test postimages from the pinned final JEP 510 lineage.
 * This recipe grants no product or promotion authority.</p>
 */
public final class M3Jep510KdfSecurityTestsRecipe extends Recipe {
    public static final String UPSTREAM_TEST_PIN =
            "79456110fb6dd11ef19e9637c6f40ee7ce329481";

    @Override
    public String getDisplayName() {
        return "Prepare JEP 510 KDF security tests for M3JDK21";
    }

    @Override
    public String getDescription() {
        return "Adds the exact pinned HKDF known-answer, basic, non-extractable PRK, and delayed "
                + "provider test postimages required by the JEP 510 candidate.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3",
                "jdk21",
                "backport",
                "jep-510",
                "kdf",
                "security",
                "jtreg",
                "hash-pinned",
                "candidate-only");
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(new M3Jdk21HashPinnedSnapshotRecipe("jdk25-jep510-kdf-tests"));
    }
}
