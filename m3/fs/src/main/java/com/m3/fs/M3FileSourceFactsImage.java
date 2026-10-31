// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.fs;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.util.Objects;

/** Read-only SMFSA001 admission. Attribute strings remain validated byte spans, not object rows. */
final class M3FileSourceFactsImage {
  static final int MAX_IMAGE_BYTES = 512 * 1024 * 1024;
  private static final long MAGIC = 0x534d465341303031L;
  private static final int MAX_ARRAY_LENGTH = 16_000_000;
  private static final int MAX_STRING_BYTES = 1_048_576;
  private static final long BASE_A = 0x9e3779b185ebca87L;
  private static final long BASE_B = 0xc2b2ae3d27d4eb4fL;
  private M3FileSourceFactsImage() {}

  /** Returns only token-to-raw-byte offsets after complete format, binding and checksum checks. */
  static long[] read(M3MappedObject image, long[] semantic, byte[] rawId, int idOffset, long rawSize)
      throws IOException {
    Objects.checkFromIndexSize(idOffset, 32, Objects.requireNonNull(rawId, "rawId").length);
    try {
      if (image.size() < 32 || image.size() > MAX_IMAGE_BYTES) throw invalid("image length");
      ByteBuffer all = image.bytes(0, image.size()).order(ByteOrder.BIG_ENDIAN);
      int bodyEnd = all.limit() - 32;
      MessageDigest digest = M3ContentId.newDigest();
      digest.update(all.slice(0, bodyEnd));
      byte[] checksum = digest.digest();
      for (int i = 0; i < checksum.length; i++) {
        if (checksum[i] != all.get(bodyEnd + i)) throw invalid("checksum mismatch");
      }
      ByteBuffer data = all.slice(0, bodyEnd).order(ByteOrder.BIG_ENDIAN);
      if (data.getLong() != MAGIC || data.getInt() != 1) throw invalid("magic/format");
      long fingerprint = data.getLong();
      int tokens = count(data.getInt()), unique = count(data.getInt());
      if (semantic.length != 3 || fingerprint != semantic[0]
          || tokens != semantic[1] || unique != semantic[2]) throw invalid("semantic binding");
      long[] before = attributes(data);
      long[] after = attributes(data);
      for (int i = 0; i < 6; i++) {
        if (before[i] != after[i]) throw invalid("source changed during admission");
      }
      if (!sameSpan(data, before[6], after[6]) || before[0] != rawSize) {
        throw invalid("source identity/size binding");
      }
      byteFacts(data, rawId, idOffset, rawSize);
      int boundaries = Math.addExact(tokens, 1);
      expectedCount(data, boundaries);
      if ((long) boundaries * Long.BYTES > data.remaining()) throw invalid("offset lane length");
      long[] offsets = new long[boundaries];
      long previous = 0;
      for (int i = 0; i < boundaries; i++) {
        long value = data.getLong();
        if (value < previous || value > rawSize || (i == 0 && value != 0)) {
          throw invalid("UTF-8 offsets");
        }
        offsets[i] = value;
        previous = value;
      }
      if (previous != rawSize) throw invalid("UTF-8 terminal size");
      expectedCount(data, boundaries);
      int previousPoint = 0;
      for (int i = 0; i < boundaries; i++) {
        int value = data.getInt();
        if (value < previousPoint || value > offsets[i] || (i == 0 && value != 0)) {
          throw invalid("code point offsets");
        }
        previousPoint = value;
      }
      skipBytes(data, Math.multiplyExact(unique, 32));
      prefix(data, boundaries);
      prefix(data, boundaries);
      powers(data, boundaries, BASE_A);
      powers(data, boundaries, BASE_B);
      if (data.hasRemaining()) throw invalid("trailing body bytes");
      return offsets;
    } catch (RuntimeException malformed) {
      throw new IOException("malformed SMFSA001 source facts", malformed);
    }
  }

