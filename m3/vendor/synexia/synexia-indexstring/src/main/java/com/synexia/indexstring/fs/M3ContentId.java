// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring.fs;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;

/** Immutable SHA-256 identity for content-addressed M3FileSystem objects. */
public final class M3ContentId implements Comparable<M3ContentId> {
  public static final int BYTES = 32;
  private static final HexFormat HEX = HexFormat.of();

  private final byte[] sha256;
  private final int hashCode;

  private M3ContentId(byte[] sha256) {
    this.sha256 = sha256;
    this.hashCode = Arrays.hashCode(sha256);
  }

  public static M3ContentId of(byte[] sha256) {
    byte[] checked = Objects.requireNonNull(sha256, "sha256").clone();
    if (checked.length != BYTES) {
      throw new IllegalArgumentException("SHA-256 identity must contain exactly 32 bytes");
    }
    return new M3ContentId(checked);
  }

  public static M3ContentId parse(String hex) {
    Objects.requireNonNull(hex, "hex");
    if (hex.length() != BYTES * 2) {
      throw new IllegalArgumentException("SHA-256 identity must contain exactly 64 hex characters");
    }
    try {
      return of(HEX.parseHex(hex));
    } catch (IllegalArgumentException invalid) {
      throw new IllegalArgumentException("invalid SHA-256 identity", invalid);
    }
  }

  public static M3ContentId digest(byte[] bytes) {
    return of(newDigest().digest(Objects.requireNonNull(bytes, "bytes")));
  }

  static MessageDigest newDigest() {
    try {
      return MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException impossible) {
      throw new ExceptionInInitializerError(impossible);
    }
  }

  public byte[] bytes() { return sha256.clone(); }
  public String hex() { return HEX.formatHex(sha256); }

  @Override
  public int compareTo(M3ContentId other) {
    Objects.requireNonNull(other, "other");
    return Arrays.compareUnsigned(sha256, other.sha256);
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof M3ContentId id && MessageDigest.isEqual(sha256, id.sha256);
  }

  @Override public int hashCode() { return hashCode; }
  @Override public String toString() { return hex(); }
}
