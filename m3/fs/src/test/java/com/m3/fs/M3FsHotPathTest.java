// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
// Adapted for the M3JDK21 M3FS runtime from the pinned Synexia donor.
package com.m3.fs;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import org.junit.jupiter.api.Test;

/** Optional-native profile wrapper; standalone verification always enables the real library. */
class M3FsHotPathTest {
    @Test
    void integratedBlockPathPreservesBytesAndSnapshots() throws Exception {
        String configured = System.getProperty("m3.fs.native.path");
        assumeTrue(configured != null && !configured.isBlank(), "explicit native MIndexFS profile is required");
        assertTrue(M3FS.supportsReadSnapshots(), "configured JNI must support retained read views");
        M3FsHotPathSelfTest.main(new String[0]);
    }
}
