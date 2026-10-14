/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.text;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Domain-separated, UTF-8, length-framed SHA-256 encoding. No delimiter ambiguity. */
public final class Canonical {
  private final MessageDigest digest;
  public Canonical(String domain) {
    try { digest = MessageDigest.getInstance("SHA-256"); }
    catch (NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    text(domain);
  }
  public Canonical number(long value) {
    for (int shift = 56; shift >= 0; shift -= 8) digest.update((byte) (value >>> shift));
    return this;
  }
  public Canonical text(String value) {
    validUnicode(value);
    byte[] data = value.getBytes(StandardCharsets.UTF_8);
    number(data.length);
    digest.update(data);
    return this;
  }
  public String finish() { return HexFormat.of().formatHex(digest.digest()); }
  public static void validUnicode(String value) {
    java.util.Objects.requireNonNull(value, "text");
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      if (Character.isHighSurrogate(c)) {
        if (++i >= value.length() || !Character.isLowSurrogate(value.charAt(i))) {
          throw new IllegalArgumentException("unpaired high surrogate");
        }
      } else if (Character.isLowSurrogate(c)) {
        throw new IllegalArgumentException("unpaired low surrogate");
      }
    }
  }
}
