/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 * Modified 2026: compose the existing stored-representative and batch-admission snapshots.
 */
package com.synexia.rewrite;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.openrewrite.Option;
import org.openrewrite.Recipe;

/** Composes existing wrapper atoms through the unchanged canonical snapshot provider. */
public final class M3WrapperCompositionRecipe extends Recipe {
    @Option(displayName = "Predecessor",
            description = "Exact admitted source state: representative or batch.",
            example = "representative")
    private final String predecessor;

    /** Uses the stored-representative PR as the primary receiving predecessor. */
    public M3WrapperCompositionRecipe() {
        this("representative");
    }

    /** Selects an explicit source seal; this never changes an older recipe's admission rules. */
    @JsonCreator
    public M3WrapperCompositionRecipe(@JsonProperty("predecessor") String predecessor) {
        if (!"representative".equals(predecessor) && !"batch".equals(predecessor)) {
            throw new IllegalArgumentException("unsupported wrapper predecessor");
        }
        this.predecessor = predecessor;
    }

    public String getPredecessor() {
        return predecessor;
    }

    @Override
    public String getDisplayName() {
        return "Compose the qualified filtered and sorted wrapper fixes";
    }

    @Override
    public String getDescription() {
        return "Preserves stored-representative membership and deletion while adding "
                + "the existing staged batch admission atom through exact Java source seals.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(new M3HashPinnedJavaSnapshotRecipe(
                "wrapper-composition-" + predecessor + "-20261017"));
    }
}