  private static long[] attributes(ByteBuffer data) throws IOException {
    if (string(data) == -1) throw invalid("null normalized path");
    long size = data.getLong();
    if (size < 0) throw invalid("negative attribute size");
    long[] times = new long[6];
    for (int i = 0; i < 3; i++) {
      long seconds = data.getLong();
      int nanos = data.getInt();
      seconds = Math.addExact(seconds, Math.floorDiv(nanos, 1_000_000_000));
      nanos = Math.floorMod(nanos, 1_000_000_000);
      if (seconds < -31557014167219200L || seconds > 31556889864403199L) {
        throw invalid("file time range");
      }
      times[2 * i] = seconds;
      times[2 * i + 1] = nanos;
    }
    int flags = 0;
    for (int i = 0; i < 4; i++) if (bool(data)) flags |= 1 << i;
    long key = string(data);
    string(data); // owner
    string(data); // group
    bool(data); // POSIX available
    if ((data.getInt() & ~0x1ff) != 0) throw invalid("POSIX permission bits");
    for (int i = 0; i < 5; i++) bool(data);
    int acl = count(data.getInt());
    if ((long) acl * 4 > data.remaining()) throw invalid("ACL count");
    for (int i = 0; i < acl; i++) if (string(data) == -1) throw invalid("null ACL entry");
    map(data);
    map(data);
    // Access time is excluded, matching the donor's sameContentIdentity contract.
    return new long[] { size, times[0], times[1], times[2], times[3], flags, key };
  }

  private static void byteFacts(ByteBuffer data, byte[] id, int idOffset, long rawSize)
      throws IOException {
    long byteCount = data.getLong();
    if (byteCount != rawSize || byteCount < 0) throw invalid("raw byte count");
    for (int i = 0; i < 32; i++) if (data.get() != id[idOffset + i]) throw invalid("raw SHA-256");
    long total = 0;
    int unique = 0;
    double entropy = 0;
    for (int i = 0; i < 256; i++) {
      long frequency = data.getLong();
      if (frequency < 0) throw invalid("negative byte frequency");
      total = Math.addExact(total, frequency);
      if (frequency != 0) {
        unique++;
        double probability = (double) frequency / Math.max(1L, byteCount);
        entropy -= probability * (Math.log(probability) / Math.log(2.0));
      }
    }
    skipBytes(data, (int) Math.min(64L, byteCount));
    skipBytes(data, (int) Math.min(64L, byteCount));
    int storedUnique = data.getInt();
    double storedEntropy = Double.longBitsToDouble(data.getLong());
    double tolerance = Math.max(1.0e-12, 1024.0 * Math.ulp(Math.max(1.0, entropy)));
    boolean entropyMatches = entropy == 0
        ? Double.doubleToLongBits(storedEntropy) == Double.doubleToLongBits(0.0)
        : Math.abs(storedEntropy - entropy) <= tolerance;
    if (total != byteCount || storedUnique != unique || !Double.isFinite(storedEntropy)
        || storedEntropy < 0 || !entropyMatches) throw invalid("byte statistics");
    int chunks = count(data.getInt());
    if ((long) chunks * 36 > data.remaining()) throw invalid("chunk lane length");
    long chunkTotal = 0;
    for (int i = 0; i < chunks; i++) {
      int length = data.getInt();
      if (length < 1 || length > 512 * 1024 || (i + 1 < chunks && length != 512 * 1024)) {
        throw invalid("chunk length");
      }
      chunkTotal = Math.addExact(chunkTotal, length);
      skip(data, 32);
    }
    if (chunkTotal != rawSize) throw invalid("chunk total");
  }

  private static void prefix(ByteBuffer data, int expected) throws IOException {
    expectedCount(data, expected);
    if (data.getLong() != 0) throw invalid("prefix origin");
    skip(data, Math.multiplyExact(expected - 1, Long.BYTES));
  }

  private static void powers(ByteBuffer data, int expected, long base) throws IOException {
    expectedCount(data, expected);
    long power = 1;
    for (int i = 0; i < expected; i++) {
      if (data.getLong() != power) throw invalid("range power recurrence");
      power *= base;
    }
  }

