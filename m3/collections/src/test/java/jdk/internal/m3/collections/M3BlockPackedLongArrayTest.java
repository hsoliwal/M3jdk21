/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.m3.collections;

import org.junit.jupiter.api.Test;

final class M3BlockPackedLongArrayTest {
    @Test void representationAndWidths() { M3BlockPackedLongArrayContract.representationAndWidths(); }
    @Test void copyRangesAndAtomicity() { M3BlockPackedLongArrayContract.copyRangesAndAtomicity(); }
    @Test void exactCountsAndBounds() { M3BlockPackedLongArrayContract.exactCountsAndBounds(); }
    @Test void randomizedOracle() { M3BlockPackedLongArrayContract.randomizedOracle(); }
}
