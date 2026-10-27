/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

/**
 * In-place lane algorithms over {@code long[]}/{@code int[]} value lanes and {@code long[]} state
 * bitmasks: the array-manipulation rows of the algorithm catalogue (leaf GAP-LC-catalogue-gaps)
 * that the lane owners' compaction and reordering paths share.
 *
 * <p>Catalogue rationale (idea-only rows, nothing copied from any editorial): LeetCode 189 rotate
 * array (three-reversal {@link #rotate(long[], int, int, int)} and juggling/cycle-leader
 * {@link #rotateByCycles(long[], int, int, int)}), LeetCode 75 sort colors (Dutch national flag
 * three-way partition {@link #partition3(long[], int, int, long)}), LeetCode 41 first missing
 * positive and LeetCode 268 missing number (cyclic sort / slot permutation
 * {@link #cyclicPlace(int[], int, int, int)} and {@link #firstAbsent(int[], int, int, int)}).
 * Every method works on a half-open range {@code [from, to)}, is O(n) time and O(1) extra space,
 * and allocates nothing (per-element allocation: 0; no temporaries beyond scalars). Mask variants
 * keep a {@code long[]} state bitmask aligned with the lane it describes: bit {@code i} is
 * {@code words[i >>> 6] & (1L << i)}, the layout {@code java.util.BitSet} and the M3 occupancy
 * lanes use. Proving-ground tooling for the M3 collection owners; not a {@code java.util.Arrays}
 * or {@code java.util.Collections} replacement.</p>
 */
public final class M3LaneAlgorithms {
    private M3LaneAlgorithms() {
    }

    /** Reverses {@code lane[from, to)} in place. */
    public static void reverse(long[] lane, int from, int to) {
        checkRange(lane.length, from, to);
        for (int i = from, j = to - 1; i < j; i++, j--) {
            long t = lane[i];
            lane[i] = lane[j];
            lane[j] = t;
        }
    }

    /** Reverses {@code lane[from, to)} in place. */
    public static void reverse(int[] lane, int from, int to) {
        checkRange(lane.length, from, to);
        for (int i = from, j = to - 1; i < j; i++, j--) {
            int t = lane[i];
            lane[i] = lane[j];
            lane[j] = t;
        }
    }

    /** Reverses the bit range {@code [from, to)} of a state bitmask in place. */
    public static void reverseBits(long[] words, int from, int to) {
        checkRange((long) words.length << 6, from, to);
        for (int i = from, j = to - 1; i < j; i++, j--) {
            if (bit(words, i) != bit(words, j)) {
                words[i >>> 6] ^= 1L << i;
                words[j >>> 6] ^= 1L << j;
            }
        }
    }

    /**
     * Rotates {@code lane[from, to)} by {@code distance} with {@code java.util.Collections.rotate}
     * semantics (the element at {@code i} moves to {@code from + ((i - from + distance) mod n)});
     * LeetCode 189 three-reversal form, any {@code int} distance including negatives.
     */
    public static void rotate(long[] lane, int from, int to, int distance) {
        checkRange(lane.length, from, to);
        int shift = normalizedShift(to - from, distance);
        if (shift == 0) {
            return;
        }
        reverse(lane, from, to);
        reverse(lane, from, from + shift);
        reverse(lane, from + shift, to);
    }

    /** {@code int[]} form of {@link #rotate(long[], int, int, int)}. */
    public static void rotate(int[] lane, int from, int to, int distance) {
        checkRange(lane.length, from, to);
        int shift = normalizedShift(to - from, distance);
        if (shift == 0) {
            return;
        }
        reverse(lane, from, to);
        reverse(lane, from, from + shift);
        reverse(lane, from + shift, to);
    }

    /** Rotates the bit range {@code [from, to)} of a state bitmask, same semantics as {@link #rotate(long[], int, int, int)}. */
    public static void rotateBits(long[] words, int from, int to, int distance) {
        checkRange((long) words.length << 6, from, to);
        int shift = normalizedShift(to - from, distance);
        if (shift == 0) {
            return;
        }
        reverseBits(words, from, to);
        reverseBits(words, from, from + shift);
        reverseBits(words, from + shift, to);
    }

