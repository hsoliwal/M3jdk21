/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import org.junit.jupiter.api.Test;
import jdk.internal.m3.collections.ColumnarGrowthContract;

/** Owning-module regression entry points; all oracles also run without JUnit. */
final class M3ArrayExpansionTest {
    @Test void dequeGrowthAndCopyContracts() throws Exception {
        M3ArrayDequeGrowthContract.run();
    }

    @Test void columnarOrdinaryBehavior() {
        ColumnarGrowthContract.runOrdinary();
    }

    @Test void columnarOverflowBoundaries() {
        ColumnarGrowthContract.runBoundary();
    }
}
