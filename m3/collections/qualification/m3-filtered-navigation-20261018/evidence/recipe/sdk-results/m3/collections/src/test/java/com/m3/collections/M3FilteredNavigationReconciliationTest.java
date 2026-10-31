/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** Owning-module entry points for the independent wrapper and mask contracts. */
final class M3FilteredNavigationReconciliationTest {
    @Test
    void filteredSortedContracts() {
        M3FilteredNavigationReconciliationContract.main(new String[0]);
    }

    @Test
    void maskAliasesRefuseBeforeEitherBackendWrites() {
        M3IndexPathMasksAliasContract.main(M3IndexPathMasks.nativeAvailable()
                ? new String[] {"--native"} : new String[0]);
    }
}
