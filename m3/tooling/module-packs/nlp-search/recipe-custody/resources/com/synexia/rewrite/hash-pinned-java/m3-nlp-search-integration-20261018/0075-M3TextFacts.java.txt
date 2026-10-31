// Copyright 2026 Hitesh Soliwal and contributors
// Full file-family receiver; source and target mapping in FILE_PORT_MAP.json.
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.util.Objects;
import java.util.function.IntUnaryOperator;

/**
 * Content-only facts that are safe to retain once per canonical UTF-16 payload.
 *
 * <p>Every hash/signal is a candidate filter except the character flags. Matching filters never
 * prove equality or containment; exact UTF-16 verification remains authoritative.</p>
 */
public record M3TextFacts(
    int characterFlags,
    int asciiUpperHash,
    int asciiLowerHash,
    int asciiTitleHash,
    long prefix4,
    long suffix4,
    long bigramSignal64,
    long trigramSignal64) {

  public static final M3TextFacts EMPTY =
      new M3TextFacts(M3CharacterFlags.initial(0), 0, 0, 0, 0L, 0L, 0L, 0L);

  /**
   * Composes facts for the same immutable text summarized by the supplied metrics.
   *
   * <p>Only packed boundary units are consulted. Presence flags and n-gram filters are ORed;
   * universal flags are ANDed. One bigram and up to two trigrams are added at the seam. Case hashes
   * use UTF-16 lengths and ordinary Java int overflow. ASCII title case affects only the first unit
   * of the complete value, so a nonempty left child consumes the right child's lower-case hash.
   * No hash or signal hit becomes an exact equality or containment result.
   */
  public static M3TextFacts combine(
      M3TextFacts left, M3TextMetrics leftMetrics,
      M3TextFacts right, M3TextMetrics rightMetrics) {
    Objects.requireNonNull(left, "left");
    Objects.requireNonNull(leftMetrics, "leftMetrics");
    Objects.requireNonNull(right, "right");
    Objects.requireNonNull(rightMetrics, "rightMetrics");
    return combine(left, leftMetrics.utf16Length(), right, rightMetrics.utf16Length(),
        rightMetrics.hash31Power());
  }

  /** UTF-16-only composition for admitted spans; no aggregate byte count is needed. */
  static M3TextFacts combine(
      M3TextFacts left, int leftLength,
      M3TextFacts right, int rightLength, int rightPower) {
    Objects.requireNonNull(left, "left");
    Objects.requireNonNull(right, "right");
    Math.addExact(leftLength, rightLength);
    if (leftLength == 0) return right;
    if (rightLength == 0) return left;

    int universal = M3CharacterFlags.ASCII | M3CharacterFlags.LATIN1
        | M3CharacterFlags.BLANK | M3CharacterFlags.EMPTY;
    int flags = ((left.characterFlags & right.characterFlags) & universal)
        | ((left.characterFlags | right.characterFlags) & ~universal);
    long prefix = leftLength >= 4 ? left.prefix4
        : left.prefix4 | (right.prefix4 >>> (leftLength * 16));
    long suffix = rightLength >= 4 ? right.suffix4
        : (left.suffix4 << (rightLength * 16)) | right.suffix4;
    char last = (char) left.suffix4;
    char first = (char) (right.prefix4 >>> 48);
    long bigrams = addBigramSignal(left.bigramSignal64 | right.bigramSignal64, last, first);
    long trigrams = left.trigramSignal64 | right.trigramSignal64;
    if (leftLength >= 2) {
      trigrams = addTrigramSignal(trigrams, (char) (left.suffix4 >>> 16), last, first);
    }
    if (rightLength >= 2) {
      trigrams = addTrigramSignal(trigrams, last, first, (char) (right.prefix4 >>> 32));
    }
    int power = rightPower;
    return new M3TextFacts(
        flags, left.asciiUpperHash * power + right.asciiUpperHash,
        left.asciiLowerHash * power + right.asciiLowerHash,
        left.asciiTitleHash * power + right.asciiLowerHash,
        prefix, suffix, bigrams, trigrams);
  }

  public static M3TextFacts of(CharSequence value) {
    CharSequence text = Objects.requireNonNull(value, "value");
    return compile(text.length(), text::charAt);
  }

  private static M3TextFacts compile(int length, IntUnaryOperator units) {
    if (length == 0) return EMPTY;
    int flags = M3CharacterFlags.initial(length);
    int upper = 0;
    int lower = 0;
    int title = 0;
    long bigrams = 0L;
    long trigrams = 0L;
    long prefix = 0L;
    long suffix = 0L;

    char previous2 = 0;
    char previous1 = 0;
    for (int index = 0; index < length; index++) {
      char unit = (char) units.applyAsInt(index);
      if (index < 4) prefix |= (long) unit << (48 - (index << 4));
      suffix = (suffix << 16) | unit;
      flags = M3CharacterFlags.add(flags, unit);
      upper = 31 * upper + asciiUpper(unit);
      lower = 31 * lower + asciiLower(unit);
      title = 31 * title + (index == 0 ? asciiUpper(unit) : asciiLower(unit));
      if (index >= 1) bigrams = addSignal(bigrams, pair(previous1, unit));
      if (index >= 2) trigrams = addSignal(trigrams, triple(previous2, previous1, unit));
      previous2 = previous1;
      previous1 = unit;
    }

    return new M3TextFacts(
        flags,
        upper,
        lower,
        title,
        prefix,
        suffix,
        bigrams,
        trigrams);
  }

  /** Packed first four UTF-16 units, left aligned. */
  public static long prefix4(CharSequence value) {
    CharSequence text = Objects.requireNonNull(value, "value");
    int width = Math.min(4, text.length());
    long packed = 0L;
    for (int index = 0; index < width; index++) {
      packed |= (long) text.charAt(index) << (48 - (index << 4));
    }
    return packed;
  }

  /** Packed last four UTF-16 units, right aligned. */
  public static long suffix4(CharSequence value) {
    CharSequence text = Objects.requireNonNull(value, "value");
    int width = Math.min(4, text.length());
    long packed = 0L;
    int start = text.length() - width;
    for (int index = 0; index < width; index++) {
      packed = (packed << 16) | text.charAt(start + index);
    }
    return packed;
  }

  static long addBigramSignal(long signal, char first, char second) {
    return addSignal(signal, pair(first, second));
  }

  static long addTrigramSignal(long signal, char first, char second, char third) {
    return addSignal(signal, triple(first, second, third));
  }

  public static long bigramSignal64(CharSequence value) {
    CharSequence text = Objects.requireNonNull(value, "value");
    long signal = 0L;
    for (int index = 1; index < text.length(); index++) {
      signal = addBigramSignal(signal, text.charAt(index - 1), text.charAt(index));
    }
    return signal;
  }

  public static long trigramSignal64(CharSequence value) {
    CharSequence text = Objects.requireNonNull(value, "value");
    long signal = 0L;
    for (int index = 2; index < text.length(); index++) {
      signal =
          addTrigramSignal(
              signal,
              text.charAt(index - 2),
              text.charAt(index - 1),
              text.charAt(index));
    }
    return signal;
  }

  /**
   * Necessary condition using two already-precomputed canonical fact images.
   *
   * <p>No spelling is read. A false result proves the needle cannot occur; a true result remains
   * only a Bloom-style candidate and must be followed by exact UTF-16 verification.</p>
   */
  public boolean mayContain(M3TextFacts needle, int needleUtf16Length) {
    M3TextFacts checked = Objects.requireNonNull(needle, "needle");
    if (needleUtf16Length < 0) throw new IllegalArgumentException("needleUtf16Length");
    if (needleUtf16Length >= 3
        && (trigramSignal64 & checked.trigramSignal64) != checked.trigramSignal64) {
      return false;
    }
    return needleUtf16Length < 2
        || (bigramSignal64 & checked.bigramSignal64) == checked.bigramSignal64;
  }

  /**
   * Necessary complete-value prefix condition using only packed canonical boundary facts.
   * Exact region comparison remains authoritative.
   */
  public boolean prefixMayMatch(
      M3TextFacts prefix, int prefixUtf16Length) {
    M3TextFacts checked = Objects.requireNonNull(prefix, "prefix");
    if (prefixUtf16Length < 0) throw new IllegalArgumentException("prefixUtf16Length");
    int width = Math.min(4, prefixUtf16Length);
    if (width == 0) return true;
    long mask = width == 4 ? -1L : -1L << (64 - width * 16);
    return (prefix4 & mask) == (checked.prefix4 & mask);
  }

  /**
   * Necessary complete-value suffix condition using only packed canonical boundary facts.
   * Exact region comparison remains authoritative.
   */
  public boolean suffixMayMatch(
      M3TextFacts suffix, int suffixUtf16Length) {
    M3TextFacts checked = Objects.requireNonNull(suffix, "suffix");
    if (suffixUtf16Length < 0) throw new IllegalArgumentException("suffixUtf16Length");
    int width = Math.min(4, suffixUtf16Length);
    if (width == 0) return true;
    long mask = width == 4 ? -1L : (1L << (width * 16)) - 1L;
    return (suffix4 & mask) == (checked.suffix4 & mask);
  }

  /** Necessary condition only; true still requires exact verification. */
  public static boolean mayContainNgrams(
      long haystackBigrams,
      long haystackTrigrams,
      CharSequence needle) {
    CharSequence checked = Objects.requireNonNull(needle, "needle");
    if (checked.length() >= 3) {
      long query = trigramSignal64(checked);
      if ((haystackTrigrams & query) != query) return false;
    }
    if (checked.length() >= 2) {
      long query = bigramSignal64(checked);
      if ((haystackBigrams & query) != query) return false;
    }
    return true;
  }

  public static boolean prefixMayMatch(long packedPrefix4, CharSequence prefix) {
    CharSequence checked = Objects.requireNonNull(prefix, "prefix");
    int width = Math.min(4, checked.length());
    if (width == 0) return true;
    long mask = width == 4 ? -1L : -1L << (64 - width * 16);
    return (packedPrefix4 & mask) == (prefix4(checked) & mask);
  }

  public static boolean suffixMayMatch(long packedSuffix4, CharSequence suffix) {
    CharSequence checked = Objects.requireNonNull(suffix, "suffix");
    int width = Math.min(4, checked.length());
    if (width == 0) return true;
    long mask = width == 4 ? -1L : (1L << (width * 16)) - 1L;
    long query = suffix4(checked) & mask;
    return (packedSuffix4 & mask) == query;
  }

  private static long pair(char first, char second) {
    return ((long) first << 16) | second;
  }

  private static long triple(char first, char second, char third) {
    long value = ((long) first << 32) | ((long) second << 16) | third;
    return mix64(value);
  }

  private static long addSignal(long signal, long value) {
    long mixed = mix64(value);
    int first = (int) mixed & 63;
    int second = (int) (mixed >>> 32) & 63;
    return signal | (1L << first) | (1L << second);
  }

  private static long mix64(long value) {
    long mixed = value;
    mixed ^= mixed >>> 30;
    mixed *= 0xbf58476d1ce4e5b9L;
    mixed ^= mixed >>> 27;
    mixed *= 0x94d049bb133111ebL;
    return mixed ^ (mixed >>> 31);
  }

  private static char asciiUpper(char value) {
    return value >= 'a' && value <= 'z' ? (char) (value - ('a' - 'A')) : value;
  }

  private static char asciiLower(char value) {
    return value >= 'A' && value <= 'Z' ? (char) (value + ('a' - 'A')) : value;
  }
}
