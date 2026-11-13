// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.tools.cpu;

import java.util.Arrays;

/** Immutable, compact half-open rectangle index over detached integer lanes. */
public final class M3PackedSpatialIndex {
    private static final int MAX_RECTANGLES = 200_000;
    private static final long MAX_VISITS = 20_000_000L;

    private M3PackedSpatialIndex() { }

    /** Deep-copy caller lanes; output index is safe for concurrent read-only queries. */
    public static Index build(int[] minX, int[] minY, int[] maxX, int[] maxY) {
        if (minX == null || minY == null || maxX == null || maxY == null
                || minX.length > MAX_RECTANGLES || minY.length != minX.length
                || maxX.length != minX.length || maxY.length != minX.length) {
            throw new IllegalArgumentException("invalid packed rectangle lanes");
        }
        int count = minX.length;
        for (int i = 0; i < count; i++) {
            if (minX[i] > maxX[i] || minY[i] > maxY[i]) {
                throw new IllegalArgumentException("reversed half-open rectangle");
            }
        }
        int[] index = new int[count];
        int[] scratch = new int[count];
        for (int i = 0; i < count; i++) { index[i] = i; }
        // Bottom-up stable merge: no per-rectangle structural Entry/Node objects.
        for (int width = 1; width < count; width *= 2) {
            for (int begin = 0; begin < count; begin += 2 * width) {
                int middle = Math.min(count, begin + width);
                int end = Math.min(count, begin + 2 * width);
                int left = begin;
                int right = middle;
                for (int output = begin; output < end; output++) {
                    if (left == middle || (right < end
                            && minX[index[left]] > minX[index[right]])) {
                        scratch[output] = index[right++];
                    } else {
                        scratch[output] = index[left++];
                    }
                }
            }
            int[] old = index;
            index = scratch;
            scratch = old;
        }
        int[] prefixMaxX = new int[count];
        int maximum = Integer.MIN_VALUE;
        for (int i = 0; i < count; i++) {
            maximum = Math.max(maximum, maxX[index[i]]);
            prefixMaxX[i] = maximum;
        }
        return new Index(minX.clone(), minY.clone(), maxX.clone(), maxY.clone(),
                index, prefixMaxX);
    }

    /** Caller-visible query capability; no retained object per rectangle. */
    public static final class Index {
        private final int[] minX;
        private final int[] minY;
        private final int[] maxX;
        private final int[] maxY;
        private final int[] sorted;
        private final int[] prefixMaxX;

        private Index(int[] minX, int[] minY, int[] maxX, int[] maxY,
                int[] sorted, int[] prefixMaxX) {
            this.minX = minX;
            this.minY = minY;
            this.maxX = maxX;
            this.maxY = maxY;
            this.sorted = sorted;
            this.prefixMaxX = prefixMaxX;
        }

        public int size() { return sorted.length; }

        /**
         * Return sorted original IDs of all exact half-open intersections.
         * A result or work cap exhaustion throws; no hit is silently truncated.
         * Output is independently mutable and never aliases index storage.
         */
        public int[] query(int queryMinX, int queryMinY, int queryMaxX,
                int queryMaxY, int maxResults, long maxVisits) {
            if (queryMinX > queryMaxX || queryMinY > queryMaxY || maxResults < 0
                    || maxVisits < 0L || maxVisits > MAX_VISITS) {
                throw new IllegalArgumentException("invalid rectangle query or budget");
            }
            if (queryMinX == queryMaxX || queryMinY == queryMaxY) { return new int[0]; }
            int lo = 0;
            int hi = sorted.length;
            while (lo < hi) {
                int middle = lo + ((hi - lo) >>> 1);
                if (minX[sorted[middle]] < queryMaxX) { lo = middle + 1; }
                else { hi = middle; }
            }
            int[] matching = new int[Math.min(maxResults, sorted.length)];
            int used = 0;
            long remaining = maxVisits;
            for (int i = lo - 1; i >= 0; i--) {
                if (prefixMaxX[i] <= queryMinX) { break; }
                if (remaining == 0L) {
                    throw new IllegalStateException("spatial query visit budget exhausted");
                }
                remaining--;
                int id = sorted[i];
                if (minX[id] < maxX[id] && minY[id] < maxY[id]
                        && maxX[id] > queryMinX && minY[id] < queryMaxY
                        && maxY[id] > queryMinY) {
                    if (used == matching.length) {
                        throw new IllegalStateException("spatial query result budget exhausted");
                    }
                    matching[used++] = id;
                }
            }
            Arrays.sort(matching, 0, used);
            return Arrays.copyOf(matching, used);
        }
    }
}
