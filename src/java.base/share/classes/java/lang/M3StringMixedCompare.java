/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package java.lang;

import jdk.internal.util.ArraysSupport;

/**
 * Comparisons that involve a String carrying M3 storage, folded over bulk-read windows of UTF-16
 * units instead of per-unit owner dispatch (A10 MIXED-COMPARE-WINDOWS).
 *
 * <p>Each side is read through {@link String#getChars(int, int, char[], int)}: an M3 side copies
 * from its owner in bulk ({@code M3StringOwner.getChars}), a flat side from its value array. The
 * windows are transient scratch, never a shadow (the A8 {@code M3StringFacts#scan} shape).
 *
 * <p>The case rules are the stock ones, chosen by the coders exactly as {@code String} does for two
 * flat sides: the per-unit rule of {@code StringLatin1.compareToCI}/{@code regionMatchesCI_UTF16}
 * (upper case first, then the lower case of the upper case) when either side is Latin-1, and the
 * code-point rule of {@code StringUTF16.compareToCIImpl} (surrogate pairs joined, looking one unit
 * back for a leading low surrogate) when both sides are UTF-16. Equality and ordering use the
 * vectorized {@link ArraysSupport#mismatch(char[], char[], int)}.
 */
final class M3StringMixedCompare {

    /** Units per window side; two windows per call. */
    static final int WINDOW = 256;

    /** Up to this many units the direct per-unit loop beats two window copies (bench, 8 vs 32). */
    static final int SHORT = 16;

    private M3StringMixedCompare() {
    }

    /** {@code left.compareTo(right)} by UTF-16 units: first unit difference, else length difference. */
    static int compareUnits(String left, String right) {
        int leftLength = left.length();
        int rightLength = right.length();
        int limit = Math.min(leftLength, rightLength);
        if (limit <= SHORT) {
            for (int index = 0; index < limit; index++) {
                int difference = left.charAt(index) - right.charAt(index);
                if (difference != 0) return difference;
            }
            return leftLength - rightLength;
        }
        char[] a = new char[Math.min(limit, WINDOW)];
        char[] b = new char[a.length];
        for (int base = 0; base < limit; base += a.length) {
            int count = Math.min(a.length, limit - base);
            left.getChars(base, base + count, a, 0);
            right.getChars(base, base + count, b, 0);
            int mismatch = ArraysSupport.mismatch(a, b, count);
            if (mismatch >= 0) return a[mismatch] - b[mismatch];
        }
        return leftLength - rightLength;
    }

    /** Unit equality of {@code storage} against {@code other} of the same length. */
    static boolean unitsEqual(M3String storage, String other) {
        int length = storage.length();
        if (length <= SHORT) {
            for (int index = 0; index < length; index++) {
                if (storage.charAt(index) != other.charAt(index)) return false;
            }
            return true;
        }
        char[] a = new char[Math.min(length, WINDOW)];
        char[] b = new char[a.length];
        for (int base = 0; base < length; base += a.length) {
            int count = Math.min(a.length, length - base);
            storage.getChars(base, base + count, a, 0);
            other.getChars(base, base + count, b, 0);
            if (ArraysSupport.mismatch(a, b, count) >= 0) return false;
        }
        return true;
    }

    /** {@code CASE_INSENSITIVE_ORDER.compare(left, right)} under the stock coder-dependent rule. */
    static int compareIgnoreCase(String left, String right) {
        if (left.coder() == String.LATIN1 || right.coder() == String.LATIN1) {
            return compareUnitsIgnoreCase(left, 0, left.length(), right, 0, right.length());
        }
        return compareCodePointsIgnoreCase(left, 0, left.length(), right, 0, right.length());
    }

    /** {@code left.regionMatches(true, toffset, right, ooffset, len)} with the bounds checked. */
    static boolean regionMatchesIgnoreCase(String left, int toffset, String right, int ooffset,
            int len) {
        if (left.coder() == String.LATIN1 || right.coder() == String.LATIN1) {
            return compareUnitsIgnoreCase(left, toffset, toffset + len, right, ooffset,
                    ooffset + len) == 0;
        }
        return compareCodePointsIgnoreCase(left, toffset, toffset + len, right, ooffset,
                ooffset + len) == 0;
    }

