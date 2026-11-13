// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.tools.cpu;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable normalized signed-int half-open interval set backed by primitive endpoints.
 * Every range is [startInclusive,endExclusive), and adjacent ranges are merged.
 * Exactly one parent owns the retained endpoints; every export is detached.
 */
public final class M3IntIntervalSet {
    /** Maximum supplied nonempty-or-empty interval pairs admitted in one snapshot. */
    public static final int MAX_PAIRS = 1_000_000;
    private final int[] pairs;

    private M3IntIntervalSet(int[] normalized) {
        pairs = normalized;
    }

    /**
     * Normalize arbitrarily ordered pairs, rejecting reversed intervals and odd lengths.
     * Empty intervals are ignored. The source is never retained or modified.
     */
    public static M3IntIntervalSet copyOf(int[] rawPairs) {
        Objects.requireNonNull(rawPairs, "rawPairs");
        if ((rawPairs.length & 1) != 0) throw new IllegalArgumentException("odd interval endpoints");
        int count = rawPairs.length >>> 1;
        if (count > MAX_PAIRS) throw new IllegalArgumentException("interval set too large");
        long[] packed = new long[count];
        int occupied = 0;
        for (int i = 0; i < rawPairs.length; i += 2) {
            int start = rawPairs[i];
            int end = rawPairs[i + 1];
            if (start > end) throw new IllegalArgumentException("reversed interval");
            if (start != end) packed[occupied++] = ((long) start << 32) | (end & 0xffff_ffffL);
        }
        Arrays.sort(packed, 0, occupied);
        int[] output = new int[occupied * 2];
        int used = 0;
        for (int i = 0; i < occupied; i++) {
            int start = (int) (packed[i] >> 32);
            int end = (int) packed[i];
            if (used == 0 || start > output[used - 1]) {
                output[used++] = start;
                output[used++] = end;
            } else if (end > output[used - 1]) {
                output[used - 1] = end;
            }
        }
        return new M3IntIntervalSet(Arrays.copyOf(output, used));
    }

    public int intervalCount() { return pairs.length >>> 1; }

    /** Fresh caller-owned alternating [start,end) array. */
    public int[] toPairs() { return pairs.clone(); }

    /** Number of covered integers, up to 2^32-1; computed without int overflow. */
    public long coveredLength() {
        long length = 0;
        for (int i = 0; i < pairs.length; i += 2) {
            length += (long) pairs[i + 1] - pairs[i];
        }
        return length;
    }

    /** Signed-coordinate membership in O(log numberOfIntervals). */
    public boolean contains(int value) {
        int low = 0;
        int high = intervalCount();
        while (low < high) {
            int mid = low + ((high - low) >>> 1);
            if (pairs[mid * 2] <= value) low = mid + 1;
            else high = mid;
        }
        return low != 0 && value < pairs[(low - 1) * 2 + 1];
    }

    /** Exact covered cardinality within [startInclusive,endExclusive). */
    public long coveredLengthIn(int startInclusive, int endExclusive) {
        if (startInclusive > endExclusive) throw new IllegalArgumentException("reversed query");
        if (startInclusive == endExclusive) return 0;
        int low = 0;
        int high = intervalCount();
        while (low < high) {
            int mid = low + ((high - low) >>> 1);
            if (pairs[mid * 2 + 1] <= startInclusive) low = mid + 1;
            else high = mid;
        }
        long length = 0;
        for (int i = low; i < intervalCount() && pairs[2 * i] < endExclusive; i++) {
            int a = Math.max(startInclusive, pairs[2 * i]);
            int b = Math.min(endExclusive, pairs[2 * i + 1]);
            if (a < b) length += (long) b - a;
        }
        return length;
    }
}
