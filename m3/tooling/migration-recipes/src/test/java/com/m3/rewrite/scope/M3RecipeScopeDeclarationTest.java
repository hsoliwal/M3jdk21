// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.scope;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.m3.rewrite.InstallIndexStringCompatibility;
import com.synexia.rewrite.M3HashPinnedJavaSnapshotRecipe;
import com.synexia.rewrite.M3MIndexJoinedCharsViewRecipe;
import com.synexia.rewrite.M3SegmentedLaneNativeRecipe;
import org.junit.jupiter.api.Test;

final class M3RecipeScopeDeclarationTest {
    @Test
    void existingRecipesDeclareTheNarrowestWriteBoundary() {
        var joinedChars = new M3MIndexJoinedCharsViewRecipe();
        assertEquals(M3EditScope.FILE, joinedChars.requiredScope());
        assertTrue(joinedChars.fileLocalMechanical());

        var nativeLane = new M3SegmentedLaneNativeRecipe();
        assertEquals(M3EditScope.FILE, nativeLane.requiredScope());
        assertTrue(nativeLane.fileLocalMechanical());

        var compatibility = new InstallIndexStringCompatibility();
        assertEquals(M3EditScope.MODULE, compatibility.requiredScope());

        var collectionCrate = new M3HashPinnedJavaSnapshotRecipe("m3-collection-lanes");
        assertEquals(M3EditScope.MODULE, collectionCrate.requiredScope());
    }

    @Test
    void scopeDeclarationsAreAdmissionGatesNotDocumentation() {
        var fileRecipe = new M3MIndexJoinedCharsViewRecipe();
        assertDoesNotThrow(() -> fileRecipe.requireGrantedScope(M3EditScope.FILE));

        var moduleRecipe = new InstallIndexStringCompatibility();
        assertThrows(
                IllegalArgumentException.class,
                () -> moduleRecipe.requireGrantedScope(M3EditScope.FILE));
        assertDoesNotThrow(() -> moduleRecipe.requireGrantedScope(M3EditScope.MODULE));
    }

    @Test
    void currentMechanicalRecipesRemainBehaviorAndContractPreserving() {
        assertEquals(
                M3ContractMode.BEHAVIOR_AND_CONTRACT_PRESERVING,
                new M3MIndexJoinedCharsViewRecipe().contractMode());
        assertEquals(
                M3ContractMode.BEHAVIOR_AND_CONTRACT_PRESERVING,
                new M3SegmentedLaneNativeRecipe().contractMode());
        assertEquals(
                M3ContractMode.BEHAVIOR_AND_CONTRACT_PRESERVING,
                new InstallIndexStringCompatibility().contractMode());
        assertEquals(
                M3ContractMode.BEHAVIOR_AND_CONTRACT_PRESERVING,
                new M3HashPinnedJavaSnapshotRecipe("m3-collection-lanes").contractMode());
    }
}
