// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.convergence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.m3.rewrite.M3Java21ConvergenceRecipe;
import com.m3.rewrite.atom.M3AtomizePureIntReturnRecipe;
import com.m3.rewrite.atom.M3DocumentPureIntAtomRecipe;
import com.m3.rewrite.atom.M3InventoryPureIntAtomCandidates;
import com.m3.rewrite.atom.M3PatternizePureIntAtomRecipe;
import com.m3.rewrite.scope.M3ContractMode;
import com.m3.rewrite.scope.M3EditScope;
import com.m3.rewrite.scope.M3RecipeScopeRegistry;
import java.util.List;
import org.junit.jupiter.api.Test;

final class M3FileConvergenceRecipeDagTest {
    @Test
    void registryIsTheSingleOrderedFileRecipeAuthority() {
        var atoms = M3FileConvergenceRecipeDag.atoms();

        assertEquals(4, atoms.size());
        assertEquals(
                List.of(
                        M3FileConvergenceRecipeDag.Phase.INVENTORY,
                        M3FileConvergenceRecipeDag.Phase.ATOMIZATION,
                        M3FileConvergenceRecipeDag.Phase.PATTERNIZATION,
                        M3FileConvergenceRecipeDag.Phase.DOCUMENTATION),
                atoms.stream().map(M3FileConvergenceRecipeDag.Atom::phase).toList());
        assertEquals(M3InventoryPureIntAtomCandidates.class, atoms.get(0).recipe().getClass());
        assertEquals(M3AtomizePureIntReturnRecipe.class, atoms.get(1).recipe().getClass());
        assertEquals(M3PatternizePureIntAtomRecipe.class, atoms.get(2).recipe().getClass());
        assertEquals(M3DocumentPureIntAtomRecipe.class, atoms.get(3).recipe().getClass());

        assertEquals(1, M3FileConvergenceRecipeDag.phase(
                M3FileConvergenceRecipeDag.Phase.ATOMIZATION).size());
        assertTrue(M3FileConvergenceRecipeDag.Phase.ATOMIZATION.mutating());
        assertTrue(M3FileConvergenceRecipeDag.Phase.PATTERNIZATION.mutating());
        assertTrue(M3FileConvergenceRecipeDag.Phase.DOCUMENTATION.mutating());
        assertFalse(M3FileConvergenceRecipeDag.Phase.INVENTORY.mutating());
    }

    @Test
    void everyRegisteredAtomIsFileScopedAndContractPreserving() {
        for (var atom : M3FileConvergenceRecipeDag.atoms()) {
            var policy = M3RecipeScopeRegistry.require(atom.recipe().getClass());
            assertEquals(M3EditScope.FILE, policy.minimumScope(), atom.id());
            assertEquals(
                    M3ContractMode.BEHAVIOR_AND_CONTRACT_PRESERVING,
                    policy.contractMode(),
                    atom.id());
        }
    }

    @Test
    void compositeRecipeUsesExactlyTheSameAtomicRegistry() {
        var composite = new M3Java21ConvergenceRecipe();

        assertEquals(
                M3FileConvergenceRecipeDag.atoms().stream()
                        .map(atom -> atom.recipe().getClass())
                        .toList(),
                composite.getRecipeList().stream()
                        .map(Object::getClass)
                        .toList());
        assertEquals(
                M3Java21ConvergenceRecipe.class,
                M3FileConvergenceRecipeDag.fixedPointRecipe().getClass());
    }
}
