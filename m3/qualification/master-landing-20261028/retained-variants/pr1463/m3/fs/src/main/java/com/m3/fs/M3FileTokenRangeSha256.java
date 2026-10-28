// SPDX-License-Identifier: Apache-2.0
package com.m3.fs;

import java.util.Objects;

/** Exact SHA-256 for one logical UTF-8 interval addressed by MIndexFile token boundaries. */
public record M3FileTokenRangeSha256(
    int startToken,
    int endToken,
    long utf8Start,
    long utf8End,
    M3ContentId sha256) {
  public M3FileTokenRangeSha256 {
    if (startToken < 0 || endToken < startToken) {
      throw new IllegalArgumentException("invalid token range");
    }
    if (utf8Start < 0L || utf8End < utf8Start) {
      throw new IllegalArgumentException("invalid UTF-8 byte range");
    }
    sha256 = Objects.requireNonNull(sha256, "sha256");
  }

  /** Number of UTF-8 bytes covered by this token range. */
  public long byteCount() {
    return utf8End - utf8Start;
  }
}
