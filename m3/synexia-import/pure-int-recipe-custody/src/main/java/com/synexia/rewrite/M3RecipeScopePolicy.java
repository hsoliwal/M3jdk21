// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Immutable reusable scope policy for one recipe invocation.
 *
 * <p>The policy is canonical in Synexia. Public targets may layer target-specific recipe names on
 * top of this value contract, but they must not fork the scope semantics.</p>
 */
public record M3RecipeScopePolicy(
        M3EditScope minimumScope,
        M3ContractMode contractMode,
        boolean inferFromTargets) {

    public M3RecipeScopePolicy {
        minimumScope = Objects.requireNonNull(minimumScope, "minimumScope");
        contractMode = Objects.requireNonNull(contractMode, "contractMode");
    }

    /** Resolve exact authority for one invocation without changing the recipe type. */
    public M3EditScope resolve(Collection<String> javaTargets) {
        Collection<String> checked = Objects.requireNonNull(javaTargets, "javaTargets");
        M3EditScope resolved = inferFromTargets
                ? M3ScopeInference.inferJavaTargets(List.copyOf(checked))
                : minimumScope;
        return minimumScope.promote(resolved);
    }

    /** True only when the invocation is behavior-preserving and mechanically file-local. */
    public boolean fileLocalMechanical(Collection<String> javaTargets) {
        return contractMode == M3ContractMode.BEHAVIOR_AND_CONTRACT_PRESERVING
                && resolve(javaTargets) == M3EditScope.FILE;
    }

    public static M3RecipeScopePolicy fixed(M3EditScope scope) {
        return new M3RecipeScopePolicy(
                scope,
                M3ContractMode.BEHAVIOR_AND_CONTRACT_PRESERVING,
                false);
    }

    public static M3RecipeScopePolicy inferred() {
        return new M3RecipeScopePolicy(
                M3EditScope.FILE,
                M3ContractMode.BEHAVIOR_AND_CONTRACT_PRESERVING,
                true);
    }

    public static M3RecipeScopePolicy explicitContractChange() {
        return new M3RecipeScopePolicy(
                M3EditScope.LIBRARY_API,
                M3ContractMode.EXPLICIT_CONTRACT_CHANGE,
                false);
    }
}
