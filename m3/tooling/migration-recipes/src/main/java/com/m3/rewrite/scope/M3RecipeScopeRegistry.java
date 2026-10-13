// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.scope;

import java.util.Map;
import java.util.Objects;

/**
 * M3JDK21 target-only recipe-scope overlay.
 *
 * <p>Portable reusable recipe scope is canonical in
 * {@link com.synexia.rewrite.M3RecipeScopeRegistry}. This class owns only OpenJDK/JEP/receiver
 * mappings that are genuinely target-specific and converts portable canonical policies into the
 * retained target compatibility value type.</p>
 */
public final class M3RecipeScopeRegistry {
    private static final Map<String, M3RecipeScopePolicy> TARGET_POLICIES = Map.ofEntries(
            Map.entry(
                    "com.m3.rewrite.InstallIndexStringCompatibility",
                    fixed(M3EditScope.MODULE)),
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
                    "com.m3.rewrite.backport.M3Jep458BackportRecipe",
                    explicitChange()),
            Map.entry(
                    "com.m3.rewrite.backport.M3Jep485StreamGatherersBackportRecipe",
                    explicitChange()),
            Map.entry(
                    "com.m3.rewrite.backport.M3Jdk8338587ShakeXofBackportRecipe",
                    fixed(M3EditScope.MODULE)),
            Map.entry(
                    "com.m3.rewrite.backport.M3Jdk8357439JcmdCompletionBackportRecipe",
                    fixed(M3EditScope.MODULE)),
            Map.entry(
                    "com.m3.rewrite.backport.M3Jep496MlKemBackportRecipe",
                    explicitChange()),
            Map.entry(
                    "com.m3.rewrite.backport.M3Jep497MlDsaBackportRecipe",
                    explicitChange()),
            Map.entry(
                    "com.m3.rewrite.backport.M3Jep510KdfBackportRecipe",
                    explicitChange()),
            Map.entry(
                    "com.m3.rewrite.backport.M3Jep510KdfSecurityTestsRecipe",
                    fixed(M3EditScope.MODULE)),
            Map.entry(
                    "com.m3.rewrite.backport.M3Jep510KdfCandidateRecipe",
                    explicitChange()),
            Map.entry(
                    "com.m3.rewrite.backport.M3ReleaseJepAuthorityRepairRecipe",
                    fixed(M3EditScope.MULTI_MODULE)),
            Map.entry(
                    "com.m3.rewrite.synexia.M3SynexiaImportInventoryRecipe",
                    fixed(M3EditScope.MULTI_MODULE)),
            Map.entry(
                    "com.m3.rewrite.semantic.M3SemanticHashRecipe",
                    fixed(M3EditScope.FILE)));

    private M3RecipeScopeRegistry() {}

    public static M3RecipeScopePolicy require(String recipeClassName) {
        String checked = Objects.requireNonNull(recipeClassName, "recipeClassName");
        M3RecipeScopePolicy local = TARGET_POLICIES.get(checked);
        if (local != null) {
            return local;
        }
        if (com.synexia.rewrite.M3RecipeScopeRegistry.registered(checked)) {
            return M3RecipeScopePolicy.fromCanonical(
                    com.synexia.rewrite.M3RecipeScopeRegistry.require(checked));
        }
        throw new IllegalArgumentException("unregistered M3 recipe: " + checked);
    }

    public static M3RecipeScopePolicy require(Class<?> recipeType) {
        return require(Objects.requireNonNull(recipeType, "recipeType").getName());
    }

    public static boolean registered(String recipeClassName) {
        return recipeClassName != null
                && (TARGET_POLICIES.containsKey(recipeClassName)
                        || com.synexia.rewrite.M3RecipeScopeRegistry.registered(recipeClassName));
    }

    /** Portable Synexia registrations plus target-only OpenJDK receiver registrations. */
    public static int size() {
        return com.synexia.rewrite.M3RecipeScopeRegistry.size() + TARGET_POLICIES.size();
    }

    /** Target-only overlay cardinality; useful for receiver proof without copying canonical names. */
    static int targetOverlaySize() {
        return TARGET_POLICIES.size();
    }

    private static M3RecipeScopePolicy fixed(M3EditScope scope) {
        return new M3RecipeScopePolicy(
                scope,
                M3ContractMode.BEHAVIOR_AND_CONTRACT_PRESERVING,
                false);
    }

    private static M3RecipeScopePolicy explicitChange() {
        return new M3RecipeScopePolicy(
                M3EditScope.LIBRARY_API,
                M3ContractMode.EXPLICIT_CONTRACT_CHANGE,
                false);
    }

    private static M3RecipeScopePolicy inferred() {
        return new M3RecipeScopePolicy(
                M3EditScope.FILE,
                M3ContractMode.BEHAVIOR_AND_CONTRACT_PRESERVING,
                true);
    }
}
