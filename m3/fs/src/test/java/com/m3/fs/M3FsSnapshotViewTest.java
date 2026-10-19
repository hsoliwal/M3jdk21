// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
// Adapted for the M3JDK21 M3FS runtime from the pinned Synexia donor.
package com.m3.fs;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import org.junit.jupiter.api.Test;

/** Native profile gate; an explicitly configured library must support the new contract. */
class M3FsSnapshotViewTest {
  @Test
  void retainedViewsSurviveConcurrentMutation() throws Exception {
    String configured = System.getProperty("m3.fs.native.path");
    assumeTrue(configured != null && !configured.isBlank(), "explicit native MIndexFS profile is required");
    assertTrue(M3FS.supportsReadSnapshots(), "configured JNI must support retained read views");
    M3FsSnapshotViewSelfTest.main(new String[0]);
  }
}
