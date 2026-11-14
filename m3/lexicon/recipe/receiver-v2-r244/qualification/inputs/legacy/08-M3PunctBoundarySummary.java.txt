// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.lexicon;

/**
 * Punctuation separator monoid: bit j is set when unit j belongs to a code point whose
 * {@code Character.getType} is in the configured class mask (both units of a surrogate pair). The only
 * seam work is a surrogate pair split across two atoms: a trailing high surrogate and a leading low
 * surrogate stay undecided until their neighbour is known; a lone surrogate is never punctuation.
 */
public final class M3PunctBoundarySummary {
    private final int classes;
    private final int length;
    private final char first;
    private final char last;
    private final long[] bits;

    private M3PunctBoundarySummary(int classes, int length, char first, char last, long[] bits) {
        this.classes = classes;
        this.length = length;
        this.first = first;
        this.last = last;
        this.bits = bits;
    }

    /** Summarizes one atom for a class mask such as {@link M3SeparatorKind#PUNCTUATION_CLASSES}. */
    public static M3PunctBoundarySummary of(int classes, CharSequence atom) {
        int n = atom.length();
        long[] bits = M3BitLanes.lanes(n);
        int index = 0;
        while (index < n) {
            int codePoint = Character.codePointAt(atom, index);
            int width = Character.charCount(codePoint);
            if (index + width <= n && member(classes, codePoint)) {
                M3BitLanes.set(bits, index);
                if (width == 2) {
                    M3BitLanes.set(bits, index + 1);
                }
            }
            index += width;
        }
        char first = n == 0 ? 0 : atom.charAt(0);
        char last = n == 0 ? 0 : atom.charAt(n - 1);
        return new M3PunctBoundarySummary(classes, n, first, last, bits);
    }

    /** Monoid product: {@code L ++ R}. */
    public M3PunctBoundarySummary combine(M3PunctBoundarySummary right) {
        if (right.classes != classes) {
            throw new IllegalArgumentException("punctuation class mask mismatch");
        }
        if (length == 0 || right.length == 0) {
            return length == 0 ? right : this;
        }
        int n = length + right.length;
        long[] merged = M3BitLanes.copyOf(bits, n);
        M3BitLanes.orShifted(merged, right.bits, right.length, length);
        if (Character.isHighSurrogate(last) && Character.isLowSurrogate(right.first)
                && member(classes, Character.toCodePoint(last, right.first))) {
            M3BitLanes.set(merged, length - 1);
            M3BitLanes.set(merged, length);
        }
        char newLast = right.length == 0 ? last : right.last;
        return new M3PunctBoundarySummary(classes, n, first, newLast, merged);
    }

    /** @return separator-unit bits of the summarized text */
    public long[] finish() {
        return M3BitLanes.copyOf(bits, length);
    }

    /** @return number of UTF-16 units summarized */
    public int length() {
        return length;
    }

    static boolean member(int classes, int codePoint) {
        return (classes & (1 << Character.getType(codePoint))) != 0;
    }
}
