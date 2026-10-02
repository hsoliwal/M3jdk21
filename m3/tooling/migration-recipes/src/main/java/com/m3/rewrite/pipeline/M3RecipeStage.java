// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.pipeline;

/** Ordered semantic stages in the Java 21 M3 OpenRewrite convergence pipeline. */
public enum M3RecipeStage {
    INVENTORY(0, false),
    ATOMIZATION(10, true),
    PATTERNIZATION_IOP(20, true),
    DOCUMENTATION(30, true);

    private final int order;
    private final boolean mutating;

    M3RecipeStage(int order, boolean mutating) {
        this.order = order;
        this.mutating = mutating;
    }

    public int order() {
        return order;
    }

    public boolean mutating() {
        return mutating;
    }
}
