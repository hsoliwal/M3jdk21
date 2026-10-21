// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
package com.m3.text;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Canonical SHA-256 framing that preserves every UTF-16 code unit, including lone surrogates. */
public final class M3CorpusProjectionDigest {
  private final MessageDigest digest;

  public M3CorpusProjectionDigest() {
    try {
      digest = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException impossible) {
      throw new AssertionError(impossible);
    }
  }

  public M3CorpusProjectionDigest number(long value) {
    for (int shift = 56; shift >= 0; shift -= 8) digest.update((byte) (value >>> shift));
    return this;
  }

  public M3CorpusProjectionDigest text(CharSequence value) {
    number(value.length());
    for (int i = 0; i < value.length(); i++) {
      digest.update((byte) (value.charAt(i) >>> 8));
      digest.update((byte) value.charAt(i));
    }
    return this;
  }

  public String finish() { return HexFormat.of().formatHex(digest.digest()); }

  public static String bytes(byte[] bytes) {
    M3CorpusProjectionDigest framed = new M3CorpusProjectionDigest();
    framed.digest.update(bytes);
    return framed.finish();
  }
}
