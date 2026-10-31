// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.tools.cpu;

import java.util.Objects;

/** Spatial filtering and exact squared-distance nearest-neighbor over parallel int lanes.
 * Bound coordinates to avoid overflow; no H3/S2 compatibility or geographic CRS claim.
 */
public final class M3SpatialQueries {
    private M3SpatialQueries() { }
    private static final int LIMIT = 1_000_000;

    /** Original row order and empty result are preserved. Inclusive rectangle bounds. */
    public static long[] rectangle(int[] x, int[] y, int minX, int minY, int maxX, int maxY) {
        validate(x, y);
        if (maxX < minX || maxY < minY) throw new IllegalArgumentException("rectangle geometry");
        long[] mask = new long[(x.length + 63) >>> 6];
        for (int i = 0; i < x.length; i++) if (x[i] >= minX && x[i] <= maxX && y[i] >= minY && y[i] <= maxY)
            mask[i >>> 6] |= 1L << (i & 63);
        return mask;
    }

    /** Exact squared distance, tie-break by the smallest physical index. Empty => -1. */
    public static int nearest(int[] x, int[] y, int queryX, int queryY) {
        validate(x, y);
        if (queryX < -LIMIT || queryX > LIMIT || queryY < -LIMIT || queryY > LIMIT)
            throw new IllegalArgumentException("query coordinate");
        long best = Long.MAX_VALUE;
        int closest = -1;
        for (int i = 0; i < x.length; i++) {
            long dx = (long) x[i] - queryX, dy = (long) y[i] - queryY;
            long distance = dx * dx + dy * dy;
            if (distance < best) { best = distance; closest = i; }
        }
        return closest;
    }

    private static void validate(int[] x, int[] y) {
        Objects.requireNonNull(x); Objects.requireNonNull(y);
        if (x.length != y.length || x.length > 1_000_000) throw new IllegalArgumentException("points geometry");
        for (int i = 0; i < x.length; i++) if (x[i] < -LIMIT || x[i] > LIMIT || y[i] < -LIMIT || y[i] > LIMIT)
            throw new IllegalArgumentException("coordinates out of exact-distance range");
    }
}
