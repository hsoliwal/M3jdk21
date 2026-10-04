// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.scope;

import java.util.Map;
import java.util.Objects;

/**
 * External scope authority for retained OpenRewrite recipes.
 *
 * <p>FILE-local contract-preserving recipes remain independently executable. Authority escalates
 * only when the actual edit/contract boundary requires VISIBILITY, PACKAGE, MODULE, MULTI_MODULE
 * or LIBRARY_API. Public JDK/tool/API additions are explicit contract changes even when their
 * implementation happens to live in one module.
 */
public final class M3RecipeScopeRegistry {
    private static final Map<String, M3RecipeScopePolicy> POLICIES = Map.ofEntries(
            Map.entry(
                    "com.synexia.rewrite.M3MIndexJoinedCharsViewRecipe",
                    fixed(M3EditScope.MODULE)),
            Map.entry(
                    "com.synexia.rewrite.M3SegmentedLaneNativeRecipe",
                    fixed(M3EditScope.FILE)),
            Map.entry(
                    "com.m3.rewrite.InstallIndexStringCompatibility",
                    fixed(M3EditScope.MODULE)),
            Map.entry(
                    "com.synexia.rewrite.M3HashPinnedJavaSnapshotRecipe",
                    inferred()),
            Map.entry(
                    "com.m3.rewrite.atom.M3AtomizePureIntReturnRecipe",
                    fixed(M3EditScope.FILE)),
            Map.entry(
                    "com.m3.rewrite.atom.M3InventoryPureIntAtomCandidates",
                    fixed(M3EditScope.FILE)),
            Map.entry(
                    "com.m3.rewrite.atom.M3PatternizePureIntAtomRecipe",
                    fixed(M3EditScope.FILE)),
            Map.entry(
                    "com.m3.rewrite.atom.M3DocumentPureIntAtomRecipe",
                    fixed(M3EditScope.FILE)),
            Map.entry(
                    "com.m3.rewrite.atom.M3PureIntConvergenceRecipe",
                    fixed(M3EditScope.FILE)),
            Map.entry(
                    "com.m3.rewrite.M3Java21ConvergenceRecipe",
                    fixed(M3EditScope.FILE)),
            Map.entry(
                    "com.m3.rewrite.backport.M3Jdk21HashPinnedSnapshotRecipe",
                    inferred()),
            Map.entry(
                    "com.m3.rewrite.backport.M3Jdk21HashPinnedTextSnapshotRecipe",
                    inferred()),
            Map.entry(
                    "com.m3.rewrite.backport.M3VerbatimJavaPairRecipe",
                    fixed(M3EditScope.FILE)),
            Map.entry(
                    "com.m3.rewrite.index.M3SemanticIndexRecipe",
                    fixed(M3EditScope.FILE)),
            Map.entry(
                    "com.m3.rewrite.index.M3TypeRelationRecipe",
                    fixed(M3EditScope.MODULE)),
            Map.entry(
                    "com.m3.rewrite.index.M3WholeSemanticHashRecipe",
                    fixed(M3EditScope.MULTI_MODULE)),
            Map.entry(
                    "com.m3.rewrite.index.M3SemanticIndexM3DbBridgeRecipe",
                    fixed(M3EditScope.MULTI_MODULE)),
            Map.entry(
                    "com.m3.rewrite.pass.M3MultiPassPlannerRecipe",
                    fixed(M3EditScope.MULTI_MODULE)),
            Map.entry(
                    "com.m3.rewrite.pass.M3VerificationPlanRecipe",
                    fixed(M3EditScope.LIBRARY_API)),
            Map.entry(
                    "com.m3.rewrite.backport.M3ReleaseJepDenominatorRecipe",
                    fixed(M3EditScope.MODULE)),
            Map.entry(
                    "com.m3.rewrite.backport.M3Jdk8347112BackportRecipe",
                    explicitChange()),
            Map.entry(
                    "com.m3.rewrite.backport.M3Jdk8364182BackportRecipe",
                    explicitChange()),
            Map.entry(
                    "com.m3.rewrite.backport.M3Jdk8374808BackportRecipe",
                    explicitChange()),
            Map.entry(
                    "com.m3.rewrite.backport.M3Jdk8368692PasswordSystemInBackportRecipe",
                    explicitChange()),
            Map.entry(
                    "com.m3.rewrite.backport.M3Jdk8367584JfrOptionsHelpBackportRecipe",
                    explicitChange()));

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

    private static M3RecipeScopePolicy explicitChange() {
        return new M3RecipeScopePolicy(
                M3EditScope.LIBRARY_API,
                M3ContractMode.EXPLICIT_CONTRACT_CHANGE,
                false);
    }
}
