// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring.precompute;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/** Exact dependency contract. A hash is an address, never a semantic-equivalence proof. */
public record MIndexPrecomputeScope(
    String specificationRevision, String semanticProfile, String lexiconSnapshot,
    String compilerRevision, String bindingSnapshot, String sourceSha256) {
  public MIndexPrecomputeScope {
    specificationRevision = required(specificationRevision);
    semanticProfile = required(semanticProfile);
    lexiconSnapshot = required(lexiconSnapshot);
    compilerRevision = required(compilerRevision);
    bindingSnapshot = required(bindingSnapshot);
    sourceSha256 = checkedDigest(sourceSha256);
  }

  /** Canonical length-framed UTF-16 encoding, including unpaired surrogate code units. */
  public String fingerprint() {
    MessageDigest digest = digest();
    for (String field : new String[] {"MIndexPrecomputeScope/v1", specificationRevision,
        semanticProfile, lexiconSnapshot, compilerRevision, bindingSnapshot, sourceSha256}) {
      putLong(digest, field.length());
      for (int i = 0; i < field.length(); i++) {
        char c = field.charAt(i);
        digest.update((byte) (c >>> 8));
        digest.update((byte) c);
      }
    }
    return HexFormat.of().formatHex(digest.digest());
  }

  public static String sha256(byte[] bytes) {
    return HexFormat.of().formatHex(digest().digest(Objects.requireNonNull(bytes, "bytes")));
  }

  static String tokenDigest(long[] tokens) {
    MessageDigest digest = digest();
    putLong(digest, tokens.length);
    for (long token : tokens) putLong(digest, token);
    return HexFormat.of().formatHex(digest.digest());
  }

  static String checkedDigest(String value) {
    Objects.requireNonNull(value, "digest");
    if (value.length() != 64) throw new IllegalArgumentException("expected SHA-256 hex");
    byte[] bytes = HexFormat.of().parseHex(value);
    return HexFormat.of().formatHex(bytes);
  }

  static String required(String value) {
    if (Objects.requireNonNull(value, "identity field").isBlank()) {
      throw new IllegalArgumentException("blank identity field");
    }
    return value;
  }

  static MessageDigest digest() {
    try {
      return MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException impossible) {
      throw new AssertionError(impossible);
    }
  }

  static void putLong(MessageDigest digest, long value) {
    for (int shift = 56; shift >= 0; shift -= 8) digest.update((byte) (value >>> shift));
  }
}
