// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import jdk.m3.algorithm.M3ProgressMonitor;

import java.util.Arrays;
import java.util.Objects;

/** Productionized primitive-array algorithms covering common interview/problem shapes. */
//FLLDCIM: RELOCATE - donor blob d84bb86d33fd at commit 3385d79a2968 (repository, path, URL and digests: m3/algorithms/recipe/algorithm-extras-20261106/PROVENANCE.tsv); recipe m3/algorithms/recipe/algorithm-extras-20261106/relocate.py v1
public final class M3PrimitiveArrayAlgorithms {
    private M3PrimitiveArrayAlgorithms() {}

    public static int binarySearch(int[] values, int target) {
        return binarySearch(values, target, M3ProgressMonitor.none());
    }

    public static int binarySearch(int[] values, int target, M3ProgressMonitor monitor) {
        Objects.requireNonNull(values, "values");
        M3ProgressMonitor progress = progress(monitor);
        progress.beginTask("binary-search", values.length);
        try {
            int low = 0;
            int high = values.length - 1;
            while (low <= high) {
                progress.checkCanceled();
                int middle = low + ((high - low) >>> 1);
                int value = values[middle];
                progress.worked(1);
                if (value < target) low = middle + 1;
                else if (value > target) high = middle - 1;
                else return middle;
            }
            return -1;
        } finally {
            progress.done();
        }
    }

    public static int[] nextGreaterCircular(int[] values) {
        Objects.requireNonNull(values, "values");
        int n = values.length;
        int[] output = new int[n];
        Arrays.fill(output, -1);
        int[] stack = new int[n];
        int size = 0;
        for (int i = 0; i < n * 2; i++) {
            int index = i % n;
            while (size > 0 && values[stack[size - 1]] < values[index]) {
                output[stack[--size]] = values[index];
            }
            if (i < n) stack[size++] = index;
        }
        return output;
    }

    public static long maxProductSubarray(int[] values) {
        Objects.requireNonNull(values, "values");
        if (values.length == 0) throw new IllegalArgumentException("values must not be empty");
        long maximum = values[0];
        long minimum = values[0];
        long best = values[0];
        for (int i = 1; i < values.length; i++) {
            long value = values[i];
            if (value < 0) {
                long swap = maximum;
                maximum = minimum;
                minimum = swap;
            }
            maximum = Math.max(value, maximum * value);
            minimum = Math.min(value, minimum * value);
            best = Math.max(best, maximum);
        }
        return best;
    }

    public static long maxProfitOneTransaction(int[] prices) {
        Objects.requireNonNull(prices, "prices");
        if (prices.length < 2) return 0L;
        int minimum = prices[0];
        long best = 0L;
        for (int i = 1; i < prices.length; i++) {
            best = Math.max(best, (long) prices[i] - minimum);
            minimum = Math.min(minimum, prices[i]);
        }
        return best;
    }

    public static int majorityElement(int[] values) {
        Objects.requireNonNull(values, "values");
        if (values.length == 0) throw new IllegalArgumentException("values must not be empty");
        int candidate = 0;
        int count = 0;
        for (int value : values) {
            if (count == 0) candidate = value;
            count += value == candidate ? 1 : -1;
        }
        int occurrences = 0;
        for (int value : values) if (value == candidate) occurrences++;
        if (occurrences <= values.length / 2) {
            throw new IllegalArgumentException("no strict majority element");
        }
        return candidate;
    }

    public static int[] countBits(int n) {
        if (n < 0) throw new IllegalArgumentException("n must be non-negative");
        int[] output = new int[n + 1];
        for (int i = 1; i <= n; i++) output[i] = output[i >>> 1] + (i & 1);
        return output;
    }

    public static int findMinRotated(int[] values) {
        Objects.requireNonNull(values, "values");
        if (values.length == 0) throw new IllegalArgumentException("values must not be empty");
        int low = 0;
        int high = values.length - 1;
        while (low < high) {
            int middle = low + ((high - low) >>> 1);
            if (values[middle] > values[high]) low = middle + 1;
            else high = middle;
        }
        return values[low];
    }

    public static int findDuplicateFloyd(int[] values) {
        Objects.requireNonNull(values, "values");
        if (values.length < 2) throw new IllegalArgumentException("requires n+1 values");
        int n = values.length - 1;
        for (int value : values) {
            if (value < 1 || value > n) {
                throw new IllegalArgumentException("values must be in [1,n]");
            }
        }
        int slow = values[0];
        int fast = values[0];
        do {
            slow = values[slow];
            fast = values[values[fast]];
        } while (slow != fast);
        slow = values[0];
        while (slow != fast) {
            slow = values[slow];
            fast = values[fast];
        }
        return slow;
    }

    public static long containerMostWater(int[] heights) {
        Objects.requireNonNull(heights, "heights");
        int left = 0;
        int right = heights.length - 1;
        long best = 0L;
        while (left < right) {
            int height = Math.min(heights[left], heights[right]);
            best = Math.max(best, (long) height * (right - left));
            if (heights[left] <= heights[right]) left++;
            else right--;
        }
        return best;
    }

    public static void moveZeroes(int[] values) {
        Objects.requireNonNull(values, "values");
        int write = 0;
        for (int value : values) if (value != 0) values[write++] = value;
        while (write < values.length) values[write++] = 0;
    }

    public static void rotateRight(int[] values, int steps) {
        Objects.requireNonNull(values, "values");
        if (values.length == 0) return;
        int normalized = Math.floorMod(steps, values.length);
        if (normalized == 0) return;
        reverse(values, 0, values.length - 1);
        reverse(values, 0, normalized - 1);
        reverse(values, normalized, values.length - 1);
    }

    private static void reverse(int[] values, int left, int right) {
        while (left < right) swap(values, left++, right--);
    }

    private static void swap(int[] values, int left, int right) {
        int value = values[left];
        values[left] = values[right];
        values[right] = value;
    }

    private static M3ProgressMonitor progress(M3ProgressMonitor monitor) {
        return monitor == null ? M3ProgressMonitor.none() : monitor;
    }
}