    /**
     * Same result as {@link #rotate(long[], int, int, int)} by the juggling (cycle-leader) method:
     * {@code gcd(n, shift)} cycles, each element written exactly once; LeetCode 189 alternative.
     */
    public static void rotateByCycles(long[] lane, int from, int to, int distance) {
        checkRange(lane.length, from, to);
        int n = to - from;
        int shift = normalizedShift(n, distance);
        if (shift == 0) {
            return;
        }
        int cycles = gcd(n, shift);
        for (int start = 0; start < cycles; start++) {
            long carried = lane[from + start];
            int index = start;
            do {
                index += shift;
                if (index >= n) {
                    index -= n;
                }
                long displaced = lane[from + index];
                lane[from + index] = carried;
                carried = displaced;
            } while (index != start);
        }
    }

    /** {@code int[]} form of {@link #rotateByCycles(long[], int, int, int)}. */
    public static void rotateByCycles(int[] lane, int from, int to, int distance) {
        checkRange(lane.length, from, to);
        int n = to - from;
        int shift = normalizedShift(n, distance);
        if (shift == 0) {
            return;
        }
        int cycles = gcd(n, shift);
        for (int start = 0; start < cycles; start++) {
            int carried = lane[from + start];
            int index = start;
            do {
                index += shift;
                if (index >= n) {
                    index -= n;
                }
                int displaced = lane[from + index];
                lane[from + index] = carried;
                carried = displaced;
            } while (index != start);
        }
    }

    /**
     * Dutch national flag three-way partition (LeetCode 75) of {@code lane[from, to)} around
     * {@code pivot} in one sweep with three write pointers: afterwards {@code [from, low)} holds
     * values below the pivot, {@code [low, high)} values equal to it and {@code [high, to)} values
     * above it. Unstable. Returns {@code low} and {@code high} packed as one {@code long}; read
     * them with {@link #lowBound(long)} and {@link #highBound(long)}.
     */
    public static long partition3(long[] lane, int from, int to, long pivot) {
        checkRange(lane.length, from, to);
        int low = from;
        int mid = from;
        int high = to;
        while (mid < high) {
            long v = lane[mid];
            if (v < pivot) {
                lane[mid++] = lane[low];
                lane[low++] = v;
            } else if (v > pivot) {
                lane[mid] = lane[--high];
                lane[high] = v;
            } else {
                mid++;
            }
        }
        return pack(low, high);
    }

    /** {@code int[]} form of {@link #partition3(long[], int, int, long)}. */
    public static long partition3(int[] lane, int from, int to, int pivot) {
        checkRange(lane.length, from, to);
        int low = from;
        int mid = from;
        int high = to;
        while (mid < high) {
            int v = lane[mid];
            if (v < pivot) {
                lane[mid++] = lane[low];
                lane[low++] = v;
            } else if (v > pivot) {
                lane[mid] = lane[--high];
                lane[high] = v;
            } else {
                mid++;
            }
        }
        return pack(low, high);
    }

    /** First index of the equal-to-pivot run from a {@link #partition3(long[], int, int, long)} result. */
    public static int lowBound(long packedBounds) {
        return (int) (packedBounds >>> 32);
    }

    /** First index of the above-pivot run from a {@link #partition3(long[], int, int, long)} result. */
    public static int highBound(long packedBounds) {
        return (int) packedBounds;
    }

    /**
     * Stable compaction by state bitmask: the elements of {@code lane[from, to)} whose {@code keep}
     * bit is set are moved, in order, to {@code lane[from, from + count)} and {@code count} is
     * returned; slots at or beyond {@code from + count} keep stale values. The mask is realigned
     * with the lane: bits {@code [from, from + count)} set, {@code [from + count, to)} cleared.
     */
    public static int compact(long[] lane, long[] keep, int from, int to) {
        checkRange(lane.length, from, to);
        checkRange((long) keep.length << 6, from, to);
        int write = from;
        for (int read = from; read < to; read++) {
            if (bit(keep, read)) {
                lane[write++] = lane[read];
            }
        }
        fill(keep, from, write, true);
        fill(keep, write, to, false);
        return write - from;
    }

    /** {@code int[]} form of {@link #compact(long[], long[], int, int)}. */
    public static int compact(int[] lane, long[] keep, int from, int to) {
        checkRange(lane.length, from, to);
        checkRange((long) keep.length << 6, from, to);
        int write = from;
        for (int read = from; read < to; read++) {
            if (bit(keep, read)) {
                lane[write++] = lane[read];
            }
        }
        fill(keep, from, write, true);
        fill(keep, write, to, false);
        return write - from;
    }

