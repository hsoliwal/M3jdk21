// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.pipeline;

import java.util.Objects;
import org.openrewrite.Recipe;

/** Immutable registration for one admitted OpenRewrite recipe in the convergence DAG. */
public record M3RecipeRegistration(
        String id,
        M3RecipeStage stage,
        int order,
        Class<? extends Recipe> recipeType) {

    public M3RecipeRegistration {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(stage, "stage");
        Objects.requireNonNull(recipeType, "recipeType");
        if (!id.matches("[a-z0-9][a-z0-9.-]{2,120}")) {
            throw new IllegalArgumentException("invalid M3 recipe id: " + id);
        }
        if (order < stage.order()) {
            throw new IllegalArgumentException(
                    "recipe order " + order + " precedes stage " + stage);
        }
    }
}
