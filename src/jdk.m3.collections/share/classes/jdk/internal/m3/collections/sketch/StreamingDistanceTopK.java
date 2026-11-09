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
package jdk.internal.m3.collections.sketch;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.LongUnaryOperator;
import jdk.internal.m3.collections.LongKeyOrder;

/**
 * Thread-confined nearest-K selection. Retains two long[K] lanes, not a score per input.
 * Input ordinals must be strictly increasing and nonnegative, including rejected candidates.
 * Equal distances retain earlier input ordinals. Scores are nonnegative signed longs.
 * A throwing extractor does not advance the input cursor. Callbacks must not reenter this owner.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/foss/StreamingDistanceTopK.java}
 * blob {@code e704a0e0f10982abc3210ba32d81a2d3cda53681} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections subpackages batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
public final class StreamingDistanceTopK {
    private final long[] distances;
    private final long[] ordinals;
    private int size;
    private long lastOrdinal = -1;

    public StreamingDistanceTopK(int limit) {
        if (limit < 0) throw new IllegalArgumentException("limit");
        distances = new long[limit];
        ordinals = new long[limit];
    }
    public int size() { return size; }
    public int limit() { return distances.length; }
    /** Backing-array payload only; excludes array/object headers and export workspace. */
    public long payloadBytes() { return 16L * distances.length; }
    public long threshold() {
        if (size == 0) throw new java.util.NoSuchElementException();
        return distances[0];
    }
    /** Computes the distance once; a zero limit advances ordinal validation but skips extraction. */
    public boolean offerComputed(long ordinal, LongUnaryOperator distance) {
        Objects.requireNonNull(distance, "distance");
        checkOrdinal(ordinal);
        if (distances.length == 0) { lastOrdinal = ordinal; return false; }
        return offer(ordinal, distance.applyAsLong(ordinal));
    }
    public boolean offer(long ordinal, long distance) {
        checkOrdinal(ordinal);
        if (distance < 0) throw new IllegalArgumentException("negative distance");
        lastOrdinal = ordinal;
        if (distances.length == 0) return false;
        if (size < distances.length) {
            int at = size++;
            while (at > 0) {
                int parent = (at - 1) >>> 1;
                if (!worse(distance, ordinal, distances[parent], ordinals[parent])) break;
                distances[at] = distances[parent]; ordinals[at] = ordinals[parent]; at = parent;
            }
            distances[at] = distance; ordinals[at] = ordinal;
            return true;
        }
        // New ordinals are larger, so an equal cutoff cannot improve stable nearest-K.
        if (distance >= distances[0]) return false;
        int at = 0;
        while (at < (size >>> 1)) {
            int child = (at << 1) + 1;
            if (child + 1 < size && worse(distances[child+1], ordinals[child+1],
                    distances[child], ordinals[child])) child++;
            if (!worse(distances[child], ordinals[child], distance, ordinal)) break;
            distances[at] = distances[child]; ordinals[at] = ordinals[child]; at = child;
        }
        distances[at] = distance; ordinals[at] = ordinal;
        return true;
    }
    /** Detached ordinal snapshot ordered by (distance, source ordinal). O(K) export workspace. */
    public long[] toSortedOrdinals() {
        long[] d = Arrays.copyOf(distances, size);
        long[] ids = Arrays.copyOf(ordinals, size);
        int[] order = LongKeyOrder.lexicographic(d, ids);
        long[] result = new long[size];
        for (int i = 0; i < size; i++) result[i] = ids[order[i]];
        return result;
    }
    public void clear() { size = 0; lastOrdinal = -1; }
    private void checkOrdinal(long ordinal) {
        if (ordinal < 0 || ordinal <= lastOrdinal) throw new IllegalArgumentException("ordinal order");
    }
    private static boolean worse(long a, long ai, long b, long bi) {
        return a > b || (a == b && ai > bi);
    }
}
