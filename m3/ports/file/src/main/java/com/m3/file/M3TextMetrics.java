// Copyright 2026 Hitesh Soliwal and contributors
// Full file-family receiver; source and target mapping in FILE_PORT_MAP.json.
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.nio.charset.StandardCharsets;
import java.nio.CharBuffer;
import java.util.Objects;

/**
 * Composable facts about UTF-16 text.
 *
 * <p>The Java hash and its 31-power make concatenated String-compatible hashes composable:
 * {@code hash(a + b) = hash(a) * 31^length(b) + hash(b)} with normal 32-bit overflow.
 *
 * <p>UTF-8 byte length and code-point count are also composable. A surrogate pair split across
 * two components is repaired at the component boundary, including the replacement-byte correction
 * used by the JDK UTF-8 encoder for each isolated surrogate.
 */
public record M3TextMetrics(
    int utf16Length,
    int utf8Length,
    int codePointCount,
    int unpairedSurrogateCount,
    int javaHashCode,
    int hash31Power,
    int firstUtf16Unit,
    int lastUtf16Unit) {

  private static final int UTF8_REPLACEMENT_BYTES =
      StandardCharsets.UTF_8.newEncoder().replacement().length;

  public static final M3TextMetrics EMPTY =
      new M3TextMetrics(0, 0, 0, 0, 0, 1, -1, -1);

  public M3TextMetrics {
    if (utf16Length < 0 || utf8Length < 0 || codePointCount < 0 || unpairedSurrogateCount < 0) {
      throw new IllegalArgumentException("negative text metric");
    }
    if (utf16Length == 0 && (firstUtf16Unit != -1 || lastUtf16Unit != -1)) {
      throw new IllegalArgumentException("empty text cannot expose boundary units");
    }
    if (utf16Length != 0
        && (firstUtf16Unit < Character.MIN_VALUE
            || firstUtf16Unit > Character.MAX_VALUE
            || lastUtf16Unit < Character.MIN_VALUE
            || lastUtf16Unit > Character.MAX_VALUE)) {
      throw new IllegalArgumentException("invalid UTF-16 boundary unit");
    }
  }

  public static M3TextMetrics of(CharSequence value) {
    Objects.requireNonNull(value, "value");
    int length = value.length();
    if (length == 0) {
      return EMPTY;
    }

    if (value instanceof CharBuffer buffer && buffer.isDirect()) {
      M3TextMetrics prepared = directMetrics(buffer, length);
      if (prepared != null) return prepared;
    }

    int utf8 = 0;
    int codePoints = 0;
    int unpaired = 0;
    int hash = 0;
    int power = 1;

    for (int i = 0; i < length; i++) {
      char current = value.charAt(i);
      hash = 31 * hash + current;
      power *= 31;

      if (Character.isHighSurrogate(current)) {
        if (i + 1 < length && Character.isLowSurrogate(value.charAt(i + 1))) {
          char low = value.charAt(++i);
          hash = 31 * hash + low;
          power *= 31;
          utf8 = Math.addExact(utf8, 4);
          codePoints++;
        } else {
          utf8 = Math.addExact(utf8, UTF8_REPLACEMENT_BYTES);
          codePoints++;
          unpaired++;
        }
      } else if (Character.isLowSurrogate(current)) {
        utf8 = Math.addExact(utf8, UTF8_REPLACEMENT_BYTES);
        codePoints++;
        unpaired++;
      } else {
        utf8 = Math.addExact(utf8, utf8Bytes(current));
        codePoints++;
      }
    }

    return new M3TextMetrics(
        length,
        utf8,
        codePoints,
        unpaired,
        hash,
        power,
        value.charAt(0),
        value.charAt(length - 1));
  }

  /**
   * Computes exact metrics across independent read-only or heap {@link CharBuffer} segments.
   *
   * <p>Each buffer is duplicated before reading, so positions and limits owned by the caller are
   * unchanged. A high surrogate at one segment's end remains pending until the next segment is
   * inspected; no contiguous character payload is allocated.</p>
   */
  public static M3TextMetrics ofBuffers(CharBuffer[] buffers) {
    Objects.requireNonNull(buffers, "buffers");
    int length = 0;
    int utf8 = 0;
    int codePoints = 0;
    int unpaired = 0;
    int hash = 0;
    int power = 1;
    int first = -1;
    int last = -1;
    char pendingHigh = 0;

    for (CharBuffer buffer : buffers) {
      CharBuffer cursor = Objects.requireNonNull(buffer, "buffer").duplicate();
      while (cursor.hasRemaining()) {
        char current = cursor.get();
        if (length == 0) first = current;
        last = current;
        length = Math.addExact(length, 1);
        hash = 31 * hash + current;
        power *= 31;

        if (pendingHigh != 0) {
          if (Character.isLowSurrogate(current)) {
            utf8 = Math.addExact(utf8, 4);
            codePoints++;
            pendingHigh = 0;
            continue;
          }
          utf8 = Math.addExact(utf8, UTF8_REPLACEMENT_BYTES);
          codePoints++;
          unpaired++;
          pendingHigh = 0;
        }

        if (Character.isHighSurrogate(current)) {
          pendingHigh = current;
        } else if (Character.isLowSurrogate(current)) {
          utf8 = Math.addExact(utf8, UTF8_REPLACEMENT_BYTES);
          codePoints++;
          unpaired++;
        } else {
          utf8 = Math.addExact(utf8, utf8Bytes(current));
          codePoints++;
        }
      }
    }

    if (pendingHigh != 0) {
      utf8 = Math.addExact(utf8, UTF8_REPLACEMENT_BYTES);
      codePoints++;
      unpaired++;
    }
    if (length == 0) return EMPTY;
    return new M3TextMetrics(length, utf8, codePoints, unpaired, hash, power, first, last);
  }

  public static M3TextMetrics combine(M3TextMetrics left, M3TextMetrics right) {
    Objects.requireNonNull(left, "left");
    Objects.requireNonNull(right, "right");
    if (left.utf16Length == 0) {
      return right;
    }
    if (right.utf16Length == 0) {
      return left;
    }

    boolean joinsSurrogatePair =
        Character.isHighSurrogate((char) left.lastUtf16Unit)
            && Character.isLowSurrogate((char) right.firstUtf16Unit);

    int utf8 = Math.addExact(left.utf8Length, right.utf8Length);
    int codePoints = Math.addExact(left.codePointCount, right.codePointCount);
    int unpaired = Math.addExact(left.unpairedSurrogateCount, right.unpairedSurrogateCount);
    if (joinsSurrogatePair) {
      utf8 = Math.addExact(utf8, 4 - 2 * UTF8_REPLACEMENT_BYTES);
      codePoints = Math.subtractExact(codePoints, 1);
      unpaired = Math.subtractExact(unpaired, 2);
    }

    return new M3TextMetrics(
        Math.addExact(left.utf16Length, right.utf16Length),
        utf8,
        codePoints,
        unpaired,
        left.javaHashCode * right.hash31Power + right.javaHashCode,
        left.hash31Power * right.hash31Power,
        left.firstUtf16Unit,
        right.lastUtf16Unit);
  }

  /**
   * Facts for {@code value.repeat(count)} without visiting or allocating the repeated text.
   * Composition by squaring performs O(log count) combines, repairing every surrogate seam.
   * Counts that cannot be represented by these int metrics throw ArithmeticException.
   */
  public M3TextMetrics repeat(int count) {
    if (count < 0) throw new IllegalArgumentException("negative repeat count: " + count);
    if (count == 0 || isEmpty()) return EMPTY;
    M3TextMetrics result = EMPTY;
    M3TextMetrics block = this;
    int remaining = count;
    while (remaining != 0) {
      if ((remaining & 1) != 0) result = combine(result, block);
      remaining >>>= 1;
      if (remaining != 0) block = combine(block, block);
    }
    return result;
  }

  /** Candidate lane; the existing native loader owns library loading and lifetime. */
  private static M3TextMetrics directMetrics(CharBuffer buffer, int length) {
    try {
      if (!Boolean.getBoolean("m3.file.metrics.native") || !M3FileNative.available()) return null;
      int[] values = Native.directMetrics0(buffer, buffer.position(), length,
          buffer.order() == java.nio.ByteOrder.LITTLE_ENDIAN, UTF8_REPLACEMENT_BYTES);
      if (values == null) return null; // Direct access may be unsupported on this VM.
      if (values.length != 8) throw new IllegalStateException("native text metrics ABI");
      return new M3TextMetrics(values[0], values[1], values[2], values[3],
          values[4], values[5], values[6], values[7]);
    } catch (UnsatisfiedLinkError | SecurityException unavailable) {
      return null; // No loader, stale optional native binary or forbidden property: use Java.
    }
  }

  private static final class Native {
    private Native() { }
    private static native int[] directMetrics0(
        CharBuffer buffer, int position, int length, boolean littleEndian, int replacementBytes);
  }

  public boolean isEmpty() {
    return utf16Length == 0;
  }

  public boolean isWellFormedUtf16() {
    return unpairedSurrogateCount == 0;
  }

  private static int utf8Bytes(char value) {
    if (value <= 0x7f) {
      return 1;
    }
    if (value <= 0x7ff) {
      return 2;
    }
    return 3;
  }
}
