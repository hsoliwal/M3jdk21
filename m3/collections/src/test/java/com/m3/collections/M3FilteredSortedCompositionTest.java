// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** JUnit endpoint for primitive live filters and detached sorted mask snapshots. */
final class M3FilteredSortedCompositionTest {
    @Test
    void seededLiveAndSnapshotOracle() throws Exception {
        M3FilteredSortedCompositionSelfTest.main(new String[0]);
    }
}
