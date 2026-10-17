// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.Objects;

/**
 * Numeric mechanics over stable nonnegative count spans, not another storage/index owner.
 * Java is the default. -Dm3.collections.counts.native=true explicitly requires the native
 * library; a failed load is not disguised as a native success. No projection, retained
 * array/pointer, prefix cache, entry, thread or domain admission is owned by this class.
 * Inputs must not change during a call. The complete selected span is validated, including
 * entries after a located occurrence. Length * maximum int count fits a signed long.
 */
final class M3IndexCountKernel {
    private static final boolean NATIVE = Boolean.getBoolean("m3.collections.counts.native");
    static { if (NATIVE) System.loadLibrary("m3_index_counts"); }
    private M3IndexCountKernel() { }

    static boolean nativeEnabled() { return NATIVE; }

    static long sum(int[] counts, int from, int to) {
        bounds(counts, from, to);
        if (NATIVE) return sum0(counts, from, to);
        long total = 0;
        for (int i = from; i < to; i++) { requireCount(counts[i]); total += counts[i]; }
        return total;
    }

    /** High 32 bits: physical row; low 32 bits: intra-row offset in encounter order. */
    static long locate(int[] counts, int from, int to, long ordinal, boolean descending) {
        bounds(counts, from, to);
        if (ordinal < 0) throw new IndexOutOfBoundsException("negative occurrence");
        if (NATIVE) return locate0(counts, from, to, ordinal, descending);
        long remaining = ordinal;
        long cursor = -1;
        for (int i = descending ? to - 1 : from;
                i >= from && i < to; i += descending ? -1 : 1) {
            int count = counts[i]; requireCount(count);
            if (cursor < 0) {
                if (remaining < count) cursor = ((long) i << 32) | remaining;
                else remaining -= count;
            }
        }
        if (cursor < 0) throw new IndexOutOfBoundsException("occurrence outside count span");
        return cursor;
    }

    private static void bounds(int[] counts, int from, int to) {
        Objects.requireNonNull(counts, "counts");
        Objects.checkFromToIndex(from, to, counts.length);
    }
    private static void requireCount(int count) {
        if (count < 0) throw new IllegalArgumentException("negative count");
    }
    private static native long sum0(int[] counts, int from, int to);
    private static native long locate0(int[] counts, int from, int to, long ordinal, boolean descending);

    /** Synexia M3RowSelectionKernel atom: exact mask extent, stable ascending row positions. */
    static int[] selectedRows(long[] words, int logicalRows) {
        Objects.requireNonNull(words, "words");
        if (logicalRows < 0) throw new IllegalArgumentException("logicalRows must be nonnegative");
        int required = (logicalRows >>> 6) + ((logicalRows & 63) == 0 ? 0 : 1);
        if (words.length != required) throw new IllegalArgumentException("mask word count mismatch");
        if (NATIVE) return selectedRows0(words, logicalRows);
        int count = 0;
        for (int i = 0; i < words.length; i++) count += Long.bitCount(maskedWord(words, logicalRows, i));
        int[] positions = new int[count]; int next = 0;
        for (int i = 0; i < words.length; i++) {
            long bits = maskedWord(words, logicalRows, i);
            while (bits != 0L) {
                if (next == count) throw new IllegalStateException("mask changed during selection");
                positions[next++] = (i << 6) + Long.numberOfTrailingZeros(bits);
                bits &= bits - 1;
            }
        }
        if (next != count) throw new IllegalStateException("mask changed during selection");
        return positions;
    }
    private static long maskedWord(long[] words, int logicalRows, int index) {
        long bits = words[index];
        if (index == words.length - 1 && (logicalRows & 63) != 0)
            bits &= -1L >>> (64 - (logicalRows & 63));
        return bits;
    }
    private static native int[] selectedRows0(long[] words, int logicalRows);
}
