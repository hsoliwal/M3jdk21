// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.precompute;

import org.junit.jupiter.api.Test;

final class NcmCodebookTest {
  @Test
  void exactOracleAndNativeParity() throws Exception {
    if (Boolean.getBoolean("m3.precompute.requireLegacyNative")) {
      org.junit.jupiter.api.Assertions.assertTrue(NativePrecomputeOps.available(),
          "legacy precompute ABI must load");
      org.junit.jupiter.api.Assertions.assertTrue(NativePrecomputeOps.tuplesAvailable(),
          "legacy tuple ABI must load");
      org.junit.jupiter.api.Assertions.assertFalse(NativePrecomputeOps.ncmCodebookAvailable(),
          "legacy library must lack NCM feature");
    }
    NcmCodebookParity.verify(Boolean.getBoolean("m3.precompute.requireNcmNative"),
        Boolean.getBoolean("m3.precompute.forbidNcmNative"));
  }
}
