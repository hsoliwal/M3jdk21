// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.fs;
import org.junit.jupiter.api.Test;
class M3SnapshotStoreTest {
  @Test void durableSnapshotsRetainRestartCorruptionOwnershipAndPublicationChecks() throws Exception {
    M3SnapshotStoreSelfTest.main(new String[0]);
  }
}
