// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.m3.tools.cpu;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable, signed-order sorted integer postings represented by two primitive lanes:
 * unique values and their cumulative end-exclusive positions.
 *
 * <p>This is a compact index only when sufficient runs repeat: input length N and
 * distinct run count R require 8*R bytes of lane payload versus raw 4*N bytes.
 * Instances expose only detached arrays, and are safe for concurrent readers.
 */
public final class M3RunLengthPostingIndex {
    public static final int MAX_ITEMS = 2_000_000;

    private final int[] values;
    private final int[] ends;
    private final int total;

    private M3RunLengthPostingIndex(int[] values, int[] ends, int total) {
        this.values = values;
        this.ends = ends;
        this.total = total;
    }

    /** Snapshot and index a nondecreasing signed int lane; never retain the input. */
    public static M3RunLengthPostingIndex fromSorted(int[] source) {
        Objects.requireNonNull(source, "source");
        if (source.length > MAX_ITEMS) throw new IllegalArgumentException("postings too large");
        // Validate the private snapshot, not a mutable caller alias, in both passes.
        int[] snapshot = source.clone();
        int runs = 0;
        for (int i = 0; i < snapshot.length; i++) {
            if (i != 0 && snapshot[i] < snapshot[i - 1]) {
                throw new IllegalArgumentException("postings must be nondecreasing");
            }
            if (i == 0 || snapshot[i] != snapshot[i - 1]) runs++;
        }
        int[] unique = new int[runs];
        int[] cumulative = new int[runs];
        int r = 0;
        for (int i = 0; i < snapshot.length; i++) {
            if (i == 0 || snapshot[i] != snapshot[i - 1]) {
                unique[r] = snapshot[i];
                cumulative[r++] = i + 1;
            } else {
                cumulative[r - 1] = i + 1;
            }
        }
        return new M3RunLengthPostingIndex(unique, cumulative, snapshot.length);
    }

    /** Construct without expanding runs. Both caller arrays are validated and copied. */
    public static M3RunLengthPostingIndex fromRuns(int[] distinctValues, int[] multiplicities) {
        Objects.requireNonNull(distinctValues, "distinctValues");
        Objects.requireNonNull(multiplicities, "multiplicities");
        if (distinctValues.length > MAX_ITEMS || distinctValues.length != multiplicities.length) {
            throw new IllegalArgumentException("invalid run lanes");
        }
        int[] unique = distinctValues.clone();
        int[] counts = multiplicities.clone();
        long total = 0L;
        for (int i = 0; i < unique.length; i++) {
            if (i != 0 && unique[i - 1] >= unique[i]) {
                throw new IllegalArgumentException("run values must be strictly increasing");
            }
            if (counts[i] <= 0) throw new IllegalArgumentException("multiplicity must be positive");
            total += counts[i];
            if (total > MAX_ITEMS) throw new IllegalArgumentException("postings too large");
            counts[i] = (int) total;
        }
        return new M3RunLengthPostingIndex(unique, counts, (int) total);
    }

    public int size() { return total; }

    public int runCount() { return values.length; }

    /** Fresh copy of distinct signed values in strictly increasing order. */
    public int[] distinctValues() { return values.clone(); }

    /** Fresh copy of cumulative end-exclusive positions (one per distinct value). */
    public int[] runEndsExclusive() { return ends.clone(); }

    /** Reconstruct the exact original sorted lane, including duplicates. */
    public int[] toArray() {
        int[] output = new int[total];
        int start = 0;
        for (int i = 0; i < values.length; i++) {
            Arrays.fill(output, start, ends[i], values[i]);
            start = ends[i];
        }
        return output;
    }

    /** Number of postings strictly less than value (0..size). */
    public int lowerBound(int value) {
        int run = lowerRun(value);
        return run == 0 ? 0 : ends[run - 1];
    }

    /** Number of postings less than or equal to value (0..size). */
    public int upperBound(int value) {
        int run = upperRun(value);
        return run == 0 ? 0 : ends[run - 1];
    }

    /** Exact signed-int multiplicity, with no per-query array or object allocation. */
    public int countOf(int value) {
        int run = lowerRun(value);
        if (run == values.length || values[run] != value) return 0;
        return ends[run] - (run == 0 ? 0 : ends[run - 1]);
    }

    /** Retrieve value at a zero-based index in the uncompressed sorted lane. */
    public int select(int index) {
        if (index < 0 || index >= total) throw new IndexOutOfBoundsException("position: " + index);
        int lo = 0;
        int hi = ends.length;
        while (lo < hi) {
            int mid = lo + ((hi - lo) >>> 1);
            if (ends[mid] <= index) lo = mid + 1;
            else hi = mid;
        }
        return values[lo];
    }

    /** Count postings in [minInclusive,maxExclusive), with signed ordering. */
    public int rangeCount(int minInclusive, int maxExclusive) {
        if (minInclusive > maxExclusive) throw new IllegalArgumentException("inverted value range");
        if (minInclusive == maxExclusive) return 0;
        return lowerBound(maxExclusive) - lowerBound(minInclusive);
    }

    /** Count a value within positional window [beginInclusive,endExclusive). */
    public int countInPositions(int value, int beginInclusive, int endExclusive) {
        if (beginInclusive < 0 || beginInclusive > endExclusive || endExclusive > total) {
            throw new IndexOutOfBoundsException("invalid position window");
        }
        int run = lowerRun(value);
        if (run == values.length || values[run] != value) return 0;
        int start = run == 0 ? 0 : ends[run - 1];
        int end = ends[run];
        return Math.max(0, Math.min(end, endExclusive) - Math.max(start, beginInclusive));
    }

    private int lowerRun(int target) {
        int count = values.length;
        if (count == 0 || target <= values[0]) return 0;
        if (target > values[count - 1]) return count;
        int lo = 0;
        int hi = count;
        while (lo < hi) {
            int mid = lo + ((hi - lo) >>> 1);
            if (values[mid] < target) lo = mid + 1;
            else hi = mid;
        }
        return lo;
    }

    private int upperRun(int target) {
        int count = values.length;
        if (count == 0 || target < values[0]) return 0;
        if (target >= values[count - 1]) return count;
        int lo = 0;
        int hi = count;
        while (lo < hi) {
            int mid = lo + ((hi - lo) >>> 1);
            if (values[mid] <= target) lo = mid + 1;
            else hi = mid;
        }
        return lo;
    }

    /** Package-private; only the explicit optional JNI atom receives owned immutable lanes. */
    int[] ownedDistinctValues() { return values; }

    /** Package-private; only the explicit optional JNI atom receives owned immutable lanes. */
    int[] ownedRunEnds() { return ends; }
}
