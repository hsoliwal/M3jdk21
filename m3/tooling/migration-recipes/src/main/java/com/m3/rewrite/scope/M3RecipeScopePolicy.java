// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.scope;

import java.util.Collection;
import java.util.Objects;

/** Immutable external scope policy for an existing recipe class. */
public record M3RecipeScopePolicy(
        M3EditScope minimumScope,
        M3ContractMode contractMode,
        boolean inferFromTargets) {

    public M3RecipeScopePolicy {
        Objects.requireNonNull(minimumScope, "minimumScope");
        Objects.requireNonNull(contractMode, "contractMode");
    }

    /** Resolve the exact authority required for one invocation without changing the recipe type. */
    public M3EditScope resolve(Collection<String> moduleRelativeJavaTargets) {
        M3EditScope resolved = inferFromTargets
                ? M3ScopeInference.forJavaPaths(moduleRelativeJavaTargets)
                : minimumScope;
        return minimumScope.promote(resolved);
    }

    /** True only when a specific invocation is safely file-local and behavior preserving. */
    public boolean fileLocalMechanical(Collection<String> moduleRelativeJavaTargets) {
        return contractMode == M3ContractMode.BEHAVIOR_AND_CONTRACT_PRESERVING
                && resolve(moduleRelativeJavaTargets) == M3EditScope.FILE;
    }
}
