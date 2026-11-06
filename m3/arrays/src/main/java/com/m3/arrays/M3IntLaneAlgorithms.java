// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.arrays;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Primitive, array-backed search and scan atoms without row objects or boxing.
 * Sorted-search precondition: {@code lane[from,to)} is nondecreasing.
 * Native entrypoints are explicitly required, never silently substituted.
 */
public final class M3IntLaneAlgorithms {
    private M3IntLaneAlgorithms() {}

    /** First index with value >= key; {@code to} if absent. O(log n), O(1) extra memory. */
    public static int lowerBound(int[] lane, int from, int to, int key) {
        Objects.checkFromToIndex(from, to, Objects.requireNonNull(lane, "lane").length);
        int low = from;
        int high = to;
        while (low < high) {
            int middle = low + ((high - low) >>> 1);
            if (lane[middle] < key) low = middle + 1;
            else high = middle;
        }
        return low;
    }

    /** First index with value > key; {@code to} if absent. O(log n), O(1) extra memory. */
    public static int upperBound(int[] lane, int from, int to, int key) {
        Objects.checkFromToIndex(from, to, Objects.requireNonNull(lane, "lane").length);
        int low = from;
        int high = to;
        while (low < high) {
            int middle = low + ((high - low) >>> 1);
            if (lane[middle] <= key) low = middle + 1;
            else high = middle;
        }
        return low;
    }

    /** Two boundaries, in a detached [firstInclusive, afterLastExclusive] pair. */
    public static int[] equalRange(int[] lane, int from, int to, int key) {
        int first = lowerBound(lane, from, to, key);
        return new int[] {first, upperBound(lane, first, to, key)};
    }

    /** Native required lower bound; no Java fallback if the JNI owner is absent. */
    public static int lowerBoundRequired(int[] lane, int from, int to, int key) {
        Objects.checkFromToIndex(from, to, Objects.requireNonNull(lane, "lane").length);
        loadRequired();
        return nativeBound(lane, from, to, key, false);
    }

    /** Native required upper bound; bounded JNI region reads only. */
    public static int upperBoundRequired(int[] lane, int from, int to, int key) {
        Objects.checkFromToIndex(from, to, Objects.requireNonNull(lane, "lane").length);
        loadRequired();
        return nativeBound(lane, from, to, key, true);
    }

    private static void loadRequired() {
        if (!M3ArrayNative.isLoaded()) {
            String path = System.getProperty("m3.arrays.native.path");
            if (path == null || path.isBlank()) {
                throw new UnsatisfiedLinkError("M3 array JNI library is not loaded");
            }
            M3ArrayNative.load(Path.of(path));
        }
    }

    private static native int nativeBound(int[] lane, int from, int to, int key, boolean upper);

    /**
     * Stable compaction of [from,to): copies nonmatching values toward {@code from}.
     * Returns the new exclusive endpoint. Slots after that endpoint are unspecified.
     * O(n) time and O(1) space; the caller retains ownership of its lane.
     */
    public static int compactExcept(int[] lane, int from, int to, int rejected) {
        Objects.checkFromToIndex(from, to, Objects.requireNonNull(lane, "lane").length);
        int write = from;
        for (int read = from; read < to; read++) {
            int value = lane[read];
            if (value != rejected) lane[write++] = value;
        }
        return write;
    }

    /**
     * Rotates only [from,to) left by distance, allowing signed distances.
     * An empty range is a no-op. Three reversals, O(n) time, O(1) space.
     */
    public static void rotateLeft(int[] lane, int from, int to, int distance) {
        Objects.checkFromToIndex(from, to, Objects.requireNonNull(lane, "lane").length);
        int length = to - from;
        if (length < 2) return;
        int shift = Math.floorMod(distance, length);
        if (shift == 0) return;
        reverse(lane, from, from + shift);
        reverse(lane, from + shift, to);
        reverse(lane, from, to);
    }

    private static void reverse(int[] lane, int from, int to) {
        for (int left = from, right = to - 1; left < right; left++, right--) {
            int temporary = lane[left];
            lane[left] = lane[right];
            lane[right] = temporary;
        }
    }

    /**
     * Maximum after inclusive, zero-based range additions on a zero-initialized lane.
     * start/end/delta are aligned operation columns, not per-operation objects.
     * Empty input of size zero yields zero; a nonempty operation requires size > 0.
     * Uses checked signed-long arithmetic rather than silently wrapping.
     * O(size + operations) time, O(size) extra primitive memory.
     */
    public static long rangeAddMaximum(int size, int[] start, int[] end, long[] delta) {
        if (size < 0) throw new IllegalArgumentException("negative size");
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        Objects.requireNonNull(delta, "delta");
        if (start.length != end.length || start.length != delta.length) {
            throw new IllegalArgumentException("unaligned range columns");
        }
        if (size == 0) {
            if (start.length != 0) throw new IndexOutOfBoundsException("update of empty lane");
            return 0L;
        }
        // The last subtraction at end + 1 is unnecessary: only [0,size) is read.
        long[] difference = new long[size];
        for (int i = 0; i < start.length; i++) {
            int left = start[i];
            int right = end[i];
            if (left < 0 || right < left || right >= size) {
                throw new IndexOutOfBoundsException("invalid inclusive range at operation " + i);
            }
            difference[left] = Math.addExact(difference[left], delta[i]);
            if (right + 1 < size) {
                difference[right + 1] = Math.subtractExact(difference[right + 1], delta[i]);
            }
        }
        long running = 0L;
        long maximum = Long.MIN_VALUE;
        for (long increment : difference) {
            running = Math.addExact(running, increment);
            if (running > maximum) maximum = running;
        }
        return maximum;
    }

    /**
     * Maxima for each consecutive window, using an index-only monotonic deque.
     * O(n) time, O(windowSize) extra indices plus the returned output.
     */
    public static int[] slidingWindowMaximum(int[] lane, int windowSize) {
        Objects.requireNonNull(lane, "lane");
        if (windowSize < 1 || windowSize > lane.length) {
            throw new IllegalArgumentException("window outside [1,length]");
        }
        int[] result = new int[lane.length - windowSize + 1];
        int[] queue = new int[windowSize];
        int head = 0;
        int tail = 0;
        for (int i = 0; i < lane.length; i++) {
            while (head < tail && queue[head % windowSize] <= i - windowSize) head++;
            while (head < tail && lane[queue[(tail - 1) % windowSize]] <= lane[i]) tail--;
            queue[tail % windowSize] = i;
            tail++;
            if (i + 1 >= windowSize) result[i - windowSize + 1] = lane[queue[head % windowSize]];
        }
        return result;
    }
}
