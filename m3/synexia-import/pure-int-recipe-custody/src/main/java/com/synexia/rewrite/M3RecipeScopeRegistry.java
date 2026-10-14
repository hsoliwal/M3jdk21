// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.util.Map;
import java.util.Objects;

/**
 * Canonical reusable scope registry for portable Synexia/OpenRewrite recipes.
 *
 * <p>Target repositories may maintain a small target-specific overlay for JEP/backport receiver
 * recipes. The reusable scope lattice/policy and portable recipe registrations remain canonical in
 * Synexia.</p>
 */
public final class M3RecipeScopeRegistry {
    private static final Map<String, M3RecipeScopePolicy> POLICIES = Map.ofEntries(
            Map.entry(
                    "org.openrewrite.java.RemoveUnusedImports",
                    M3RecipeScopePolicy.fixed(M3EditScope.FILE)),
            Map.entry(
                    "com.synexia.rewrite.M3MIndexJoinedCharsViewRecipe",
                    M3RecipeScopePolicy.fixed(M3EditScope.MODULE)),
            Map.entry(
                    "com.synexia.rewrite.M3SegmentedLaneNativeRecipe",
                    M3RecipeScopePolicy.fixed(M3EditScope.FILE)),
            Map.entry(
                    "com.synexia.rewrite.M3HashPinnedJavaSnapshotRecipe",
                    M3RecipeScopePolicy.inferred()),
            Map.entry(
                    "com.synexia.rewrite.atom.M3AtomizePureIntReturnRecipe",
                    M3RecipeScopePolicy.fixed(M3EditScope.FILE)),
            Map.entry(
                    "com.synexia.rewrite.atom.M3InventoryPureIntAtomCandidates",
                    M3RecipeScopePolicy.fixed(M3EditScope.FILE)),
            Map.entry(
                    "com.synexia.rewrite.atom.M3PatternizePureIntAtomRecipe",
                    M3RecipeScopePolicy.fixed(M3EditScope.FILE)),
            Map.entry(
                    "com.synexia.rewrite.atom.M3DocumentPureIntAtomRecipe",
                    M3RecipeScopePolicy.fixed(M3EditScope.FILE)),
            Map.entry(
                    "com.synexia.rewrite.atom.M3PureIntConvergenceRecipe",
                    M3RecipeScopePolicy.fixed(M3EditScope.FILE)),
            Map.entry(
                    "com.synexia.rewrite.M3PureIntAtomizeRecipe",
                    M3RecipeScopePolicy.fixed(M3EditScope.FILE)),
            Map.entry(
                    "com.synexia.rewrite.M3PureIntInventoryRecipe",
                    M3RecipeScopePolicy.fixed(M3EditScope.FILE)),
            Map.entry(
                    "com.synexia.rewrite.M3PureIntPatternizeRecipe",
                    M3RecipeScopePolicy.fixed(M3EditScope.FILE)),
            Map.entry(
                    "com.synexia.rewrite.M3PureIntDocumentationRecipe",
                    M3RecipeScopePolicy.fixed(M3EditScope.FILE)),
            Map.entry(
                    "com.synexia.rewrite.M3PureIntConvergenceRecipe",
                    M3RecipeScopePolicy.fixed(M3EditScope.FILE)),
            Map.entry(
                    "com.synexia.rewrite.semantic.M3SemanticHashRecipe",
                    M3RecipeScopePolicy.fixed(M3EditScope.FILE)),
            Map.entry(
                    "com.synexia.rewrite.M3Jdk21FileConvergenceRecipe",
                    M3RecipeScopePolicy.fixed(M3EditScope.FILE)));

    private M3RecipeScopeRegistry() {}

    public static M3RecipeScopePolicy require(String recipeClassName) {
        String checked = Objects.requireNonNull(recipeClassName, "recipeClassName");
        M3RecipeScopePolicy policy = POLICIES.get(checked);
        if (policy == null) {
            throw new IllegalArgumentException(
                    "unregistered canonical Synexia recipe scope: " + checked);
        }
        return policy;
    }

    public static M3RecipeScopePolicy require(Class<?> recipeType) {
        return require(Objects.requireNonNull(recipeType, "recipeType").getName());
    }

    public static boolean registered(String recipeClassName) {
        return recipeClassName != null && POLICIES.containsKey(recipeClassName);
    }

    public static int size() {
        return POLICIES.size();
    }

    /**
     * A target-specific overlay may reuse canonical policy values while owning only its target
     * recipe-name mapping.
     */
    public static M3RecipeScopePolicy targetOverlay(
            M3EditScope minimumScope,
            M3ContractMode contractMode,
            boolean inferFromTargets) {
        return new M3RecipeScopePolicy(minimumScope, contractMode, inferFromTargets);
    }
}
