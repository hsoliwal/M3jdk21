// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import java.util.List;
import java.util.Set;
import org.openrewrite.Recipe;

/**
 * Java-21 compatibility adaptation of JDK-8338587 for post-quantum security backports.
 *
 * <p>The upstream change assumes the later long[]/VarHandle SHA3 representation and removes the
 * standalone SHAKE classes. M3JDK21 preserves its Java-21 byte-state SHA3 engine and standalone
 * SHAKE256 compatibility while adding only the nested streaming XOF and public lane permutation
 * surfaces required by ML-KEM/ML-DSA.</p>
 */
public final class M3Jdk8338587ShakeXofBackportRecipe extends Recipe {
    public static final String ISSUE = "JDK-8338587";
    public static final String UPSTREAM_COMMIT =
            "c54fc08aa3c63e4b26dc5edb2436844dfd3bab7c";

    @Override
    public String getDisplayName() {
        return "Adapt JDK-8338587 SHAKE XOF to Java 21";
    }

    @Override
    public String getDescription() {
        return "Replays a two-file current-tree-pinned Java 21 XOF candidate: adapted SHA3 plus "
                + "known-answer/legacy-parity jtreg, without deleting standalone SHAKE256.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3", "jdk21", "backport", "jdk-8338587", "shake", "xof",
                "security", "pqc", "hash-pinned", "candidate-only");
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new M3Jdk21HashPinnedSnapshotRecipe(
                        "jdk24-jdk8338587-shake-xof"));
    }
}
