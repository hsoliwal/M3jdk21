// Copyright 2026 Hitesh Soliwal and contributors
// Full file-family receiver; source and target mapping in FILE_PORT_MAP.json.
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;
import java.util.Objects;
import java.nio.charset.StandardCharsets;
final class M3FileHash {
  private static final long FNV64_OFFSET = 0xcbf29ce484222325L;
  private static final long FNV64_PRIME = 0x100000001b3L;
  public static long hashText(String text) {
    Objects.requireNonNull(text, "text");
    long hash = FNV64_OFFSET;
    for (byte value : text.getBytes(StandardCharsets.UTF_8)) {
      hash ^= value & 0xff;
      hash *= FNV64_PRIME;
    }
    return hash;
  }

  public static long mix(long left, long right) {
    long z = left ^ Long.rotateLeft(right, 29) ^ 0x9e3779b97f4a7c15L;
    z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
    z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
    return z ^ (z >>> 31);
  }

}