    /**
     * Cyclic sort / slot permutation: every value {@code v} of {@code lane[from, to)} with
     * {@code base <= v < base + n} ends in slot {@code from + (v - base)} (one representative when
     * duplicated); values outside that window stay in whichever slots remain. O(n) swaps.
     */
    public static void cyclicPlace(int[] lane, int from, int to, int base) {
        checkRange(lane.length, from, to);
        long top = (long) base + (to - from);
        for (int i = from; i < to; i++) {
            for (;;) {
                int v = lane[i];
                if (v < base || v >= top) {
                    break;
                }
                int target = from + (v - base);
                if (lane[target] == v) {
                    break;
                }
                lane[i] = lane[target];
                lane[target] = v;
            }
        }
    }

    /** {@code long[]} form of {@link #cyclicPlace(int[], int, int, int)}. */
    public static void cyclicPlace(long[] lane, int from, int to, long base) {
        checkRange(lane.length, from, to);
        int n = to - from;
        for (int i = from; i < to; i++) {
            for (;;) {
                long v = lane[i];
                long offset = v - base;
                if (v < base || Long.compareUnsigned(offset, n) >= 0) {
                    break;
                }
                int target = from + (int) offset;
                if (lane[target] == v) {
                    break;
                }
                lane[i] = lane[target];
                lane[target] = v;
            }
        }
    }

    /**
     * Smallest value {@code >= base} absent from {@code lane[from, to)}, found in O(n) time and O(1)
     * space by {@link #cyclicPlace(int[], int, int, int)} followed by one scan; the lane is left
     * permuted. {@code base = 1} is LeetCode 41 (first missing positive), {@code base = 0} is
     * LeetCode 268 (missing number). The answer never exceeds {@code base + n}, which must fit an
     * {@code int}.
     */
    public static int firstAbsent(int[] lane, int from, int to, int base) {
        int n = to - from;
        if ((long) base + n > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("base + length overflows int");
        }
        cyclicPlace(lane, from, to, base);
        for (int i = from; i < to; i++) {
            if (lane[i] != base + (i - from)) {
                return base + (i - from);
            }
        }
        return base + n;
    }

    /** {@code long[]} form of {@link #firstAbsent(int[], int, int, int)}; {@code base + n} must fit a {@code long}. */
    public static long firstAbsent(long[] lane, int from, int to, long base) {
        int n = to - from;
        if (base > Long.MAX_VALUE - n) {
            throw new IllegalArgumentException("base + length overflows long");
        }
        cyclicPlace(lane, from, to, base);
        for (int i = from; i < to; i++) {
            if (lane[i] != base + (i - from)) {
                return base + (i - from);
            }
        }
        return base + n;
    }

    /** LeetCode 41: smallest positive integer absent from {@code lane[from, to)}; permutes the lane. */
    public static int firstMissingPositive(int[] lane, int from, int to) {
        return firstAbsent(lane, from, to, 1);
    }

    /** LeetCode 268: smallest non-negative integer absent from {@code lane[from, to)}; permutes the lane. */
    public static int missingNumber(int[] lane, int from, int to) {
        return firstAbsent(lane, from, to, 0);
    }

    private static int normalizedShift(int length, int distance) {
        if (length < 2) {
            return 0;
        }
        return Math.floorMod(distance, length);
    }

    private static int gcd(int a, int b) {
        int x = a;
        int y = b;
        while (y != 0) {
            int t = x % y;
            x = y;
            y = t;
        }
        return x;
    }

    private static long pack(int low, int high) {
        return ((long) low << 32) | (high & 0xFFFFFFFFL);
    }

    private static boolean bit(long[] words, int index) {
        return (words[index >>> 6] & (1L << index)) != 0;
    }

    /** Word-wise fill of the bit range {@code [from, to)}, {@code java.util.BitSet.set(from, to)} layout. */
    private static void fill(long[] words, int from, int to, boolean value) {
        if (from >= to) {
            return;
        }
        int firstWord = from >>> 6;
        int lastWord = (to - 1) >>> 6;
        long firstMask = -1L << from;
        long lastMask = -1L >>> -to;
        if (firstWord == lastWord) {
            apply(words, firstWord, firstMask & lastMask, value);
            return;
        }
        apply(words, firstWord, firstMask, value);
        for (int word = firstWord + 1; word < lastWord; word++) {
            words[word] = value ? -1L : 0L;
        }
        apply(words, lastWord, lastMask, value);
    }

