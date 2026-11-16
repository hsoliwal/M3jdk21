/*
 * Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import org.junit.jupiter.api.Test;

/** Owning-module entrypoints for the full matrix and real heap-failure regressions. */
final class PrimitiveMatrixContractTest {
    @Test
    void allPrimitivePairsAndReentrantCallbacks() {
        PrimitiveMatrixContract.main(new String[0]);
    }

    @Test
    void growthFailurePreservesBothLanesAndZeroSidecar() throws Exception {
        PrimitiveMatrixAllocationTest.main(new String[0]);
    }
}
