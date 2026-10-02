// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.scope;

/**
 * M3 recipe contract: every transformation declares the narrowest edit scope and contract mode.
 */
public interface M3ScopedRecipe {
    M3EditScope requiredScope();

    M3ContractMode contractMode();

    /**
     * Refuses execution authority broader or weaker than the recipe contract permits.
     *
     * <p>Contract-changing recipes are admitted only at LIBRARY_API scope. Behavior-preserving
     * recipes may run at their declared boundary or a broader explicitly granted boundary.
     */
    default void requireGrantedScope(M3EditScope grantedScope) {
        if (!grantedScope.permits(requiredScope())) {
            throw new IllegalArgumentException(
                    "recipe requires " + requiredScope() + " but was granted " + grantedScope);
        }
        if (contractMode() == M3ContractMode.EXPLICIT_CONTRACT_CHANGE
                && grantedScope != M3EditScope.LIBRARY_API) {
            throw new IllegalArgumentException(
                    "contract-changing recipe requires LIBRARY_API authority");
        }
    }

    /** True only for embarrassingly parallel file-local BCP work. */
    default boolean fileLocalMechanical() {
        return requiredScope() == M3EditScope.FILE
                && contractMode() == M3ContractMode.BEHAVIOR_AND_CONTRACT_PRESERVING;
    }
}
