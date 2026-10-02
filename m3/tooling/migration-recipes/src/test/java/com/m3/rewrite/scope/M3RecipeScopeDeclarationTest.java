// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.scope;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.m3.rewrite.InstallIndexStringCompatibility;
import com.synexia.rewrite.M3HashPinnedJavaSnapshotRecipe;
import com.synexia.rewrite.M3MIndexJoinedCharsViewRecipe;
import com.synexia.rewrite.M3SegmentedLaneNativeRecipe;
import java.util.List;
import org.junit.jupiter.api.Test;

final class M3RecipeScopeDeclarationTest {
    @Test
    void everyCurrentRecipeHasExternalScopeAuthority() {
        assertEquals(4, M3RecipeScopeRegistry.size());
        assertTrue(M3RecipeScopeRegistry.registered(M3MIndexJoinedCharsViewRecipe.class.getName()));
        assertTrue(M3RecipeScopeRegistry.registered(M3SegmentedLaneNativeRecipe.class.getName()));
        assertTrue(M3RecipeScopeRegistry.registered(InstallIndexStringCompatibility.class.getName()));
        assertTrue(M3RecipeScopeRegistry.registered(M3HashPinnedJavaSnapshotRecipe.class.getName()));
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

        var inferredPolicy = M3RecipeScopeRegistry.require(M3HashPinnedJavaSnapshotRecipe.class);
        assertEquals(
                M3EditScope.FILE,
                inferredPolicy.resolve(List.of("src/main/java/a/A.java")));
        assertEquals(
                M3EditScope.PACKAGE_VISIBILITY,
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
                M3HashPinnedJavaSnapshotRecipe.class)) {
            assertEquals(
                    M3ContractMode.BEHAVIOR_AND_CONTRACT_PRESERVING,
                    M3RecipeScopeRegistry.require(recipe).contractMode());
        }
    }
}
