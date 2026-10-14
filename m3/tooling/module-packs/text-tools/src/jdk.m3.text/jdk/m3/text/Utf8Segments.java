// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.text;

import java.security.MessageDigest;
import java.util.Objects;

/**
 * Stateless UTF-16 to UTF-8 boundary operations over a logical M3-backed String.
 * The input remains canonical; this class owns no text payload or intern table.
 * Malformed surrogate sequences are refused instead of silently replaced.
 */
final class Utf8Segments {
  private static final int CHUNK_BYTES = 8192;

  private Utf8Segments() {}

  static long length(String text) {
    Objects.requireNonNull(text, "text");
    long bytes = 0;
    for (int i = 0, n = text.length(); i < n; i++) {
      char ch = text.charAt(i);
      if (ch < 0x80) bytes++;
      else if (ch < 0x800) bytes += 2;
      else if (Character.isHighSurrogate(ch)) {
        if (i + 1 >= n || !Character.isLowSurrogate(text.charAt(i + 1))) {
          throw new IllegalArgumentException("unpaired high surrogate");
        }
        i++;
        bytes += 4;
      } else if (Character.isLowSurrogate(ch)) {
        throw new IllegalArgumentException("unpaired low surrogate");
      } else bytes += 3;
    }
    return bytes;
  }

  /** Writes UTF-8 directly into the digest through a bounded reusable-per-call chunk. */
  static void update(MessageDigest digest, String text) {
    Objects.requireNonNull(digest, "digest");
    Objects.requireNonNull(text, "text");
    if (text.isEmpty()) return;
    byte[] chunk = new byte[Math.min(CHUNK_BYTES, Math.max(4, text.length()))];
    int used = 0;
    for (int i = 0, n = text.length(); i < n; i++) {
      int codePoint = text.charAt(i);
      if (Character.isHighSurrogate((char) codePoint)) {
        if (i + 1 >= n || !Character.isLowSurrogate(text.charAt(i + 1))) {
          throw new IllegalArgumentException("unpaired high surrogate");
        }
        codePoint = Character.toCodePoint((char) codePoint, text.charAt(++i));
      } else if (Character.isLowSurrogate((char) codePoint)) {
        throw new IllegalArgumentException("unpaired low surrogate");
      }
      int width = codePoint <= 0x7f ? 1 : codePoint <= 0x7ff ? 2
          : codePoint <= 0xffff ? 3 : 4;
      if (chunk.length - used < width) {
        digest.update(chunk, 0, used);
        used = 0;
      }
      switch (width) {
        case 1 -> chunk[used++] = (byte) codePoint;
        case 2 -> {
          chunk[used++] = (byte) (0xc0 | (codePoint >>> 6));
          chunk[used++] = (byte) (0x80 | (codePoint & 0x3f));
        }
        case 3 -> {
          chunk[used++] = (byte) (0xe0 | (codePoint >>> 12));
          chunk[used++] = (byte) (0x80 | ((codePoint >>> 6) & 0x3f));
          chunk[used++] = (byte) (0x80 | (codePoint & 0x3f));
        }
        case 4 -> {
          chunk[used++] = (byte) (0xf0 | (codePoint >>> 18));
          chunk[used++] = (byte) (0x80 | ((codePoint >>> 12) & 0x3f));
          chunk[used++] = (byte) (0x80 | ((codePoint >>> 6) & 0x3f));
          chunk[used++] = (byte) (0x80 | (codePoint & 0x3f));
        }
        default -> throw new AssertionError(width);
      }
    }
    if (used != 0) digest.update(chunk, 0, used);
  }
}
