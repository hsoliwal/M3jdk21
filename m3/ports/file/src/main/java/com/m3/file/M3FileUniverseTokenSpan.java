// Copyright 2026 Hitesh Soliwal and contributors
// Full file-family receiver; source and target mapping in FILE_PORT_MAP.json.
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import com.m3.file.M3Hash256;
import java.util.Objects;

/** Immutable global-coordinate token span projected from one Universe snapshot. */
public record M3FileUniverseTokenSpan(
    int globalTokenOrdinal,
    int fileOrdinal,
    int fileTokenOrdinal,
    int utf16Start,
    int utf16End,
    long utf8Start,
    long utf8End,
    int codePointStart,
    int codePointEnd,
    M3Hash256 sha256) {
  public M3FileUniverseTokenSpan {
    if (globalTokenOrdinal < 0 || fileOrdinal < 0 || fileTokenOrdinal < 0) {
      throw new IllegalArgumentException("negative Universe token coordinate");
    }
    if (utf16Start < 0 || utf16End < utf16Start) {
      throw new IllegalArgumentException("invalid Universe UTF-16 span");
    }
    if (utf8Start < 0L || utf8End < utf8Start) {
      throw new IllegalArgumentException("invalid Universe UTF-8 span");
    }
    if (codePointStart < 0 || codePointEnd < codePointStart) {
      throw new IllegalArgumentException("invalid Universe code-point span");
    }
    sha256 = Objects.requireNonNull(sha256, "sha256");
  }

  public int utf16Length() {
    return utf16End - utf16Start;
  }

  public long utf8Length() {
    return utf8End - utf8Start;
  }

  public int codePointLength() {
    return codePointEnd - codePointStart;
  }
}
