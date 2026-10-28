// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
// Adapted for the M3JDK21 M3FS runtime from the pinned Synexia donor.
package com.m3.fs;

import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/** Native profile gate; standalone verification requires JNI rather than skipping it. */
class M3FsIndexedTest {
  @Test
  void indexedNativeOperations() throws Exception {
    String configured = System.getProperty("m3.fs.native.path");
    assumeTrue(configured != null && !configured.isBlank(), "explicit native MIndexFS profile is required");
    assertTrue(M3FS.nativeAvailable(), "configured JNI library must load; never skip a load failure");
    M3FsIndexedSelfTest.main(new String[0]);
    M3FsReadSnapshotSelfTest.main(new String[0]);
  }
}
