// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.util.Objects;
import java.util.OptionalLong;

/** Exact Synexia frozen-coordinate fingerprint formula, with a target-owned resolver capability. */
public final class M3FileCoordinates {
  private static final long DOMAIN = M3FileHash.hashText("SYNEXIA-MINDEX-COORDINATES-1");
  private M3FileCoordinates() { }

  public static OptionalLong tryOf(M3FileResolver resolver, int languageId) {
    return Objects.requireNonNull(resolver, "resolver").coordinateFingerprint(languageId);
  }

  public static long of(M3FileResolver resolver, int languageId) {
    return tryOf(resolver, languageId).orElseThrow(() ->
        new IllegalArgumentException("durable coordinates require an immutable file dictionary"));
  }

  static long compute(int languageId, String tag, String version, String[] words) {
    long hash = M3FileHash.mix(DOMAIN, Integer.toUnsignedLong(languageId));
    hash = M3FileHash.mix(hash, M3FileHash.hashText(tag));
    hash = M3FileHash.mix(hash, M3FileHash.hashText(version));
    hash = M3FileHash.mix(hash, Integer.toUnsignedLong(words.length));
    for (int local = 0; local < words.length; local++) {
      hash = M3FileHash.mix(hash, M3FileHash.mix(Integer.toUnsignedLong(local),
          M3FileHash.hashText(Objects.requireNonNull(words[local], "dictionary word"))));
    }
    return hash;
  }
}
