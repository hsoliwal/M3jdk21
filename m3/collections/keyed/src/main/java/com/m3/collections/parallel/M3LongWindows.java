// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
package com.m3.collections.parallel;

import java.util.Objects;

/** Full contiguous long windows. Primitive ring-deque atom; O(n) time and O(width) scratch. */
public final class M3LongWindows {
    private M3LongWindows() { }
    public static long[] max(long[] values, int width) { return max(values, width, M3Parallel.Backend.JAVA); }
    /** Fresh output, no input mutation. A positive width greater than the input length yields empty. */
    public static long[] max(long[] values, int width, M3Parallel.Backend backend) {
        Objects.requireNonNull(values); Objects.requireNonNull(backend);
        if (width <= 0) throw new IllegalArgumentException("nonpositive window width");
        return backend == M3Parallel.Backend.NATIVE
                ? M3PartitionLayout.Native.windowMax(values, width) : javaMax(values, width);
    }
    public static long[] min(long[] values, int width) { return min(values, width, M3Parallel.Backend.JAVA); }
    /** Same fresh-output and width contract as max; handles Long.MIN_VALUE without negation. */
    public static long[] min(long[] values, int width, M3Parallel.Backend backend) {
        Objects.requireNonNull(values); Objects.requireNonNull(backend);
        if (width <= 0) throw new IllegalArgumentException("nonpositive window width");
        return backend == M3Parallel.Backend.NATIVE
                ? M3PartitionLayout.Native.windowMin(values, width) : extrema(values, width, false);
    }
    static long[] javaMax(long[] values, int width) { return extrema(values, width, true); }
    private static long[] extrema(long[] values, int width, boolean maximum) {
        Objects.requireNonNull(values);
        if (width <= 0) throw new IllegalArgumentException("nonpositive window width");
        if (width > values.length) return new long[0];
        long[] result = new long[values.length - width + 1];
        int[] queue = new int[width]; int head = 0, tail = 0, size = 0;
        for (int i = 0; i < values.length; i++) {
            if (size > 0 && queue[head] <= i - width) { head = next(head, width); size--; }
            while (size > 0) {
                int last = tail == 0 ? width - 1 : tail - 1;
                if (maximum ? values[queue[last]] >= values[i] : values[queue[last]] <= values[i]) break;
                tail = last; size--;
            }
            queue[tail] = i; tail = next(tail, width); size++;
            if (i >= width - 1) result[i - width + 1] = values[queue[head]];
        }
        return result;
    }
    private static int next(int position, int width) { return position == width - 1 ? 0 : position + 1; }
}
