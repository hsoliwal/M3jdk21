// Copyright 2026 Hitesh Soliwal and contributors
// Full file-family receiver; source and target mapping in FILE_PORT_MAP.json.
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

/**
 * Two-lane composable fingerprint for one token range.
 *
 * <p>This is a constant-time candidate identity over precomputed token SHA lanes. It is not
 * SHA-256 and every correctness-sensitive equality decision must still use exact token identity or
 * an exact cryptographic digest.</p>
 */
public record M3FileRangeFingerprint(long laneA, long laneB, int tokenCount) {
  public M3FileRangeFingerprint {
    if (tokenCount < 0) throw new IllegalArgumentException("negative token count");
  }
}
