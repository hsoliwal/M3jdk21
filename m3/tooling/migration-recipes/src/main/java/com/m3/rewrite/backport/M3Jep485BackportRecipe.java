// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import java.util.List;
import java.util.Set;
import org.openrewrite.Recipe;

/**
 * Candidate backport of final JEP 485, Stream Gatherers, onto M3JDK21.
 *
 * <p>The reviewed donor crate contains the final JDK 24 API/implementation and focused stream
 * tests. This wrapper has explicit LIBRARY_API contract-change authority through the M3 scope
 * registry; the generic hash-pinned engine remains reusable and does not gain blanket API-mutation
 * authority.</p>
 */
public final class M3Jep485BackportRecipe extends Recipe {
    public static final String CRATE = "jdk24-jep485-stream-gatherers";

    @Override
    public String getDisplayName() {
        return "Backport JEP 485 Stream Gatherers";
    }

    @Override
    public String getDescription() {
        return "Replays the reviewed final Stream Gatherers Java/test graph from exact JDK21 "
                + "preimages; compatibility and product promotion remain separate proof gates.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3",
                "jdk21",
                "backport",
                "jep-485",
                "stream-gatherers",
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
        return List.of(new M3Jdk21HashPinnedSnapshotRecipe(CRATE));
    }
}
