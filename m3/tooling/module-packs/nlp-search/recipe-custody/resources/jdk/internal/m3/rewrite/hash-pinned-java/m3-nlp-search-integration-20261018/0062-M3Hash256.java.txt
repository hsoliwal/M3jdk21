// Copyright 2026 Hitesh Soliwal and contributors
// Full file-family receiver; source and target mapping in FILE_PORT_MAP.json.
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;

/** Immutable SHA-256 value with value semantics and unsigned lexical ordering. */
public final class M3Hash256 implements Comparable<M3Hash256> {
  public static final int BYTES = 32;

  private final byte[] bytes;
  private final int hashCode;

  private M3Hash256(byte[] bytes) {
    if (bytes.length != BYTES) {
      throw new IllegalArgumentException("SHA-256 value must contain exactly " + BYTES + " bytes");
    }
    this.bytes = bytes.clone();
    this.hashCode = Arrays.hashCode(this.bytes);
  }

  public static M3Hash256 of(byte[] bytes) {
    return new M3Hash256(Objects.requireNonNull(bytes, "bytes"));
  }

  public static M3Hash256 parseHex(String hex) {
    Objects.requireNonNull(hex, "hex");
    if (hex.length() != BYTES * 2) {
      throw new IllegalArgumentException("SHA-256 hex value must contain exactly 64 characters");
    }
    return of(HexFormat.of().parseHex(hex));
  }

  public byte[] bytes() {
    return bytes.clone();
  }

  public String hex() {
    return HexFormat.of().formatHex(bytes);
  }

  @Override
  public int compareTo(M3Hash256 other) {
    Objects.requireNonNull(other, "other");
    for (int i = 0; i < BYTES; i++) {
      int comparison = Integer.compare(Byte.toUnsignedInt(bytes[i]), Byte.toUnsignedInt(other.bytes[i]));
      if (comparison != 0) return comparison;
    }
    return 0;
  }

  @Override
  public boolean equals(Object other) {
    return this == other
        || other instanceof M3Hash256 hash && Arrays.equals(bytes, hash.bytes);
  }

  @Override
  public int hashCode() {
    return hashCode;
  }

  @Override
  public String toString() {
    return hex();
  }
}
