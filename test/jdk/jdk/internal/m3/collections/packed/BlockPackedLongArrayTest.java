/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.m3.collections.packed;

import org.junit.jupiter.api.Test;

final class BlockPackedLongArrayTest {
    @Test void representationAndWidths() { BlockPackedLongArrayContract.representationAndWidths(); }
    @Test void copyRangesAndAtomicity() { BlockPackedLongArrayContract.copyRangesAndAtomicity(); }
    @Test void exactCountsAndBounds() { BlockPackedLongArrayContract.exactCountsAndBounds(); }
    @Test void randomizedOracle() { BlockPackedLongArrayContract.randomizedOracle(); }
}
