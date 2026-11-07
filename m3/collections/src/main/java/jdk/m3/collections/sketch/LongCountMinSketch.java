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
package jdk.m3.collections.sketch;

import java.util.Arrays;

/**
 * Approximate non-negative long frequencies in one flat long[] counter matrix.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/LongCountMinSketch.java}
 * blob {@code d412e1daed2464dfea74df884f43c270e688b251} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class LongCountMinSketch {
    private final long[] counters;
    private final int width;
    private final int mask;
    private final int depth;
    private long totalCount;

    public LongCountMinSketch(int width, int depth) {
        if (width <= 0) throw new IllegalArgumentException("width <= 0");
        if (depth <= 0 || depth > 32) throw new IllegalArgumentException("depth must be 1..32");
        this.width = nextPowerOfTwo(width);
        this.mask = this.width - 1;
        this.depth = depth;
        counters = new long[Math.multiplyExact(this.width, depth)];
    }

    public static LongCountMinSketch forError(double epsilon, double confidence) {
        if (!(epsilon > 0.0d && epsilon < 1.0d)) {
            throw new IllegalArgumentException("epsilon must be in (0,1)");
        }
        if (!(confidence > 0.0d && confidence < 1.0d)) {
            throw new IllegalArgumentException("confidence must be in (0,1)");
        }
        int width = (int) Math.ceil(Math.E / epsilon);
        int depth = (int) Math.ceil(Math.log(1.0d / (1.0d - confidence)));
        return new LongCountMinSketch(width, Math.max(1, depth));
    }

    public int width() { return width; }
    public int depth() { return depth; }
    public long totalCount() { return totalCount; }
    public long payloadBytes() { return (long) counters.length * Long.BYTES; }

    public long add(long value) { return add(value, 1L); }

    public long add(long value, long delta) {
        if (delta < 0L) throw new IllegalArgumentException("delta < 0");
        if (delta == 0L) return estimate(value);
        long estimate = Long.MAX_VALUE;
        long baseHash = LongBloomFilter.mix64(value);
        for (int row = 0; row < depth; row++) {
            int index = row * width + hashIndex(baseHash, row);
            counters[index] = saturatingAdd(counters[index], delta);
            estimate = Math.min(estimate, counters[index]);
        }
        totalCount = saturatingAdd(totalCount, delta);
        return estimate;
    }

    public long estimate(long value) {
        long estimate = Long.MAX_VALUE;
        long baseHash = LongBloomFilter.mix64(value);
        for (int row = 0; row < depth; row++) {
            estimate = Math.min(estimate, counters[row * width + hashIndex(baseHash, row)]);
        }
        return estimate;
    }

    public void merge(LongCountMinSketch other) {
        if (other.width != width || other.depth != depth) {
            throw new IllegalArgumentException("shape mismatch");
        }
        for (int i = 0; i < counters.length; i++) {
            counters[i] = saturatingAdd(counters[i], other.counters[i]);
        }
        totalCount = saturatingAdd(totalCount, other.totalCount);
    }

    public void clear() {
        Arrays.fill(counters, 0L);
        totalCount = 0L;
    }

    private int hashIndex(long baseHash, int row) {
        long hash = LongBloomFilter.mix64(baseHash + 0x9e3779b97f4a7c15L * (row + 1L));
        return (int) hash & mask;
    }

    private static long saturatingAdd(long left, long right) {
        if (Long.MAX_VALUE - left < right) return Long.MAX_VALUE;
        return left + right;
    }

    private static int nextPowerOfTwo(int value) {
        if (value > (1 << 30)) throw new IllegalArgumentException("width too large");
        return value <= 1 ? 1 : Integer.highestOneBit(value - 1) << 1;
    }
}
