// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.scope;

import java.util.Collection;
import java.util.Objects;

/**
 * M3JDK21 target adapter for Synexia's canonical reusable recipe-scope policy.
 *
 * <p>The target retains this value only to preserve existing package/API references. Portable
 * recipe semantics are converted from {@link com.synexia.rewrite.M3RecipeScopePolicy}; only
 * target-specific OpenJDK path inference remains local.</p>
 */
public record M3RecipeScopePolicy(
        M3EditScope minimumScope,
        M3ContractMode contractMode,
        boolean inferFromTargets) {

    public M3RecipeScopePolicy {
        Objects.requireNonNull(minimumScope, "minimumScope");
        Objects.requireNonNull(contractMode, "contractMode");
    }

    static M3RecipeScopePolicy fromCanonical(
            com.synexia.rewrite.M3RecipeScopePolicy policy) {
        com.synexia.rewrite.M3RecipeScopePolicy checked =
                Objects.requireNonNull(policy, "policy");
        return new M3RecipeScopePolicy(
                M3EditScope.fromCanonical(checked.minimumScope()),
                M3ContractMode.fromCanonical(checked.contractMode()),
                checked.inferFromTargets());
    }

    /** Resolve the exact target authority without redefining portable Synexia semantics. */
    public M3EditScope resolve(Collection<String> targetPaths) {
        Objects.requireNonNull(targetPaths, "targetPaths");
        M3EditScope resolved = inferFromTargets
                ? M3ScopeInference.forPaths(targetPaths)
                : minimumScope;
        return minimumScope.promote(resolved);
    }

    /** True only when a specific invocation is safely file-local and behavior preserving. */
    public boolean fileLocalMechanical(Collection<String> targetPaths) {
        return contractMode == M3ContractMode.BEHAVIOR_AND_CONTRACT_PRESERVING
                && resolve(targetPaths) == M3EditScope.FILE;
    }
}