  private static void expectedCount(ByteBuffer data, int expected) throws IOException {
    if (data.getInt() != expected) throw invalid("lane count");
  }
  private static void skipBytes(ByteBuffer data, int expected) throws IOException {
    expectedCount(data, expected);
    skip(data, expected);
  }
  private static void skip(ByteBuffer data, int bytes) throws IOException {
    if (bytes < 0 || bytes > data.remaining()) throw invalid("truncated lane");
    data.position(data.position() + bytes);
  }
  private static int count(int value) throws IOException {
    if (value < 0 || value > MAX_ARRAY_LENGTH) throw invalid("count bounds");
    return value;
  }
  private static boolean bool(ByteBuffer data) { return data.get() != 0; }

  private static void map(ByteBuffer data) throws IOException {
    int count = count(data.getInt());
    if ((long) count * 8 > data.remaining()) throw invalid("map count");
    long previous = -1;
    for (int i = 0; i < count; i++) {
      long key = string(data);
      if (key == -1 || (previous != -1 && compareUtf16(data, previous, key) >= 0)) {
        throw invalid("map key order");
      }
      if (string(data) == -1) throw invalid("null map value");
      previous = key;
    }
  }

  /** Returns start/length packed in one long; null is -1. Validates UTF-8 without String objects. */
  private static long string(ByteBuffer data) throws IOException {
    int length = data.getInt();
    if (length == -1) return -1;
    if (length < 0 || length > MAX_STRING_BYTES || length > data.remaining()) {
      throw invalid("string length");
    }
    int start = data.position(), end = start + length, at = start;
    while (at < end) at = (int) (codePoint(data, at, end) >>> 32);
    data.position(end);
    return ((long) start << 32) | Integer.toUnsignedLong(length);
  }

  private static boolean sameSpan(ByteBuffer data, long left, long right) {
    if (left == -1 || right == -1) return left == right;
    int length = (int) left;
    if (length != (int) right) return false;
    int a = (int) (left >>> 32), b = (int) (right >>> 32);
    for (int i = 0; i < length; i++) if (data.get(a + i) != data.get(b + i)) return false;
    return true;
  }

  /** Java String.compareTo order, including supplementary scalars before BMP U+E000..FFFF. */
  private static int compareUtf16(ByteBuffer data, long left, long right) throws IOException {
    int a = (int) (left >>> 32), b = (int) (right >>> 32);
    int ae = a + (int) left, be = b + (int) right;
    while (a < ae && b < be) {
      long av = codePoint(data, a, ae), bv = codePoint(data, b, be);
      int ac = (int) av, bc = (int) bv;
      int ah = ac > 0xffff ? Character.highSurrogate(ac) : ac;
      int bh = bc > 0xffff ? Character.highSurrogate(bc) : bc;
      if (ah != bh) return Integer.compare(ah, bh);
      if (ac != bc) return Integer.compare(ac, bc);
      a = (int) (av >>> 32);
      b = (int) (bv >>> 32);
    }
    return Integer.compare(ae - a, be - b);
  }

  private static long codePoint(ByteBuffer data, int at, int end) throws IOException {
    int b = Byte.toUnsignedInt(data.get(at++));
    int cp, n, minimum;
    if (b < 0x80) return ((long) at << 32) | b;
    if (b >= 0xc2 && b <= 0xdf) { cp = b & 31; n = 1; minimum = 0x80; }
    else if (b >= 0xe0 && b <= 0xef) { cp = b & 15; n = 2; minimum = 0x800; }
    else if (b >= 0xf0 && b <= 0xf4) { cp = b & 7; n = 3; minimum = 0x10000; }
    else throw invalid("UTF-8 lead");
    if (end - at < n) throw invalid("UTF-8 truncation");
    for (int i = 0; i < n; i++) {
      int next = Byte.toUnsignedInt(data.get(at++));
      if ((next & 0xc0) != 0x80) throw invalid("UTF-8 continuation");
      cp = (cp << 6) | (next & 63);
    }
    if (cp < minimum || cp > 0x10ffff || (cp >= 0xd800 && cp <= 0xdfff)) {
      throw invalid("UTF-8 scalar");
    }
    return ((long) at << 32) | cp;
  }

  private static IOException invalid(String reason) {
    return new IOException("invalid SMFSA001 source facts: " + reason);
  }
}
