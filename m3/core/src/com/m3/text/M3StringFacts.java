/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.text;

import java.util.Objects;

/**
 * Immutable, composable UTF-16 facts for an M3 text projection.
 *
 * <p>This is the M3JDK target for Synexia's proven {@code TextFacts}
 * projection. It consumes a {@link CharSequence} through {@code length} and
 * {@code charAt}; it never calls {@code toString()} and does not make text
 * materialization part of precomputation.</p>
 */
public record M3StringFacts(
        int utf16Length,
        int codePointCount,
        int unpairedSurrogateCount,
        int nonBmpCodePointCount,
        int javaHashCode,
        boolean ascii,
        boolean latin1,
        boolean containsWhitespace) {

    public M3StringFacts {
        if (utf16Length < 0 || codePointCount < 0
                || unpairedSurrogateCount < 0 || nonBmpCodePointCount < 0
                || codePointCount > utf16Length
                || unpairedSurrogateCount > utf16Length
                || nonBmpCodePointCount > codePointCount) {
            throw new IllegalArgumentException("invalid M3 string facts");
        }
    }

    /**
     * Precompute facts from UTF-16 code units without crossing a materialization
     * boundary.
     */
    public static M3StringFacts of(CharSequence value) {
        Objects.requireNonNull(value, "value");
        int hash = 0;
        int codePoints = 0;
        int unpaired = 0;
        int nonBmp = 0;
        boolean ascii = true;
        boolean latin1 = true;
        boolean whitespace = false;
        char pendingHigh = 0;

        for (int index = 0; index < value.length(); index++) {
            char unit = value.charAt(index);
            hash = 31 * hash + unit;
            ascii &= unit <= 0x7f;
            latin1 &= unit <= 0xff;

            if (pendingHigh != 0) {
                if (Character.isLowSurrogate(unit)) {
                    int codePoint = Character.toCodePoint(pendingHigh, unit);
                    codePoints++;
                    nonBmp++;
                    whitespace |= Character.isWhitespace(codePoint);
                    pendingHigh = 0;
                    continue;
                }
                codePoints++;
                unpaired++;
                whitespace |= Character.isWhitespace(pendingHigh);
                pendingHigh = 0;
            }

            if (Character.isHighSurrogate(unit)) {
                pendingHigh = unit;
            } else {
                codePoints++;
                if (Character.isLowSurrogate(unit)) {
                    unpaired++;
                }
                whitespace |= Character.isWhitespace(unit);
            }
        }

        if (pendingHigh != 0) {
            codePoints++;
            unpaired++;
            whitespace |= Character.isWhitespace(pendingHigh);
        }
        return new M3StringFacts(value.length(), codePoints, unpaired, nonBmp,
                hash, ascii, latin1, whitespace);
    }

    /**
     * Compose two warmed records without rescanning either operand.
     *
     * @param leftLastUnit last UTF-16 unit of the left operand, or any value
     *                     when the left operand is empty
     * @param rightFirstUnit first UTF-16 unit of the right operand, or any
     *                      value when the right operand is empty
     */
    public static M3StringFacts compose(
            M3StringFacts left,
            M3StringFacts right,
            char leftLastUnit,
            char rightFirstUnit) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        boolean surrogateSeam = left.utf16Length > 0
                && right.utf16Length > 0
                && Character.isHighSurrogate(leftLastUnit)
                && Character.isLowSurrogate(rightFirstUnit);

        int codePoints = Math.addExact(left.codePointCount, right.codePointCount);
        int unpaired = Math.addExact(
                left.unpairedSurrogateCount, right.unpairedSurrogateCount);
        int nonBmp = Math.addExact(
                left.nonBmpCodePointCount, right.nonBmpCodePointCount);
        boolean whitespace = left.containsWhitespace || right.containsWhitespace;
        if (surrogateSeam) {
            codePoints--;
            unpaired = Math.subtractExact(unpaired, 2);
            nonBmp++;
            whitespace |= Character.isWhitespace(
                    Character.toCodePoint(leftLastUnit, rightFirstUnit));
        }
        return new M3StringFacts(
                Math.addExact(left.utf16Length, right.utf16Length),
                codePoints,
                unpaired,
                nonBmp,
                left.javaHashCode * javaHashMultiplier(right.utf16Length)
                        + right.javaHashCode,
                left.ascii && right.ascii,
                left.latin1 && right.latin1,
                whitespace);
    }

    private static int javaHashMultiplier(int length) {
        int result = 1;
        int base = 31;
        int remaining = length;
        while (remaining != 0) {
            if ((remaining & 1) != 0) {
                result *= base;
            }
            base *= base;
            remaining >>>= 1;
        }
        return result;
    }
}
