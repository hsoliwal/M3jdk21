// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.scope;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.m3.rewrite.InstallIndexStringCompatibility;
import com.m3.rewrite.M3Java21ConvergenceRecipe;
import com.m3.rewrite.atom.M3AtomizePureIntReturnRecipe;
import com.m3.rewrite.atom.M3DocumentPureIntAtomRecipe;
import com.m3.rewrite.atom.M3InventoryPureIntAtomCandidates;
import com.m3.rewrite.atom.M3PatternizePureIntAtomRecipe;
import com.m3.rewrite.atom.M3PureIntConvergenceRecipe;
import com.m3.rewrite.backport.M3Jdk21HashPinnedSnapshotRecipe;
import com.m3.rewrite.backport.M3Jdk21HashPinnedTextSnapshotRecipe;
import com.m3.rewrite.backport.M3Jdk8347112BackportRecipe;
import com.m3.rewrite.backport.M3Jdk8364182BackportRecipe;
import com.m3.rewrite.backport.M3Jdk8367584JfrOptionsHelpBackportRecipe;
import com.m3.rewrite.backport.M3Jdk8368692PasswordSystemInBackportRecipe;
import com.m3.rewrite.backport.M3Jdk8374808BackportRecipe;
import com.m3.rewrite.backport.M3ReleaseJepDenominatorRecipe;
import com.m3.rewrite.backport.M3VerbatimJavaPairRecipe;
import com.m3.rewrite.index.M3SemanticIndexM3DbBridgeRecipe;
import com.m3.rewrite.index.M3SemanticIndexRecipe;
import com.m3.rewrite.index.M3TypeRelationRecipe;
import com.m3.rewrite.index.M3WholeSemanticHashRecipe;
import com.m3.rewrite.pass.M3MultiPassPlannerRecipe;
import com.m3.rewrite.pass.M3VerificationPlanRecipe;
import com.synexia.rewrite.M3HashPinnedJavaSnapshotRecipe;
import com.synexia.rewrite.M3MIndexJoinedCharsViewRecipe;
import com.synexia.rewrite.M3SegmentedLaneNativeRecipe;
import java.util.List;
import org.junit.jupiter.api.Test;

