/* Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import org.junit.jupiter.api.Test;

final class LazyTopKWindowTest {
    @Test void exactPrefix() { LazyTopKWindowContract.exactPrefix(); }
    @Test void secondarySignalAndTies() { LazyTopKWindowContract.secondarySignalAndTies(); }
    @Test void descendingStableTies() { LazyTopKWindowContract.descendingStableTies(); }
    @Test void allPrimitiveKinds() { LazyTopKWindowContract.allPrimitiveKinds(); }
    @Test void doubleOrdering() { LazyTopKWindowContract.doubleOrdering(); }
    @Test void sourceRevision() { LazyTopKWindowContract.sourceRevision(); }
    @Test void memoryPressure() { LazyTopKWindowContract.memoryPressure(); }
    @Test void zeroAndInvalidInputs() { LazyTopKWindowContract.zeroAndInvalidInputs(); }
    @Test void cancellationAtomicity() { LazyTopKWindowContract.cancellationAtomicity(); }
    @Test void randomizedLargeCollection() { LazyTopKWindowContract.randomizedLargeCollection(); }
}
