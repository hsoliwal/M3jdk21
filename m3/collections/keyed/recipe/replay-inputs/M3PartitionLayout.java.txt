// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
package com.m3.collections.parallel;

import java.util.Arrays;
import java.util.Objects;

/** Stable counting scatter atom; packed offsets followed by source row indices. */
final class M3PartitionLayout {
    private final int groups;
    private final int[] packed;
    private M3PartitionLayout(int groups, int[] packed) { this.groups = groups; this.packed = packed; }
    static M3PartitionLayout build(int[] ids, int groups, M3Parallel.Backend backend) {
        Objects.requireNonNull(backend);
        return new M3PartitionLayout(groups, backend == M3Parallel.Backend.NATIVE
                ? Native.scatter(ids, groups) : scatter(ids, groups));
    }
    static int[] scatter(int[] ids, int groups) {
        Objects.requireNonNull(ids);
        if (groups < 0 || groups > ids.length) throw new IllegalArgumentException("invalid group count");
        long total = (long) ids.length + groups + 1;
        if (total > Integer.MAX_VALUE - 8L) throw new IllegalArgumentException("layout too large");
        int[] out = new int[(int) total];
        for (int id : ids) {
            if (id < 0 || id >= groups) throw new IllegalArgumentException("invalid group id");
            out[id + 1]++;
        }
        for (int g = 0; g < groups; g++) out[g + 1] += out[g];
        int[] cursor = Arrays.copyOf(out, groups);
        for (int row = 0; row < ids.length; row++) out[groups + 1 + cursor[ids[row]]++] = row;
        return out;
    }
    int start(int group) { return packed[group]; }
    int end(int group) { return packed[group + 1]; }
    int row(int index) { return packed[groups + 1 + index]; }
    long bytes() { return (long) packed.length * Integer.BYTES; }
    /** Loading is lazy and explicit; the Java backend never needs the native library. */
    static final class Native {
        static { System.loadLibrary("m3_keyed"); }
        private Native() { }
        static native int[] scatter(int[] ids, int groups);
        static native long[] windowMax(long[] values, int width);
        static native long[] windowMin(long[] values, int width);
    }
}