final class M3RecipeScopeDeclarationTest {
    @Test
    void everyCurrentRecipeHasExternalScopeAuthority() {
        assertEquals(25, M3RecipeScopeRegistry.size());
        for (Class<?> recipe : allRecipes()) {
            assertTrue(
                    M3RecipeScopeRegistry.registered(recipe.getName()),
                    recipe.getName());
        }
        assertFalse(M3RecipeScopeRegistry.registered(null));
        assertFalse(M3RecipeScopeRegistry.registered("missing.Recipe"));
        assertThrows(
                NullPointerException.class,
                () -> M3RecipeScopeRegistry.require((Class<?>) null));
        assertThrows(
                NullPointerException.class,
                () -> M3RecipeScopeRegistry.require((String) null));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3RecipeScopeRegistry.require("missing.Recipe"));
    }

    @Test
    void fixedAndTargetInferredScopesFollowTheM3PromotionOrder() {
        var fileAtom = M3RecipeScopeRegistry.require(M3AtomizePureIntReturnRecipe.class);
        assertEquals(
                M3EditScope.FILE,
                fileAtom.resolve(List.of("src/main/java/a/A.java")));
        assertTrue(fileAtom.fileLocalMechanical(List.of("src/main/java/a/A.java")));

        var semantic = M3RecipeScopeRegistry.require(M3SemanticIndexRecipe.class);
        assertEquals(
                M3EditScope.FILE,
                semantic.resolve(List.of("src/main/java/a/A.java")));
        assertTrue(semantic.fileLocalMechanical(List.of("src/main/java/a/A.java")));

        var module = M3RecipeScopeRegistry.require(M3TypeRelationRecipe.class);
        assertEquals(
                M3EditScope.MODULE,
                module.resolve(List.of("src/main/java/a/A.java")));
        assertFalse(module.fileLocalMechanical(List.of("src/main/java/a/A.java")));

        for (Class<?> recipe : List.of(
                M3WholeSemanticHashRecipe.class,
                M3SemanticIndexM3DbBridgeRecipe.class,
                M3MultiPassPlannerRecipe.class)) {
            assertEquals(
                    M3EditScope.MULTI_MODULE,
                    M3RecipeScopeRegistry.require(recipe)
                            .resolve(List.of("src/main/java/a/A.java")));
        }

        assertEquals(
                M3EditScope.LIBRARY_API,
                M3RecipeScopeRegistry.require(M3VerificationPlanRecipe.class)
                        .resolve(List.of("src/main/java/a/A.java")));
        assertEquals(
                M3EditScope.MODULE,
                M3RecipeScopeRegistry.require(M3ReleaseJepDenominatorRecipe.class)
                        .resolve(List.of("m3/backports/JEP_CATALOGUE.tsv")));

        var jdkJavaPolicy =
                M3RecipeScopeRegistry.require(M3Jdk21HashPinnedSnapshotRecipe.class);
        assertEquals(
                M3EditScope.FILE,
                jdkJavaPolicy.resolve(List.of(
                        "src/jdk.javadoc/share/classes/jdk/javadoc/internal/Foo.java")));
        assertEquals(
                M3EditScope.PACKAGE,
                jdkJavaPolicy.resolve(List.of(
                        "src/jdk.javadoc/share/classes/jdk/javadoc/internal/A.java",
                        "src/jdk.javadoc/share/classes/jdk/javadoc/internal/B.java")));
        assertEquals(
                M3EditScope.MULTI_MODULE,
                jdkJavaPolicy.resolve(List.of(
                        "src/java.base/share/classes/java/lang/A.java",
                        "src/jdk.javadoc/share/classes/jdk/javadoc/B.java")));

        var inferred =
                M3RecipeScopeRegistry.require(M3HashPinnedJavaSnapshotRecipe.class);
        assertEquals(
                M3EditScope.FILE,
                inferred.resolve(List.of("src/main/java/a/A.java")));
        assertEquals(
                M3EditScope.PACKAGE,
                inferred.resolve(List.of(
                        "src/main/java/a/A.java",
                        "src/main/java/a/B.java")));
        assertEquals(
                M3EditScope.MODULE,
                inferred.resolve(List.of(
                        "src/main/java/a/A.java",
                        "src/main/java/b/B.java")));
    }

    @Test
    void externallyVisibleBackportsRequireExplicitLibraryApiAuthority() {
        for (Class<?> recipe : explicitBackports()) {
            var policy = M3RecipeScopeRegistry.require(recipe);
            assertEquals(M3EditScope.LIBRARY_API, policy.minimumScope());
            assertEquals(
                    M3ContractMode.EXPLICIT_CONTRACT_CHANGE,
                    policy.contractMode());
            assertFalse(
                    policy.fileLocalMechanical(
                            List.of("src/main/java/a/A.java")));
        }
    }

    @Test
    void retainedMechanicalRecipesStayBehaviorAndContractPreserving() {
        for (Class<?> recipe : allRecipes()) {
            if (explicitBackports().contains(recipe)) {
                continue;
            }
            assertEquals(
                    M3ContractMode.BEHAVIOR_AND_CONTRACT_PRESERVING,
                    M3RecipeScopeRegistry.require(recipe).contractMode(),
                    recipe.getName());
        }
    }

    private static List<Class<?>> explicitBackports() {
        return List.of(
                M3Jdk8347112BackportRecipe.class,
                M3Jdk8364182BackportRecipe.class,
                M3Jdk8374808BackportRecipe.class,
                M3Jdk8368692PasswordSystemInBackportRecipe.class,
                M3Jdk8367584JfrOptionsHelpBackportRecipe.class);
    }

    private static List<Class<?>> allRecipes() {
        return List.of(
                M3MIndexJoinedCharsViewRecipe.class,
                M3SegmentedLaneNativeRecipe.class,
                InstallIndexStringCompatibility.class,
                M3HashPinnedJavaSnapshotRecipe.class,
                M3AtomizePureIntReturnRecipe.class,
                M3InventoryPureIntAtomCandidates.class,
                M3PatternizePureIntAtomRecipe.class,
                M3DocumentPureIntAtomRecipe.class,
                M3PureIntConvergenceRecipe.class,
                M3Java21ConvergenceRecipe.class,
                M3Jdk21HashPinnedSnapshotRecipe.class,
                M3Jdk21HashPinnedTextSnapshotRecipe.class,
                M3VerbatimJavaPairRecipe.class,
                M3SemanticIndexRecipe.class,
                M3TypeRelationRecipe.class,
                M3WholeSemanticHashRecipe.class,
                M3SemanticIndexM3DbBridgeRecipe.class,
                M3MultiPassPlannerRecipe.class,
                M3VerificationPlanRecipe.class,
                M3ReleaseJepDenominatorRecipe.class,
                M3Jdk8347112BackportRecipe.class,
                M3Jdk8364182BackportRecipe.class,
                M3Jdk8374808BackportRecipe.class,
                M3Jdk8368692PasswordSystemInBackportRecipe.class,
                M3Jdk8367584JfrOptionsHelpBackportRecipe.class);
    }
}
