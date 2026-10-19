// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
// Adapted for the M3JDK21 M3FS runtime from the pinned Synexia donor.
package com.m3.fs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

/**
 * Linux filesystem/JNI checks through the real module/loader when running repository tests.
 * Other platforms require their own mapping-lifetime and symbolic-link acceptance suites.
 */
@EnabledOnOs(OS.LINUX)
final class M3MappedStorageTest {
  @Test
  void mappedStorageAndExactByteContracts() throws Exception {
    String mode = System.getProperty("m3fs.native", "auto");
    M3MappedStorageChecks.main(new String[] {mode});
  }
}
