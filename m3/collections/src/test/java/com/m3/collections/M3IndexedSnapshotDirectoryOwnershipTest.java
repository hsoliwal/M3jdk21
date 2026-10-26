/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** Maven adapter for the identical standalone directory-ownership regression. */
final class M3IndexedSnapshotDirectoryOwnershipTest {
    @Test void immutableDirectorySharingAndFusedSorting() throws Exception {
        M3IndexedSnapshotDirectoryOwnershipSelfTest.main(new String[0]);
    }
}