    /** The per-unit rule over windows: units equal, or their upper cases, or those lower cased. */
    private static int compareUnitsIgnoreCase(String left, int leftFrom, int leftTo, String right,
            int rightFrom, int rightTo) {
        int limit = Math.min(leftTo - leftFrom, rightTo - rightFrom);
        if (limit <= SHORT) {
            for (int index = 0; index < limit; index++) {
                char first = left.charAt(leftFrom + index);
                char second = right.charAt(rightFrom + index);
                if (first != second) {
                    int difference = unitCaseDifference(first, second);
                    if (difference != 0) return difference;
                }
            }
            return (leftTo - leftFrom) - (rightTo - rightFrom);
        }
        char[] a = new char[Math.min(limit, WINDOW)];
        char[] b = new char[a.length];
        for (int base = 0; base < limit; base += a.length) {
            int count = Math.min(a.length, limit - base);
            left.getChars(leftFrom + base, leftFrom + base + count, a, 0);
            right.getChars(rightFrom + base, rightFrom + base + count, b, 0);
            for (int index = 0; index < count; index++) {
                if (a[index] != b[index]) {
                    int difference = unitCaseDifference(a[index], b[index]);
                    if (difference != 0) return difference;
                }
            }
        }
        return (leftTo - leftFrom) - (rightTo - rightFrom);
    }

    /** {@code StringLatin1.compareToCI} rule for one unit pair: {@code 0} when they match. */
    static int unitCaseDifference(char first, char second) {
        char upperFirst = Character.toUpperCase(first);
        char upperSecond = Character.toUpperCase(second);
        if (upperFirst == upperSecond) return 0;
        char lowerFirst = Character.toLowerCase(upperFirst);
        char lowerSecond = Character.toLowerCase(upperSecond);
        return lowerFirst - lowerSecond;
    }

    /** {@code StringUTF16.compareToCIImpl} over two cursors: code points joined across surrogates. */
    private static int compareCodePointsIgnoreCase(String left, int leftFrom, int leftTo,
            String right, int rightFrom, int rightTo) {
        Cursor first = new Cursor(left, leftFrom, leftTo);
        Cursor second = new Cursor(right, rightFrom, rightTo);
        for (int k1 = leftFrom, k2 = rightFrom; k1 < leftTo && k2 < rightTo; k1++, k2++) {
            int cp1 = first.at(k1);
            int cp2 = second.at(k2);
            if (cp1 == cp2 || codePointCaseDifference(cp1, cp2) == 0) continue;
            cp1 = first.codePointIncluding(cp1, k1);
            if (cp1 < 0) {
                k1++;
                cp1 = -cp1;
            }
            cp2 = second.codePointIncluding(cp2, k2);
            if (cp2 < 0) {
                k2++;
                cp2 = -cp2;
            }
            int difference = codePointCaseDifference(cp1, cp2);
            if (difference != 0) return difference;
        }
        return (leftTo - leftFrom) - (rightTo - rightFrom);
    }

    /** {@code StringUTF16.compareCodePointCI}: upper case first, then lower case of the upper case. */
    static int codePointCaseDifference(int first, int second) {
        int upperFirst = Character.toUpperCase(first);
        int upperSecond = Character.toUpperCase(second);
        if (upperFirst == upperSecond) return 0;
        int lowerFirst = Character.toLowerCase(upperFirst);
        int lowerSecond = Character.toLowerCase(upperSecond);
        return lowerFirst - lowerSecond;
    }

    /** One side of a code-point comparison: a window over {@code [from, to)} refilled on demand. */
    private static final class Cursor {
        private final String source;
        private final int from;
        private final int to;
        private final char[] window;
        private int base;
        private int count;

        Cursor(String source, int from, int to) {
            this.source = source;
            this.from = from;
            this.to = to;
            this.window = new char[Math.min(to - from, WINDOW)];
            this.base = from;
            this.count = 0;
        }

        char at(int index) {
            if (index < base || index >= base + count) fill(index);
            return window[index - base];
        }

        private void fill(int index) {
            base = index;
            count = Math.min(window.length, to - index);
            source.getChars(base, base + count, window, 0);
        }

        /** {@code StringUTF16.codePointIncluding}: negative for a pair that consumes the next unit. */
        int codePointIncluding(int unit, int index) {
            if (!Character.isSurrogate((char) unit)) return unit;
            if (Character.isLowSurrogate((char) unit)) {
                if (index > from) {
                    char before = index - 1 >= base ? window[index - 1 - base] : source.charAt(index - 1);
                    if (Character.isHighSurrogate(before)) return Character.toCodePoint(before, (char) unit);
                }
            } else if (index + 1 < to) {
                char after = at(index + 1);
                if (Character.isLowSurrogate(after)) return -Character.toCodePoint((char) unit, after);
            }
            return unit;
        }
    }
}
