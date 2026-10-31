// SPDX-License-Identifier: Apache-2.0
package com.m3.arrays;

import java.util.Objects;

/**
 * Exact composable facts over an immutable UTF-16 view.
 *
 * <p>The hash is the Java String polynomial hash. Combining facts is exact, including a surrogate
 * pair split across a segment seam.</p>
 */
public record M3Utf16Facts(
        int length,
        int javaHashCode,
        int hashPower31,
        int codePointCount,
        boolean ascii,
        boolean latin1,
        char first,
        char last) {

    public M3Utf16Facts {
        if (length < 0 || codePointCount < 0 || codePointCount > length) {
            throw new IllegalArgumentException("invalid UTF-16 facts");
        }
    }

    public boolean empty() {
        return length == 0;
    }

    public static M3Utf16Facts of(CharSequence value) {
        Objects.requireNonNull(value, "value");
        int hash = 0;
        int power = 1;
        int codePoints = 0;
        boolean ascii = true;
        boolean latin1 = true;
        char first = 0;
        char last = 0;
        for (int index = 0; index < value.length(); index++) {
            char c = value.charAt(index);
            if (index == 0) first = c;
            last = c;
            hash = 31 * hash + c;
            power *= 31;
            ascii &= c < 128;
            latin1 &= c <= 255;
            if (!Character.isLowSurrogate(c)
                    || index == 0
                    || !Character.isHighSurrogate(value.charAt(index - 1))) {
                codePoints++;
            }
        }
        return new M3Utf16Facts(
                value.length(), hash, power, codePoints, ascii, latin1, first, last);
    }

    public static M3Utf16Facts combine(M3Utf16Facts left, M3Utf16Facts right) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        if (left.empty()) return right;
        if (right.empty()) return left;
        int length = Math.addExact(left.length, right.length);
        int points = Math.addExact(left.codePointCount, right.codePointCount);
        if (Character.isHighSurrogate(left.last) && Character.isLowSurrogate(right.first)) {
            points--;
        }
        return new M3Utf16Facts(
                length,
                left.javaHashCode * right.hashPower31 + right.javaHashCode,
                left.hashPower31 * right.hashPower31,
                points,
                left.ascii && right.ascii,
                left.latin1 && right.latin1,
                left.first,
                right.last);
    }
}
