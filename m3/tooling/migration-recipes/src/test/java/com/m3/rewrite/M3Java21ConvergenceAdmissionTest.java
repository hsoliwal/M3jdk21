// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.m3.rewrite.scope.M3ContractMode;
import com.m3.rewrite.scope.M3EditScope;
import com.m3.rewrite.scope.M3RecipeScopeRegistry;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

/**
 * Admission proof for the single trusted Java 21 FILE-local convergence DAG.
 *
 * <p>Every child must be explicitly registered as FILE scoped and behavior/contract preserving
 * before the composite may be used as a repository-wide convergence entry point.
 */
final class M3Java21ConvergenceAdmissionTest {
    private static final List<String> SINGLE_FILE =
            List.of("src/main/java/example/Admission.java");

    @Test
    void everyTrustedChildIsRegisteredAtFileScopeWithLockedContract() {
        Recipe convergence = new M3Java21ConvergenceRecipe();

        for (Recipe child : convergence.getRecipeList()) {
            var policy = M3RecipeScopeRegistry.require(child.getClass());
            assertEquals(M3EditScope.FILE, policy.resolve(SINGLE_FILE), child.getName());
            assertEquals(
                    M3ContractMode.BEHAVIOR_AND_CONTRACT_PRESERVING,
                    policy.contractMode(),
                    child.getName());
            assertTrue(policy.fileLocalMechanical(SINGLE_FILE), child.getName());
        }
    }

    @Test
    void compositeItselfIsAdmittedAtTheSameFileLocalContractBoundary() {
        var policy = M3RecipeScopeRegistry.require(M3Java21ConvergenceRecipe.class);

        assertEquals(M3EditScope.FILE, policy.resolve(SINGLE_FILE));
        assertEquals(
                M3ContractMode.BEHAVIOR_AND_CONTRACT_PRESERVING,
                policy.contractMode());
        assertTrue(policy.fileLocalMechanical(SINGLE_FILE));
    }
}
