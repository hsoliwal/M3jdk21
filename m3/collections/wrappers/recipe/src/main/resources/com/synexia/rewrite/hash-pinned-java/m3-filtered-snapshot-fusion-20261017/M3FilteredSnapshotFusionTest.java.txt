// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** Actual Maven/JUnit wrapper for one-pass additive M3 snapshot API proof. */
final class M3FilteredSnapshotFusionTest {
    @Test void filteredSnapshotsPreserveExistingJdkAndCollectionViewContracts() {
        M3FilteredSnapshotFusionProof.main(new String[0]);
    }
}
