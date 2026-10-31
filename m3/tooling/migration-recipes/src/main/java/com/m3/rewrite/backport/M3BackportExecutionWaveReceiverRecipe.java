// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import java.util.List;
import java.util.Set;
import org.openrewrite.Recipe;

/**
 * Source-sealed receiver for the M3JDK21 backport execution-wave control plane.
 *
 * <p>The received Python/Markdown/POM files are planning/proof substrate only. They schedule
 * already-admitted backport recipe atoms; they do not edit JDK product source, approve scope
 * promotion, or promote a backport.</p>
 */
public final class M3BackportExecutionWaveReceiverRecipe extends Recipe {
    @Override
    public String getDisplayName() {
        return "Receive M3JDK21 backport execution-wave planner";
    }

    @Override
    public String getDescription() {
        return "Replays the exact reviewed execution-wave planner, tests, documentation and Maven "
                + "wiring from hash-pinned UTF-8 preimages/postimages; these files do not edit JDK "
                + "product source or grant compatibility/promotion authority.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3",
                "jdk21",
                "backport",
                "openrewrite",
                "recipe-dag",
                "scheduler",
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
                        "jdk21-backport-execution-waves"));
    }
}
