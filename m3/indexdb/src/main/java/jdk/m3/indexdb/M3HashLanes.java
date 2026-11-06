// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.indexdb;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Packed-lane helpers (DOOP): growth, trimming, sorted-distinct and byte views of primitive lanes.
 *
 * <p>Growth doubles like {@code jdk.m3.collections.M3LongArrayList.ensureCapacity}; the M3JDK 21
 * receiver replaces these with {@code M3LongArrayList}/{@code M3IntArrayList} (same add/get/size/
 * toArray contract, verified with javap on m3-collections 0.1.0-SNAPSHOT). Effects: none beyond the
 * returned arrays. Failure: none.</p>
 */
//FLLDCIM: DOOP — lanes are long[]/int[] + size; no object per element
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/f7b9599a63da1c1264ecacdcb4c9286e055a0948/synexia-openrewrite-recipes/crates/m3-hash-family-20261030/src/main/java/com/synexia/rewrite/hash/M3Lanes.java blob 5fe8ba7f0eb9; recipe m3/indexdb/recipe/hash-family-20261106/relocate.py v3
public final class M3HashLanes {
    private M3HashLanes() { }

    public static long[] grow(long[] lane, int needed) {
        return needed <= lane.length ? lane : Arrays.copyOf(lane, Math.max(needed, lane.length * 2));
    }

    public static int[] grow(int[] lane, int needed) {
        return needed <= lane.length ? lane : Arrays.copyOf(lane, Math.max(needed, lane.length * 2));
    }

    /** Sorted distinct copy of {@code lane[0, size)}. */
    public static long[] sortedDistinct(long[] lane, int size) {
        long[] sorted = Arrays.copyOf(lane, size);
        Arrays.sort(sorted);
        int out = 0;
        for (int i = 0; i < size; i++) {
            if (out == 0 || sorted[out - 1] != sorted[i]) sorted[out++] = sorted[i];
        }
        return Arrays.copyOf(sorted, out);
    }

    /** Big-endian bytes of {@code lane[0, size)}, the receipt's output identity. */
    public static byte[] bytes(long[] lane, int size) {
        ByteBuffer buffer = ByteBuffer.allocate(size * Long.BYTES);
        for (int i = 0; i < size; i++) buffer.putLong(lane[i]);
        return buffer.array();
    }

    public static byte[] bytes(int[] lane, int size) {
        ByteBuffer buffer = ByteBuffer.allocate(size * Integer.BYTES);
        for (int i = 0; i < size; i++) buffer.putInt(lane[i]);
        return buffer.array();
    }
}
