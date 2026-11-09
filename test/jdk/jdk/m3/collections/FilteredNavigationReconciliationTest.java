/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import org.junit.jupiter.api.Test;

/** Owning-module entry points for the independent wrapper and mask contracts. */
final class FilteredNavigationReconciliationTest {
    @Test
    void filteredSortedContracts() {
        FilteredNavigationReconciliationContract.main(new String[0]);
    }

    @Test
    void maskAliasesRefuseBeforeEitherBackendWrites() {
        IndexPathMasksAliasContract.main(IndexPathMasks.nativeAvailable()
                ? new String[] {"--native"} : new String[0]);
    }
}
