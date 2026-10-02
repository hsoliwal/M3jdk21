// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.scope;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.m3.rewrite.InstallIndexStringCompatibility;
import com.m3.rewrite.atom.M3AtomizePureIntReturnRecipe;
import com.m3.rewrite.atom.M3InventoryPureIntAtomCandidates;
import com.m3.rewrite.donor.M3PinnedDonorInlineRecipe;
import com.m3.rewrite.index.M3SemanticIndexM3DbBridgeRecipe;
import com.m3.rewrite.index.M3SemanticIndexRecipe;
import com.m3.rewrite.index.M3TypeRelationRecipe;
import com.synexia.rewrite.M3HashPinnedJavaSnapshotRecipe;
import com.synexia.rewrite.M3MIndexJoinedCharsViewRecipe;
import com.synexia.rewrite.M3SegmentedLaneNativeRecipe;
import java.util.List;
import org.junit.jupiter.api.Test;

final class M3RecipeScopeDeclarationTest {
    @Test
    void everyCurrentRecipeHasExternalScopeAuthority() {
        assertEquals(10, M3RecipeScopeRegistry.size());
        assertTrue(M3RecipeScopeRegistry.registered(M3MIndexJoinedCharsViewRecipe.class.getName()));
        assertTrue(M3RecipeScopeRegistry.registered(M3SegmentedLaneNativeRecipe.class.getName()));
        assertTrue(M3RecipeScopeRegistry.registered(InstallIndexStringCompatibility.class.getName()));
        assertTrue(M3RecipeScopeRegistry.registered(M3HashPinnedJavaSnapshotRecipe.class.getName()));
        assertTrue(M3RecipeScopeRegistry.registered(M3AtomizePureIntReturnRecipe.class.getName()));
        assertTrue(M3RecipeScopeRegistry.registered(M3InventoryPureIntAtomCandidates.class.getName()));
        assertTrue(M3RecipeScopeRegistry.registered(M3PinnedDonorInlineRecipe.class.getName()));
        assertTrue(M3RecipeScopeRegistry.registered(M3SemanticIndexRecipe.class.getName()));
        assertTrue(M3RecipeScopeRegistry.registered(M3TypeRelationRecipe.class.getName()));
        assertTrue(M3RecipeScopeRegistry.registered(M3SemanticIndexM3DbBridgeRecipe.class.getName()));
        assertFalse(M3RecipeScopeRegistry.registered(null));
        assertFalse(M3RecipeScopeRegistry.registered("missing.Recipe"));
        assertThrows(NullPointerException.class, () -> M3RecipeScopeRegistry.require((Class<?>) null));
        assertThrows(NullPointerException.class, () -> M3RecipeScopeRegistry.require((String) null));
        assertThrows(IllegalArgumentException.class, () -> M3RecipeScopeRegistry.require("missing.Recipe"));
    }

    @Test
    void fixedAndTargetInferredScopesFollowTheM3PromotionOrder() {
        var filePolicy = M3RecipeScopeRegistry.require(M3MIndexJoinedCharsViewRecipe.class);
        assertEquals(M3EditScope.FILE, filePolicy.resolve(List.of("src/main/java/a/A.java")));
        assertTrue(filePolicy.fileLocalMechanical(List.of("src/main/java/a/A.java")));

        var nativePolicy = M3RecipeScopeRegistry.require(M3SegmentedLaneNativeRecipe.class);
        assertEquals(M3EditScope.FILE, nativePolicy.resolve(List.of("src/main/java/a/A.java")));

        var modulePolicy = M3RecipeScopeRegistry.require(InstallIndexStringCompatibility.class);
        assertEquals(M3EditScope.MODULE, modulePolicy.resolve(List.of("src/main/java/a/A.java")));
        assertFalse(modulePolicy.fileLocalMechanical(List.of("src/main/java/a/A.java")));

        var atomPolicy = M3RecipeScopeRegistry.require(M3AtomizePureIntReturnRecipe.class);
        assertEquals(M3EditScope.FILE, atomPolicy.resolve(List.of("src/main/java/a/A.java")));
        assertTrue(atomPolicy.fileLocalMechanical(List.of("src/main/java/a/A.java")));

        var inventoryPolicy = M3RecipeScopeRegistry.require(M3InventoryPureIntAtomCandidates.class);
        assertEquals(M3EditScope.FILE, inventoryPolicy.resolve(List.of("src/main/java/a/A.java")));
        assertTrue(inventoryPolicy.fileLocalMechanical(List.of("src/main/java/a/A.java")));

        var donorPolicy = M3RecipeScopeRegistry.require(M3PinnedDonorInlineRecipe.class);
        assertEquals(M3EditScope.MODULE, donorPolicy.resolve(List.of("src/main/java/a/A.java")));
        assertFalse(donorPolicy.fileLocalMechanical(List.of("src/main/java/a/A.java")));

        var semanticPolicy = M3RecipeScopeRegistry.require(M3SemanticIndexRecipe.class);
        assertEquals(M3EditScope.FILE, semanticPolicy.resolve(List.of("src/main/java/a/A.java")));
        assertTrue(semanticPolicy.fileLocalMechanical(List.of("src/main/java/a/A.java")));

        var typeRelationPolicy = M3RecipeScopeRegistry.require(M3TypeRelationRecipe.class);
        assertEquals(M3EditScope.MODULE, typeRelationPolicy.resolve(List.of("src/main/java/a/A.java")));
        assertFalse(typeRelationPolicy.fileLocalMechanical(List.of("src/main/java/a/A.java")));

        var semanticDbPolicy = M3RecipeScopeRegistry.require(M3SemanticIndexM3DbBridgeRecipe.class);
        assertEquals(M3EditScope.MULTI_MODULE, semanticDbPolicy.resolve(List.of("src/main/java/a/A.java")));
        assertFalse(semanticDbPolicy.fileLocalMechanical(List.of("src/main/java/a/A.java")));

        var inferredPolicy = M3RecipeScopeRegistry.require(M3HashPinnedJavaSnapshotRecipe.class);
        assertEquals(
                M3EditScope.FILE,
                inferredPolicy.resolve(List.of("src/main/java/a/A.java")));
        assertEquals(
                M3EditScope.PACKAGE,
                inferredPolicy.resolve(List.of(
                        "src/main/java/a/A.java",
                        "src/main/java/a/B.java")));
        assertEquals(
                M3EditScope.MODULE,
                inferredPolicy.resolve(List.of(
                        "src/main/java/a/A.java",
                        "src/main/java/b/B.java")));
    }

    @Test
    void currentRegistryIsBehaviorAndContractPreserving() {
        for (Class<?> recipe : List.of(
                M3MIndexJoinedCharsViewRecipe.class,
                M3SegmentedLaneNativeRecipe.class,
                InstallIndexStringCompatibility.class,
                M3HashPinnedJavaSnapshotRecipe.class,
                M3AtomizePureIntReturnRecipe.class,
                M3InventoryPureIntAtomCandidates.class,
                M3PinnedDonorInlineRecipe.class,
                M3SemanticIndexRecipe.class,
                M3TypeRelationRecipe.class,
                M3SemanticIndexM3DbBridgeRecipe.class)) {
            assertEquals(
                    M3ContractMode.BEHAVIOR_AND_CONTRACT_PRESERVING,
                    M3RecipeScopeRegistry.require(recipe).contractMode());
        }
    }
}
