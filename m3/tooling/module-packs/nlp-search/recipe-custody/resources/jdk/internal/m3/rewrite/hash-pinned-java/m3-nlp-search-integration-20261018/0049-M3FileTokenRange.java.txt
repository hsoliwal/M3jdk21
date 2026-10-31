// Copyright 2026 Hitesh Soliwal and contributors
// Full file-family receiver; source and target mapping in FILE_PORT_MAP.json.
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

/** Immutable token-boundary request for an exact raw-byte digest query. */
public record M3FileTokenRange(int startToken, int endToken) {
  public M3FileTokenRange {
    if (startToken < 0 || endToken < startToken) {
      throw new IllegalArgumentException("invalid token range");
    }
  }
}
