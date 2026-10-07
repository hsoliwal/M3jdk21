// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Fixed-size composable UTF-16 geometry.
 *
 * <p>These facts own no spelling payload. Hash arithmetic intentionally follows ordinary Java
 * String hash overflow.</p>
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
            throw new IllegalArgumentException("invalid UTF-16 boundary");
        }
    }

    public static M3TextMetrics of(CharSequence value) {
        Objects.requireNonNull(value, "value");
        if (value.length() == 0) return EMPTY;

        int utf8 = 0;
        int codePoints = 0;
        int unpaired = 0;
        int hash = 0;
        int power = 1;

        for (int index = 0; index < value.length(); index++) {
            char unit = value.charAt(index);
            hash = 31 * hash + unit;
            power *= 31;
            if (Character.isHighSurrogate(unit)) {
                if (index + 1 < value.length()
                        && Character.isLowSurrogate(value.charAt(index + 1))) {
                    char low = value.charAt(++index);
                    hash = 31 * hash + low;
                    power *= 31;
                    utf8 = Math.addExact(utf8, 4);
                    codePoints++;
                } else {
                    utf8 = Math.addExact(utf8, UTF8_REPLACEMENT_BYTES);
                    codePoints++;
                    unpaired++;
                }
            } else if (Character.isLowSurrogate(unit)) {
                utf8 = Math.addExact(utf8, UTF8_REPLACEMENT_BYTES);
                codePoints++;
                unpaired++;
            } else {
                utf8 = Math.addExact(utf8, utf8Bytes(unit));
                codePoints++;
            }
        }

        return new M3TextMetrics(
                value.length(),
                utf8,
                codePoints,
                unpaired,
                hash,
                power,
                value.charAt(0),
                value.charAt(value.length() - 1));
    }

    public static M3TextMetrics combine(M3TextMetrics left, M3TextMetrics right) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        if (left.utf16Length == 0) return right;
        if (right.utf16Length == 0) return left;

        boolean seamPair =
                Character.isHighSurrogate((char) left.lastUtf16Unit)
                        && Character.isLowSurrogate((char) right.firstUtf16Unit);
        int utf8 = Math.addExact(left.utf8Length, right.utf8Length);
        int codePoints = Math.addExact(left.codePointCount, right.codePointCount);
        int unpaired = Math.addExact(left.unpairedSurrogateCount, right.unpairedSurrogateCount);
        if (seamPair) {
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

    public boolean isEmpty() {
        return utf16Length == 0;
    }

    public boolean isWellFormedUtf16() {
        return unpairedSurrogateCount == 0;
    }

    private static int utf8Bytes(char value) {
        if (value <= 0x7f) return 1;
        if (value <= 0x7ff) return 2;
        return 3;
    }
}
