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
 * Fixed-range long histogram with exact bucket counters and approximate quantiles.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/LongHistogram.java}
 * blob {@code 867459ba820434ad609f8ad1a06b0d48cdfc0600} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class LongHistogram {
    private final long minInclusive;
    private final long maxExclusive;
    private final long[] counts;
    private long underflow;
    private long overflow;
    private long total;
    // Exactly one source-local quantile memo. No array, per-bucket object or
    // additional query allocation is needed for repeated unchanged requests.
    private long cachedQuantileBits;
    private long cachedQuantileValue;
    private boolean quantileCacheValid;

    public LongHistogram(long minInclusive, long maxExclusive, int buckets) {
        if (maxExclusive <= minInclusive) throw new IllegalArgumentException("empty range");
        if (buckets <= 0) throw new IllegalArgumentException("buckets <= 0");
        this.minInclusive = minInclusive;
        this.maxExclusive = maxExclusive;
        counts = new long[buckets];
    }

    public int bucketCount() { return counts.length; }
    public long totalCount() { return total; }
    public long underflowCount() { return underflow; }
    public long overflowCount() { return overflow; }
    public long payloadBytes() { return (long) counts.length * Long.BYTES; }

    public int add(long value) {
        quantileCacheValid = false;
        total = saturatingIncrement(total);
        if (value < minInclusive) {
            underflow = saturatingIncrement(underflow);
            return -1;
        }
        if (value >= maxExclusive) {
            overflow = saturatingIncrement(overflow);
            return counts.length;
        }
        int bucket = bucketOf(value);
        counts[bucket] = saturatingIncrement(counts[bucket]);
        return bucket;
    }

    public long bucketCount(int bucket) {
        if (bucket < 0 || bucket >= counts.length) throw new IndexOutOfBoundsException(bucket);
        return counts[bucket];
    }

    public long bucketLowerBound(int bucket) {
        if (bucket < 0 || bucket >= counts.length) throw new IndexOutOfBoundsException(bucket);
        double fraction = bucket / (double) counts.length;
        return clampToLong(minInclusive + fraction * ((double) maxExclusive - minInclusive));
    }

    public long bucketUpperBoundExclusive(int bucket) {
        if (bucket < 0 || bucket >= counts.length) throw new IndexOutOfBoundsException(bucket);
        if (bucket == counts.length - 1) return maxExclusive;
        double fraction = (bucket + 1.0d) / counts.length;
        return clampToLong(minInclusive + fraction * ((double) maxExclusive - minInclusive));
    }

    /**
     * Approximate quantile as the lower bound of the bucket containing rank q.
     * Underflow maps to minInclusive and overflow maps to maxExclusive.
     */
    public long quantile(double q) {
        if (!(q >= 0.0d && q <= 1.0d)) throw new IllegalArgumentException("q must be in [0,1]");
        if (total == 0L) throw new java.util.NoSuchElementException();
        long bits = Double.doubleToRawLongBits(q);
        if (quantileCacheValid && cachedQuantileBits == bits) return cachedQuantileValue;
        long target = q == 1.0d ? total - 1L : (long) Math.floor(q * total);
        long cumulative = underflow;
        if (target < cumulative) return rememberQuantile(bits, minInclusive);
        for (int bucket = 0; bucket < counts.length; bucket++) {
            cumulative = saturatingAdd(cumulative, counts[bucket]);
            if (target < cumulative) return rememberQuantile(bits, bucketLowerBound(bucket));
        }
        return rememberQuantile(bits, maxExclusive);
    }

    private long rememberQuantile(long bits, long value) {
        cachedQuantileBits = bits;
        cachedQuantileValue = value;
        quantileCacheValid = true;
        return value;
    }

    public void merge(LongHistogram other) {
        if (other.minInclusive != minInclusive
                || other.maxExclusive != maxExclusive
                || other.counts.length != counts.length) {
            throw new IllegalArgumentException("histogram shape mismatch");
        }
        quantileCacheValid = false;
        underflow = saturatingAdd(underflow, other.underflow);
        overflow = saturatingAdd(overflow, other.overflow);
        total = saturatingAdd(total, other.total);
        for (int i = 0; i < counts.length; i++) {
            counts[i] = saturatingAdd(counts[i], other.counts[i]);
        }
    }

    public void clear() {
        quantileCacheValid = false;
        Arrays.fill(counts, 0L);
        underflow = 0L;
        overflow = 0L;
        total = 0L;
    }

    private int bucketOf(long value) {
        double normalized = ((double) value - minInclusive)
                / ((double) maxExclusive - minInclusive);
        int bucket = (int) (normalized * counts.length);
        return Math.min(counts.length - 1, Math.max(0, bucket));
    }

    private static long clampToLong(double value) {
        if (value <= Long.MIN_VALUE) return Long.MIN_VALUE;
        if (value >= Long.MAX_VALUE) return Long.MAX_VALUE;
        return (long) Math.floor(value);
    }

    private static long saturatingIncrement(long value) {
        return value == Long.MAX_VALUE ? Long.MAX_VALUE : value + 1L;
    }

    private static long saturatingAdd(long left, long right) {
        return Long.MAX_VALUE - left < right ? Long.MAX_VALUE : left + right;
    }
}
