/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import org.junit.jupiter.api.Test;

/** Maven adapter for the identical standalone directory-ownership regression. */
final class IndexedSnapshotDirectoryOwnershipTest {
    @Test void immutableDirectorySharingAndFusedSorting() throws Exception {
        IndexedSnapshotDirectoryOwnershipSelfTest.main(new String[0]);
    }
}
