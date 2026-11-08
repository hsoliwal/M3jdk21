/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.internal.m3.collections;

import java.util.Arrays;
import java.util.Objects;

/**
 * Stable primitive permutations; input lanes are read-only for the entire call.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/foss/LongKeyOrder.java}
 * blob {@code 278421b855ad754a97bd6299f002f215a78ff3f3} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections subpackages batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
public final class LongKeyOrder {
    private LongKeyOrder() { }

    /** Full signed or unsigned order; equal keys retain their original source order. */
    public static int[] ascending(long[] keys, boolean unsigned) {
        Objects.requireNonNull(keys, "keys");
        int[] order = identity(keys.length);
        if (order.length > 1) radix(keys, unsigned, order, new int[order.length]);
        return order;
    }

    /** Signed lexicographic (primary, secondary, original ordinal) permutation. */
    public static int[] lexicographic(long[] primary, long[] secondary) {
        Objects.requireNonNull(primary, "primary");
        Objects.requireNonNull(secondary, "secondary");
        if (primary.length != secondary.length) throw new IllegalArgumentException("lane lengths");
        int[] order = identity(primary.length);
        if (order.length > 1) {
            int[] scratch = new int[order.length];
            radix(secondary, false, order, scratch);
            radix(primary, false, order, scratch);
        }
        return order;
    }

    /**
     * The smallest min(k,n) ordinals, ordered by (key, original ordinal).
     * O(n log k + k log k) time; O(k) primitive storage. k=0 returns immediately.
     */
    public static int[] smallest(long[] keys, int k, boolean unsigned) {
        Objects.requireNonNull(keys, "keys");
        if (k < 0) throw new IllegalArgumentException("k");
        int limit = Math.min(k, keys.length);
        int[] heap = new int[limit];
        if (limit == 0) return heap;
        for (int i = 0; i < limit; i++) heap[i] = i;
        for (int i = (limit >>> 1) - 1; i >= 0; i--) sift(keys, unsigned, heap, limit, i, heap[i]);
        for (int i = limit; i < keys.length; i++) {
            if (compare(keys, unsigned, i, heap[0]) < 0) sift(keys, unsigned, heap, limit, 0, i);
        }
        // Max-heap extraction yields ascending order without a boxed index or another array.
        for (int end = limit - 1; end > 0; end--) {
            int largest = heap[0];
            sift(keys, unsigned, heap, end, 0, heap[end]);
            heap[end] = largest;
        }
        return heap;
    }

    private static int[] identity(int size) {
        int[] result = new int[size];
        for (int i = 0; i < size; i++) result[i] = i;
        return result;
    }

    private static int compare(long[] keys, boolean unsigned, int a, int b) {
        int c = unsigned ? Long.compareUnsigned(keys[a], keys[b]) : Long.compare(keys[a], keys[b]);
        return c != 0 ? c : Integer.compare(a, b);
    }

    private static void sift(long[] keys, boolean unsigned, int[] heap, int size, int at, int value) {
        while (at < (size >>> 1)) {
            int child = (at << 1) + 1;
            if (child + 1 < size && compare(keys, unsigned, heap[child], heap[child + 1]) < 0) child++;
            if (compare(keys, unsigned, value, heap[child]) >= 0) break;
            heap[at] = heap[child];
            at = child;
        }
        heap[at] = value;
    }

    /** Eight stable byte passes. Flipping the high sign bit maps signed to unsigned order. */
    private static void radix(long[] keys, boolean unsigned, int[] order, int[] scratch) {
        int[] from = order;
        int[] to = scratch;
        int[] counts = new int[256];
        for (int shift = 0; shift < Long.SIZE; shift += Byte.SIZE) {
            Arrays.fill(counts, 0);
            int sign = !unsigned && shift == 56 ? 128 : 0;
            for (int index : from) counts[((int) (keys[index] >>> shift) & 255) ^ sign]++;
            int sum = 0;
            for (int i = 0; i < counts.length; i++) {
                int count = counts[i];
                counts[i] = sum;
                sum += count;
            }
            for (int index : from) to[counts[((int) (keys[index] >>> shift) & 255) ^ sign]++] = index;
            int[] swap = from;
            from = to;
            to = swap;
        }
        // Exactly eight swaps put the result back in order; no record lane is moved.
    }
}
