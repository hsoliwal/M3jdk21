/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import org.junit.jupiter.api.Test;

final class M3IndexedSnapshotRecoveryTest {
    @Test void physicalOccurrenceCoordinatesAndIndependentStableOracle() {
        M3IndexedSnapshotRecoveryContract.main(new String[] {"coordinates"});
    }

    @Test void sharedDirectoriesRemainImmutableOnSuccessfulAndThrowingSort() {
        M3IndexedSnapshotRecoveryContract.main(new String[] {"composition"});
    }

    @Test void callbacksAndStrictRefusalsPreserveTheReceivingContract() {
        M3IndexedSnapshotRecoveryContract.main(new String[] {"failure"});
    }
}
