/* Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import org.junit.jupiter.api.Test;

final class M3LazyTopKWindowTest {
    @Test void exactPrefix() { M3LazyTopKWindowContract.exactPrefix(); }
    @Test void secondarySignalAndTies() { M3LazyTopKWindowContract.secondarySignalAndTies(); }
    @Test void descendingStableTies() { M3LazyTopKWindowContract.descendingStableTies(); }
    @Test void allPrimitiveKinds() { M3LazyTopKWindowContract.allPrimitiveKinds(); }
    @Test void doubleOrdering() { M3LazyTopKWindowContract.doubleOrdering(); }
    @Test void sourceRevision() { M3LazyTopKWindowContract.sourceRevision(); }
    @Test void memoryPressure() { M3LazyTopKWindowContract.memoryPressure(); }
    @Test void zeroAndInvalidInputs() { M3LazyTopKWindowContract.zeroAndInvalidInputs(); }
    @Test void cancellationAtomicity() { M3LazyTopKWindowContract.cancellationAtomicity(); }
    @Test void randomizedLargeCollection() { M3LazyTopKWindowContract.randomizedLargeCollection(); }
}
