// Copyright 2026 Hitesh Soliwal and contributors
// Full file-family receiver; source and target mapping in FILE_PORT_MAP.json.
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.util.Objects;

/**
 * Optional Rabin-Karp-style rolling index over one file's local token handles.
 *
 * <p>Hashes are candidate filters only. Every equality/search result is confirmed token-by-token,
 * so 64-bit overflow collisions cannot change correctness.</p>
 */
public final class M3FileRollingIndex {
  static final long BASE = 0x9e3779b185ebca87L;

  private final M3File file;
  private final long[] prefix;
  private final long[] powers;
  private final boolean ownsPayload;

  public M3FileRollingIndex(M3File file) {
    this.file = Objects.requireNonNull(file, "file");
    long[] admittedPrefix = file.precomputedRollingPrefix();
    long[] admittedPowers = file.precomputedRollingPowers();
    if (admittedPrefix != null) {
      prefix = admittedPrefix;
      powers = admittedPowers;
      ownsPayload = false;
    } else {
      prefix = new long[file.tokenCount() + 1];
      powers = new long[file.tokenCount() + 1];
      powers[0] = 1L;
      for (int i = 0; i < file.tokenCount(); i++) {
        long token = Integer.toUnsignedLong(file.localHandleAt(i)) + 1L;
        prefix[i + 1] = prefix[i] * BASE + token;
        powers[i + 1] = powers[i] * BASE;
      }
      ownsPayload = true;
    }
  }

  public M3File file() {
    return file;
  }

  public long hash(int startToken, int endToken) {
    Objects.checkFromToIndex(startToken, endToken, file.tokenCount());
    int length = endToken - startToken;
    return prefix[endToken] - prefix[startToken] * powers[length];
  }

  public boolean sameTokens(int leftStart, int rightStart, int length) {
    if (length < 0) throw new IllegalArgumentException("negative length");
    Objects.checkFromIndexSize(leftStart, length, file.tokenCount());
    Objects.checkFromIndexSize(rightStart, length, file.tokenCount());
    if (leftStart == rightStart || length == 0) return true;
    if (hash(leftStart, leftStart + length) != hash(rightStart, rightStart + length)) {
      return false;
    }
    for (int i = 0; i < length; i++) {
      if (file.localHandleAt(leftStart + i) != file.localHandleAt(rightStart + i)) return false;
    }
    return true;
  }

  /**
   * Finds a span from the same file using rolling candidates followed by exact token confirmation.
   */
  public int indexOf(M3FileSpan needle, int fromToken) {
    Objects.requireNonNull(needle, "needle");
    if (needle.file() != file) throw new IllegalArgumentException("span belongs to another file");
    if (fromToken < 0 || fromToken > file.tokenCount()) {
      throw new IndexOutOfBoundsException("from token " + fromToken);
    }
    int length = needle.tokenCount();
    if (length == 0) return fromToken;
    int limit = file.tokenCount() - length;
    if (fromToken > limit) return -1;
    long expected = hash(needle.startToken(), needle.endToken());
    for (int at = fromToken; at <= limit; at++) {
      if (hash(at, at + length) == expected
          && sameTokens(at, needle.startToken(), length)) {
        return at;
      }
    }
    return -1;
  }

  public long retainedPayloadBytes() {
    return ownsPayload ? Long.BYTES * (long) (prefix.length + powers.length) : 0L;
  }
}
