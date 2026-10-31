// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import java.util.List;
import java.util.Set;
import org.openrewrite.Recipe;

/**
 * Source-sealed receiver for the JEP 404 ephemeral candidate materialization/build proof.
 *
 * <p>This recipe changes only proof/control-plane files. It never writes JDK product source in the
 * canonical checkout and grants no compatibility, scope-promotion, or promotion authority.</p>
 */
public final class M3Jep404EphemeralProofReceiverRecipe extends Recipe {
    @Override
    public String getDisplayName() {
        return "Receive JEP 404 ephemeral candidate proof";
    }

    @Override
    public String getDescription() {
        return "Replays the reviewed JEP 404 proof workflow, policy, README, and policy test from "
                + "hash-pinned text preimages; candidate source materialization occurs only inside "
                + "an ephemeral CI worktree.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3",
                "jdk21",
                "jep404",
                "shenandoah",
                "backport",
                "proof",
                "candidate-only",
                "fail-closed");
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new M3Jdk21HashPinnedTextSnapshotRecipe(
                        "jdk21-jep404-ephemeral-proof"));
    }
}
