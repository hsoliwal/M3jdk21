// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import java.util.List;
import java.util.Set;
import org.openrewrite.Recipe;

/**
 * Recipe-first candidate for JEP 510, Key Derivation Function API.
 *
 * <p>The source packet is the cumulative final JEP 510 lineage plus the HKDF-specific
 * post-final correctness/security fixes pinned by the repository J510 intake. Product application
 * remains a separate security-gated step.</p>
 */
public final class M3Jep510KdfBackportRecipe extends Recipe {
    public static final String PREVIEW_IMPLEMENTATION =
            "2a1ae0ff89a8ac364206b09059d9dc884adcc5ac";
    public static final String DELAYED_PROVIDER_FIX =
            "2c7bea1cb2acd768e57f460440228fee914255a6";
    public static final String NON_EXTRACTABLE_PRK_FIX =
            "db7fa6a2c65d11e5bd790073d345f37b5ec356b6";
    public static final String FINAL_JEP =
            "079fccfa9a03b890e698c52c689dea0f19f8fbee";
    public static final String SECRET_CLEANUP_FIX =
            "012b4eb6cea6e1756a589a6c17a805867ed60686";
    public static final String DOC_FIX =
            "79456110fb6dd11ef19e9637c6f40ee7ce329481";

    @Override
    public String getDisplayName() {
        return "Prepare JEP 510 KDF backport for M3JDK21";
    }

    @Override
    public String getDescription() {
        return "Replays the exact final KDF/HKDF API and Java-21 receiver integrations from "
                + "current-master preimages; security acceptance remains separately gated.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3",
                "jdk21",
                "backport",
                "jep-510",
                "kdf",
                "hkdf",
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
        return List.of(new M3Jdk21HashPinnedSnapshotRecipe("jdk25-jep510-kdf"));
    }
}