    private static void apply(long[] words, int word, long mask, boolean value) {
        if (value) {
            words[word] |= mask;
        } else {
            words[word] &= ~mask;
        }
    }

    private static void checkRange(long length, int from, int to) {
        if (from < 0 || to > length || from > to) {
            throw new IndexOutOfBoundsException("range [" + from + ", " + to + ") outside 0.." + length);
        }
    }

    /**
     * LeetCode #239: physical index of the maximum in each sliding window.
     * Equal maxima retain their earliest physical index (strict tail eviction).
     * The payload is never moved; no per-entry object is retained.
     */
    public static int[] slidingMaximumIndices(long[] values, int window) {
        java.util.Objects.requireNonNull(values, "values");
        if (window <= 0 || window > values.length) {
            throw new IllegalArgumentException("window must lie in [1, values.length]");
        }
        int[] indices = new int[values.length - window + 1];
        slidingMaximumIndicesInto(values, 0, values.length, window,
                indices, 0, false);
        return indices;
    }

    /**
     * Writes absolute physical indexes of maximum elements into a caller-owned
     * output lane. The complete range/shape is checked before the first write.
     * JNI is optional; a required-native call never silently falls back.
     */
    public static void slidingMaximumIndicesInto(long[] values, int from, int to,
            int window, int[] output, int outputFrom, boolean nativeRequired) {
        java.util.Objects.requireNonNull(values, "values");
        java.util.Objects.requireNonNull(output, "output");
        java.util.Objects.checkFromToIndex(from, to, values.length);
        int span = to - from;
        if (window <= 0 || window > span) {
            throw new IllegalArgumentException("window must lie in [1, to-from]");
        }
        int count = span - window + 1;
        java.util.Objects.checkFromIndexSize(outputFrom, count, output.length);
        if (nativeRequired) {
            if (!SlidingMaximumNative.AVAILABLE) {
                throw new UnsatisfiedLinkError("M3 sliding-maximum JNI unavailable");
            }
            slidingMaximumIndices0(values, from, to, window, output, outputFrom);
            return;
        }
        // JNI amortizes across larger ranges. Small windows avoid the call.
        if (SlidingMaximumNative.AVAILABLE && !SlidingMaximumNative.ABI_MISSING
                && span >= 256 && window > 1) {
            try {
                slidingMaximumIndices0(values, from, to, window, output, outputFrom);
                return;
            } catch (UnsatisfiedLinkError oldNativeImage) {
                // Native extension absent from an earlier installed image.
                // Explicit native-required calls above never downgrade.
                SlidingMaximumNative.ABI_MISSING = true;
            }
        }
        slidingMaximumIndicesJava(values, from, to, window, output, outputFrom);
    }

    private static void slidingMaximumIndicesJava(long[] values, int from, int to,
            int window, int[] output, int outputFrom) {
        // Ring deque stores only physical indexes; at most window entries.
        int[] deque = new int[window];
        int head = 0;
        int size = 0;
        for (int index = from; index < to; index++) {
            if (size != 0 && deque[head] <= index - window) {
                head = (head + 1 == window) ? 0 : head + 1;
                size--;
            }
            // Strict less-than keeps the oldest maximum on ties.
            while (size != 0 && values[deque[ringSlot(head, size - 1, window)]] < values[index]) {
                size--;
            }
            deque[ringSlot(head, size, window)] = index;
            size++;
            if (index - from >= window - 1) {
                output[outputFrom++] = deque[head];
            }
        }
    }

    /** Overflow-safe ring offset, even for an almost-full maximum-sized lane. */
    private static int ringSlot(int head, int offset, int capacity) {
        int remaining = capacity - head;
        return offset < remaining ? head + offset : offset - remaining;
    }

    public static boolean slidingMaximumNativeAvailable() {
        return SlidingMaximumNative.AVAILABLE && !SlidingMaximumNative.ABI_MISSING;
    }

    private static final class SlidingMaximumNative {
        static volatile boolean ABI_MISSING;
        static final boolean AVAILABLE = load();

        private static boolean load() {
            if (!Boolean.getBoolean("m3.collections.lane.native")) return false;
            try {
                System.loadLibrary("m3_collections_address");
                return true;
            } catch (UnsatisfiedLinkError | SecurityException unavailable) {
                return false;
            }
        }
    }

    private static native void slidingMaximumIndices0(long[] values, int from,
            int to, int window, int[] output, int outputFrom);
}
