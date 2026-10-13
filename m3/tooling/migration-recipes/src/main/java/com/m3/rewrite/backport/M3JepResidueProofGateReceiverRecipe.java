// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import java.util.List;
import java.util.Set;
import org.openrewrite.Recipe;

/**
 * Thin M3JDK21 receiver for the canonical Synexia released-JEP residue proof repair.
 *
 * <p>Source authority is the SHA-256-pinned Synexia recipe crate at
 * {@code hsoliwal/com.synexia#10724}. This receiver reuses the existing JDK21 text replay
 * engine; it does not duplicate the portable implementation or grant JEP compatibility
 * or promotion authority.</p>
 */
public final class M3JepResidueProofGateReceiverRecipe extends Recipe {
    @Override
    public String getDisplayName() {
        return "Receive canonical M3 released-JEP residue proof repair";
    }

    @Override
    public String getDescription() {
        return "Replays three Python/TSV backport-gate atoms from the reviewed exact preimages, "
                + "rejecting drift and preserving the Java 21 backport acceptance boundary.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of("m3", "jdk21", "synexia", "backport", "proof", "candidate-only");
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(new M3Jdk21HashPinnedTextSnapshotRecipe("jdk21-jep-residue-proof"));
    }
}
