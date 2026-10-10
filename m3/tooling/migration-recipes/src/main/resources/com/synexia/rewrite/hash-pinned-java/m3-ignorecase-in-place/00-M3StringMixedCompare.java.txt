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

import java.util.Arrays;

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
 * flat sides: the {@code StringLatin1.compareToCI}/{@code regionMatchesCI} family when either side
 * is Latin-1 and the code-point rule of {@code StringUTF16.compareToCIImpl} when both sides are
 * UTF-16. Since A28 the stock helpers themselves run over bulk windows in each side's own coder
 * (graded through {@link M3String#windowUnits}; two UTF-16 sides keep surrogate pairs inside one
 * window), so a differing unit pair costs what it costs a flat String and an early difference
 * costs one small read (A33). Equality and ordering use the vectorized
 * {@link ArraysSupport#mismatch(char[], char[], int)}.
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
        int decided = mismatchInPlace(left, 0, right, 0, limit);
        if (decided != UNDECIDED) {
            return decided < 0 ? leftLength - rightLength : left.charAt(decided) - right.charAt(decided);
        }
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
        M3String otherStorage = other.m3();
        if (otherStorage == null) {
            return storage.mismatchUnits(0, other.value(), 0, other.coder(), length) < 0;
        }
        return mismatchStorages(storage, 0, otherStorage, 0, length) < 0;
    }

    /** Sentinel of {@link #mismatchInPlace}: neither side is flat, fall back to windows. */
    private static final int UNDECIDED = Integer.MIN_VALUE;

    /**
     * Mismatch index of {@code left[leftFrom, leftFrom + count)} against
     * {@code right[rightFrom, rightFrom + count)} when exactly one side is flat: the M3 side
     * compares its units in place against the flat compact value (no copy); {@code -1} when the
     * regions agree, {@link #UNDECIDED} when both sides carry storage or both are flat.
     */
    private static int mismatchInPlace(String left, int leftFrom, String right, int rightFrom, int count) {
        M3String leftStorage = left.m3();
        M3String rightStorage = right.m3();
        if (leftStorage != null && rightStorage == null) {
            return leftStorage.mismatchUnits(leftFrom, right.value(), rightFrom, right.coder(), count);
        }
        if (leftStorage == null && rightStorage != null) {
            return rightStorage.mismatchUnits(rightFrom, left.value(), leftFrom, left.coder(), count);
        }
        if (leftStorage != null) {
            return mismatchStorages(leftStorage, leftFrom, rightStorage, rightFrom, count);
        }
        return UNDECIDED;
    }

    /**
     * Mismatch index of two M3 ranges: both sides are walked leaf atom by leaf atom (a tuple's
     * halves descend), and each pair of leaf segments is compared native-to-native through
     * {@link M3StringAtom#mismatchAtom}; {@code -1} when the ranges agree (A12).
     */
    static int mismatchStorages(M3String left, int leftFrom, M3String right, int rightFrom, int count) {
        Leaf a = new Leaf();
        Leaf b = new Leaf();
        for (int done = 0; done < count;) {
            a.locate(left, leftFrom + done);
            b.locate(right, rightFrom + done);
            int chunk = Math.min(count - done, Math.min(a.remaining, b.remaining));
            int index = a.atom.mismatchAtom(a.offset, b.atom, b.offset, chunk);
            if (index >= 0) return done + index;
            done += chunk;
        }
        return -1;
    }

    /** The leaf atom covering one position of an M3 range, with the units left in that atom. */
    private static final class Leaf {
        M3StringAtom atom;
        int offset;
        int remaining;

        void locate(M3String range, int index) {
            M3StringOwner owner = range.owner();
            int at = range.start() + index;
            while (owner.kind == M3StringOwner.TUPLE) {
                M3StringTuple tuple = (M3StringTuple) owner;
                int leftLength = tuple.left.length();
                M3String half = at < leftLength ? tuple.left : tuple.right;
                if (at >= leftLength) at -= leftLength;
                at += half.start();
                owner = half.owner();
            }
            atom = (M3StringAtom) owner;
            offset = at;
            remaining = owner.length - at;
        }
    }

    /** {@code left.regionMatches(toffset, right, ooffset, len)} with the bounds checked. */
    static boolean regionMatchesUnits(String left, int toffset, String right, int ooffset, int len) {
        int index = mismatchInPlace(left, toffset, right, ooffset, len);
        if (index != UNDECIDED) return index < 0;
        if (len <= SHORT) {
            for (int offset = 0; offset < len; offset++) {
                if (left.charAt(toffset + offset) != right.charAt(ooffset + offset)) return false;
            }
            return true;
        }
        char[] a = new char[Math.min(len, WINDOW)];
        char[] b = new char[a.length];
        for (int base = 0; base < len; base += a.length) {
            int count = Math.min(a.length, len - base);
            left.getChars(toffset + base, toffset + base + count, a, 0);
            right.getChars(ooffset + base, ooffset + base + count, b, 0);
            if (ArraysSupport.mismatch(a, b, count) >= 0) return false;
        }
        return true;
    }

    /**
     * {@code CASE_INSENSITIVE_ORDER.compare(left, right)}: the stock {@code regionMatchesCI} family
     * over bulk windows of both sides in their own coders (A28), a flat side read in place and an
     * M3 side through one transient window per side reused across the scan (A41; before, both
     * sides were copied per window); two sides of one coder skip their equal prefix through the
     * mismatch intrinsic first. Equal windows continue; the first window that differs is handed,
     * as exact-length copies, to the stock {@code compareToCI} family, whose word is the stock
     * one; the last word is the length difference, as stock. Two UTF-16 sides cut their windows
     * where neither side has a surrogate pair across the cut ({@link #unsplitCut}) and take their
     * difference from windows that reach one unit past the shorter side, as the stock code-point
     * rule does.
     */
    static int compareIgnoreCase(String left, String right) {
        int leftLength = left.length();
        int rightLength = right.length();
        int limit = Math.min(leftLength, rightLength);
        byte leftCoder = contentCoder(left);
        byte rightCoder = contentCoder(right);
        boolean codePoints = leftCoder == String.UTF16 && rightCoder == String.UTF16;
        M3String leftM3 = left.m3();
        M3String rightM3 = right.m3();
        byte[] a = leftM3 == null ? left.value() : null;
        byte[] b = rightM3 == null ? right.value() : null;
        for (int base = 0; base < limit; ) {
            int remaining = limit - base;
            int count = M3String.windowUnits(base, remaining);
            int fetch = codePoints && count < remaining ? count + 1 : count;
            int aOffset = base;
            int bOffset = base;
            if (leftM3 != null) {
                a = fill(leftM3, a, base, leftCoder, fetch);
                aOffset = 0;
            }
            if (rightM3 != null) {
                b = fill(rightM3, b, base, rightCoder, fetch);
                bOffset = 0;
            }
            if (fetch > count) {
                count = unsplitCut(a, aOffset, b, bOffset, count);
                if (count == 0) {
                    count = remaining;
                    if (leftM3 != null) a = fill(leftM3, a, base, leftCoder, count);
                    if (rightM3 != null) b = fill(rightM3, b, base, rightCoder, count);
                }
            }
            if (!windowMatchesIgnoreCase(a, aOffset, leftCoder, b, bOffset, rightCoder, count)) {
                int difference = windowDifferenceIgnoreCase(left, right, base, count, leftCoder, rightCoder, codePoints);
                if (difference != 0) return difference;
            }
            base += count;
        }
        return leftLength - rightLength;
    }

    /**
     * {@code left.regionMatches(true, toffset, right, ooffset, len)} with the bounds checked: the
     * stock {@code regionMatchesCI} family over bulk windows (A28); a flat side is read in place,
     * an M3 side through one transient window reused across the scan in its own coder; two sides
     * of one coder skip their equal prefix through the mismatch intrinsic first (A41). Two UTF-16
     * sides cut their windows where neither side has a surrogate pair across the cut
     * ({@link #unsplitCut}), so the stock code-point rule sees every pair whole.
     */
    static boolean regionMatchesIgnoreCase(String left, int toffset, String right, int ooffset,
            int len) {
        byte leftCoder = contentCoder(left);
        byte rightCoder = contentCoder(right);
        boolean codePoints = leftCoder == String.UTF16 && rightCoder == String.UTF16;
        M3String leftM3 = left.m3();
        M3String rightM3 = right.m3();
        byte[] a = leftM3 == null ? left.value() : null;
        byte[] b = rightM3 == null ? right.value() : null;
        for (int base = 0; base < len; ) {
            int remaining = len - base;
            int count = M3String.windowUnits(base, remaining);
            int fetch = codePoints && count < remaining ? count + 1 : count;
            int aOffset = toffset + base;
            int bOffset = ooffset + base;
            if (leftM3 != null) {
                a = fill(leftM3, a, aOffset, leftCoder, fetch);
                aOffset = 0;
            }
            if (rightM3 != null) {
                b = fill(rightM3, b, bOffset, rightCoder, fetch);
                bOffset = 0;
            }
            if (fetch > count) {
                count = unsplitCut(a, aOffset, b, bOffset, count);
                if (count == 0) {
                    count = remaining;
                    if (leftM3 != null) a = fill(leftM3, a, toffset + base, leftCoder, count);
                    if (rightM3 != null) b = fill(rightM3, b, ooffset + base, rightCoder, count);
                }
            }
            if (!windowMatchesIgnoreCase(a, aOffset, leftCoder, b, bOffset, rightCoder, count)) return false;
            base += count;
        }
        return true;
    }

    /**
     * {@code units} units of {@code storage} from {@code from} in {@code coder} at the start of
     * {@code buffer}, or of a fresh array when {@code buffer} is absent or too short (A41): the
     * transient window of an M3 side, reused across a scan.
     */
    private static byte[] fill(M3String storage, byte[] buffer, int from, byte coder, int units) {
        byte[] out = buffer != null && buffer.length >= units << coder ? buffer : new byte[units << coder];
        storage.getBytes(out, from, 0, coder, units);
        return out;
    }

    /**
     * The cut of a window of {@code count} units of two UTF-16 sides, the unit after the window
     * fetched on both (A41): moved back while a high surrogate before it and a low surrogate after
     * it on either side would fold as two lone units what the stock rule folds as one pair (A28
     * moved it back once, which could split the other side's pair); 0 when every cut of the window
     * splits a pair, surrogate pairs misaligned by one unit on the two sides, so the caller takes
     * the rest of the range as one window.
     */
    private static int unsplitCut(byte[] a, int aOffset, byte[] b, int bOffset, int count) {
        int cut = count;
        while (cut > 0 && (splitsPair(a, aOffset + cut) || splitsPair(b, bOffset + cut))) cut--;
        return cut;
    }

    private static boolean splitsPair(byte[] utf16, int index) {
        return Character.isHighSurrogate(StringUTF16.getChar(utf16, index - 1))
                && Character.isLowSurrogate(StringUTF16.getChar(utf16, index));
    }

    /**
     * The stock {@code regionMatchesCI} rule over one window of {@code count} units, each side in
     * its own coder (A41). Two sides of one coder skip their equal prefix through the mismatch
     * intrinsic and fold from the first differing unit on, which the stock rule folds the same
     * way: a Latin-1 unit folds on its own; a low surrogate pairs with the high surrogate before
     * it, equal on both sides, so the fold starts one unit earlier there.
     */
    private static boolean windowMatchesIgnoreCase(byte[] a, int aOffset, byte aCoder,
            byte[] b, int bOffset, byte bCoder, int count) {
        if (aCoder != bCoder) {
            return aCoder == String.LATIN1
                    ? StringLatin1.regionMatchesCI_UTF16(a, aOffset, b, bOffset, count)
                    : StringUTF16.regionMatchesCI_Latin1(a, aOffset, b, bOffset, count);
        }
        int mismatch = ArraysSupport.mismatch(a, aOffset << aCoder, b, bOffset << aCoder, count << aCoder);
        if (mismatch < 0) return true;
        if (aCoder == String.LATIN1) {
            return StringLatin1.regionMatchesCI(a, aOffset + mismatch, b, bOffset + mismatch, count - mismatch);
        }
        int unit = mismatch >> String.UTF16;
        if (unit > 0 && Character.isHighSurrogate(StringUTF16.getChar(a, aOffset + unit - 1))) unit--;
        return StringUTF16.regionMatchesCI(a, aOffset + unit, b, bOffset + unit, count - unit);
    }

    /**
     * The stock {@code compareToCI} word for the window at {@code base} that differs (A41):
     * exact-length copies of both sides in their own coders; two UTF-16 sides one unit longer when
     * they have one, so a surrogate pair across the end of the shorter side folds as the stock
     * code-point rule folds it.
     */
    private static int windowDifferenceIgnoreCase(String left, String right, int base, int count,
            byte leftCoder, byte rightCoder, boolean codePoints) {
        if (codePoints) {
            byte[] leftTail = window(left, base, Math.min(count + 1, left.length() - base), leftCoder);
            byte[] rightTail = window(right, base, Math.min(count + 1, right.length() - base), rightCoder);
            return StringUTF16.compareToCI(leftTail, rightTail);
        }
        byte[] a = window(left, base, count, leftCoder);
        byte[] b = window(right, base, count, rightCoder);
        return leftCoder == rightCoder
                ? StringLatin1.compareToCI(a, b)
                : (leftCoder == String.LATIN1 ? StringLatin1.compareToCI_UTF16(a, b) : StringUTF16.compareToCI_Latin1(a, b));
    }

    /**
     * The coder a flat String of the same spelling carries (A28): an M3 range inside a wide owner
     * whose units all fit Latin-1 compares under the Latin-1 rules, as its flat twin does; prepared
     * facts answer at once, otherwise a bulk scan with early exit decides.
     */
    private static byte contentCoder(String side) {
        M3String storage = side.m3();
        if (storage == null || storage.coder() == String.LATIN1) return side.coder();
        M3StringFacts prepared = storage.factsIfPrepared();
        if (prepared != null) return prepared.latin1 ? String.LATIN1 : String.UTF16;
        return storage.contentIsLatin1() ? String.LATIN1 : String.UTF16;
    }

    /**
     * {@code count} units of {@code source} from {@code from} as a fresh array in {@code coder}:
     * the exact-length copies the stock {@code compareToCI} family reads (A41: only the window
     * that differs is copied).
     */
    private static byte[] window(String source, int from, int count, byte coder) {
        M3String storage = source.m3();
        if (storage == null) {
            return Arrays.copyOfRange(source.value(), from << coder, (from + count) << coder);
        }
        byte[] out = new byte[count << coder];
        storage.getBytes(out, from, 0, coder, count);
        return out;
    }
}
