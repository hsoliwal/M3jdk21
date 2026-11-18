/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.collections;

import org.junit.jupiter.api.Test;

/** Maven/JUnit entrypoint for the standalone primitive stable-sort oracle. */
public final class IntIndexedSnapshotSortedLaneTest {
    @Test
    void randomizedSortedAndDistinctContracts() {
        IntIndexedSnapshotSortedLaneSelfTest.main(new String[0]);
    }
}
