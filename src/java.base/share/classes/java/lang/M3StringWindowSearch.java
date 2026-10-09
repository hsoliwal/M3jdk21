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

/**
 * Needle searches over an M3 haystack with the stock {@code StringLatin1}/{@code StringUTF16}
 * searches on bulk-copied windows (A14 INDEXOF-NATIVE). A window of {@link #WINDOW} units is
 * filled through {@link M3String#getBytes} (an atom copies from native memory, a tuple descends)
 * and consecutive windows overlap by needle length minus one, so a match straddling two windows
 * is still found; {@code indexOf} walks forward, {@code lastIndexOf} backward. Every facts and
 * plan gate stays in front of these searches, and the results equal the stock {@code String}
 * results on the same spellings.
 */
final class M3StringWindowSearch {

    /** Units per window; one window buffer per call, transient scratch. */
    static final int WINDOW = 4096;

    /**
     * An M3 needle of this many units and up runs the search precompute's skip search over each
     * window (sublinear for long needles); shorter needles run the stock vectorized search.
     */
    static final int LONG_NEEDLE = 16;

    private M3StringWindowSearch() {
    }

    /** The compact value of an M3 needle, read once in bulk. */
    static byte[] needleBytes(M3String needle) {
        byte coder = needle.coder();
        int count = needle.length();
        byte[] out = new byte[count << coder];
        needle.getBytes(out, 0, 0, coder, count);
        return out;
    }

    /**
     * First index in {@code [from, end)} at which the needle starts, else {@code -1}.
     * {@code needleCount} is at least 1 and at most {@code end - from}.
     */
    static int indexOf(M3String haystack, int from, int end, byte[] needle, int needleCount,
            byte needleCoder) {
        byte coder = haystack.coder();
        if (coder == String.LATIN1 && needleCoder == String.UTF16) return -1;
        int width = Math.min(end - from, Math.max(WINDOW, needleCount));
        byte[] buffer = new byte[width << coder];
        for (int base = from; base + needleCount <= end;) {
            int count = Math.min(width, end - base);
            haystack.getBytes(buffer, base, 0, coder, count);
            int found = search(buffer, count, coder, needle, needleCount, needleCoder);
            if (found >= 0) return base + found;
            if (base + count >= end) return -1;
            base += count - (needleCount - 1);
        }
        return -1;
    }

    /**
     * Last index at most {@code maximumStart} at which the needle starts, else {@code -1}.
     * {@code maximumStart + needleCount} is at most the haystack length.
     */
    static int lastIndexOf(M3String haystack, int maximumStart, byte[] needle, int needleCount,
            byte needleCoder) {
        byte coder = haystack.coder();
        if (coder == String.LATIN1 && needleCoder == String.UTF16) return -1;
        int end = maximumStart + needleCount;
        int width = Math.min(end, Math.max(WINDOW, needleCount));
        byte[] buffer = new byte[width << coder];
        for (int limit = end; limit >= needleCount;) {
            int base = Math.max(0, limit - width);
            int count = limit - base;
            haystack.getBytes(buffer, base, 0, coder, count);
            int found = searchLast(buffer, count, coder, needle, needleCount, needleCoder,
                    count - needleCount);
            if (found >= 0) return base + found;
            if (base == 0) return -1;
            limit = base + (needleCount - 1);
        }
        return -1;
    }

    /** First index in {@code [from, end)} at which the M3 needle starts, else {@code -1}. */
    static int indexOf(M3String haystack, int from, int end, M3String needle) {
        int needleCount = needle.length();
        if (needleCount < LONG_NEEDLE) {
            return indexOf(haystack, from, end, needleBytes(needle), needleCount, needle.coder());
        }
        byte coder = haystack.coder();
        if (coder == String.LATIN1 && needle.coder() == String.UTF16) return -1;
        int width = Math.min(end - from, Math.max(WINDOW, needleCount));
        byte[] buffer = new byte[width << coder];
        for (int base = from; base + needleCount <= end;) {
            int count = Math.min(width, end - base);
            haystack.getBytes(buffer, base, 0, coder, count);
            int found = M3StringSearchPrecompute.indexOf(buffer, coder, count, needle, 0);
            if (found >= 0) return base + found;
            if (base + count >= end) return -1;
            base += count - (needleCount - 1);
        }
        return -1;
    }

    /** Last index at most {@code maximumStart} at which the M3 needle starts, else {@code -1}. */
    static int lastIndexOf(M3String haystack, int maximumStart, M3String needle) {
        int needleCount = needle.length();
        if (needleCount < LONG_NEEDLE) {
            return lastIndexOf(haystack, maximumStart, needleBytes(needle), needleCount, needle.coder());
        }
        byte coder = haystack.coder();
        if (coder == String.LATIN1 && needle.coder() == String.UTF16) return -1;
        int end = maximumStart + needleCount;
        int width = Math.min(end, Math.max(WINDOW, needleCount));
        byte[] buffer = new byte[width << coder];
        for (int limit = end; limit >= needleCount;) {
            int base = Math.max(0, limit - width);
            int count = limit - base;
            haystack.getBytes(buffer, base, 0, coder, count);
            int found = M3StringSearchPrecompute.lastIndexOf(buffer, coder, count, needle,
                    count - needleCount);
            if (found >= 0) return base + found;
            if (base == 0) return -1;
            limit = base + (needleCount - 1);
        }
        return -1;
    }

    private static int search(byte[] window, int count, byte coder, byte[] needle, int needleCount,
            byte needleCoder) {
        if (coder == String.LATIN1) {
            return StringLatin1.indexOf(window, count, needle, needleCount, 0);
        }
        return needleCoder == String.LATIN1
                ? StringUTF16.indexOfLatin1(window, count, needle, needleCount, 0)
                : StringUTF16.indexOf(window, count, needle, needleCount, 0);
    }

    private static int searchLast(byte[] window, int count, byte coder, byte[] needle,
            int needleCount, byte needleCoder, int fromIndex) {
        if (coder == String.LATIN1) {
            return StringLatin1.lastIndexOf(window, count, needle, needleCount, fromIndex);
        }
        return needleCoder == String.LATIN1
                ? StringUTF16.lastIndexOfLatin1(window, count, needle, needleCount, fromIndex)
                : StringUTF16.lastIndexOf(window, count, needle, needleCount, fromIndex);
    }
}
