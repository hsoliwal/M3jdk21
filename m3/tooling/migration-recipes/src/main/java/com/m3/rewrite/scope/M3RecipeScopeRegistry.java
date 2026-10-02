// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.scope;

import java.util.Map;
import java.util.Objects;

/**
 * External scope authority for retained OpenRewrite recipes.
 *
 * <p>The registry deliberately does not modify recipe classes. Existing recipe public contracts
 * remain sealed while orchestration can still reject an invocation whose requested write boundary
 * is broader than its evidence.
 */
public final class M3RecipeScopeRegistry {
    private static final Map<String, M3RecipeScopePolicy> POLICIES = Map.of(
            "com.synexia.rewrite.M3MIndexJoinedCharsViewRecipe",
            fixed(M3EditScope.MODULE),
            "com.synexia.rewrite.M3SegmentedLaneNativeRecipe",
            fixed(M3EditScope.FILE),
            "com.m3.rewrite.InstallIndexStringCompatibility",
            fixed(M3EditScope.MODULE),
            "com.synexia.rewrite.M3HashPinnedJavaSnapshotRecipe",
            inferred(),
            "com.m3.rewrite.atom.M3AtomizePureIntReturnRecipe",
            fixed(M3EditScope.FILE),
            "com.m3.rewrite.atom.M3InventoryPureIntAtomCandidates",
            fixed(M3EditScope.FILE),
            "com.m3.rewrite.atom.M3PatternizePureIntAtomRecipe",
            fixed(M3EditScope.FILE),
            "com.m3.rewrite.atom.M3DocumentPureIntAtomRecipe",
            fixed(M3EditScope.FILE),
            "com.m3.rewrite.atom.M3PureIntConvergenceRecipe",
            fixed(M3EditScope.FILE));

    private M3RecipeScopeRegistry() {}

    public static M3RecipeScopePolicy require(String recipeClassName) {
        Objects.requireNonNull(recipeClassName, "recipeClassName");
        M3RecipeScopePolicy policy = POLICIES.get(recipeClassName);
        if (policy == null) {
            throw new IllegalArgumentException("unregistered M3 recipe: " + recipeClassName);
        }
        return policy;
    }

    public static M3RecipeScopePolicy require(Class<?> recipeType) {
        Objects.requireNonNull(recipeType, "recipeType");
        return require(recipeType.getName());
    }

    public static boolean registered(String recipeClassName) {
        return recipeClassName != null && POLICIES.containsKey(recipeClassName);
    }

    public static int size() {
        return POLICIES.size();
    }

    private static M3RecipeScopePolicy fixed(M3EditScope scope) {
        return new M3RecipeScopePolicy(
                scope,
                M3ContractMode.BEHAVIOR_AND_CONTRACT_PRESERVING,
                false);
    }

    private static M3RecipeScopePolicy inferred() {
        return new M3RecipeScopePolicy(
                M3EditScope.FILE,
                M3ContractMode.BEHAVIOR_AND_CONTRACT_PRESERVING,
                true);
    }
}
