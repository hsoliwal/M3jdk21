// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Execute the same sealed contract in the owning collection module's selected JNI lane. */
class M3SnapshotSortedSelectionTest {
    @TempDir Path temporary;

    @Test void physicalSelectionStableSortKeepsPrivateOwnershipAndEffects() throws Exception {
        M3SnapshotSortedSelectionProof.main(new String[]{
                M3IndexPathMasks.nativeAvailable() ? "native" : "java",
                temporary.resolve("sorted-selection-tape.txt").toString()});
    }
}
