// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

import java.util.Arrays;

/** Immutable sorted primitive multiset used by M3 precompute signals. */
final class M3Histogram {
    final long[] keys;
    final int[] counts;

    private M3Histogram(long[] keys, int[] counts) {
        this.keys = keys;
        this.counts = counts;
    }

    static M3Histogram of(long[] owned) {
        Arrays.sort(owned);
        int unique = 0;
        for (int index = 0; index < owned.length; index++) {
            if (index == 0 || owned[index] != owned[index - 1]) unique++;
        }
        long[] keys = new long[unique];
        int[] counts = new int[unique];
        int at = -1;
        for (long key : owned) {
            if (at < 0 || key != keys[at]) keys[++at] = key;
            counts[at]++;
        }
        return new M3Histogram(keys, counts);
    }

    M3Histogram plus(M3Histogram other) {
        long[] merged = new long[Math.addExact(keys.length, other.keys.length)];
        int[] amounts = new int[merged.length];
        int left = 0;
        int right = 0;
        int out = 0;
        while (left < keys.length || right < other.keys.length) {
            if (right == other.keys.length
                    || (left < keys.length && keys[left] < other.keys[right])) {
                merged[out] = keys[left];
                amounts[out++] = counts[left++];
            } else if (left == keys.length || other.keys[right] < keys[left]) {
                merged[out] = other.keys[right];
                amounts[out++] = other.counts[right++];
            } else {
                merged[out] = keys[left];
                amounts[out++] = Math.addExact(counts[left++], other.counts[right++]);
            }
        }
        return new M3Histogram(Arrays.copyOf(merged, out), Arrays.copyOf(amounts, out));
    }

    long l1(M3Histogram other) {
        int left = 0;
        int right = 0;
        long difference = 0;
        while (left < keys.length || right < other.keys.length) {
            if (right == other.keys.length
                    || (left < keys.length && keys[left] < other.keys[right])) {
                difference += counts[left++];
            } else if (left == keys.length || other.keys[right] < keys[left]) {
                difference += other.counts[right++];
            } else {
                difference += Math.abs((long) counts[left++] - other.counts[right++]);
            }
        }
        return difference;
    }

    int count(long key) {
        int index = Arrays.binarySearch(keys, key);
        return index < 0 ? 0 : counts[index];
    }

    boolean same(M3Histogram other) {
        return Arrays.equals(keys, other.keys) && Arrays.equals(counts, other.counts);
    }

    long payloadBytes() {
        return 12L * keys.length;
    }
}
