// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import jdk.m3.algorithm.ProgressMonitor;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.IntPredicate;
import java.util.Objects;

//FLLDCIM: RELOCATE - donor blob d707fd5060a4 at commit 3385d79a2968 (repository, path, URL and digests: m3/algorithms/recipe/families-20261106/PROVENANCE.tsv); recipe m3/algorithms/recipe/families-20261106/relocate.py v1
public final class ArrayAlgorithms {
    private ArrayAlgorithms() {
    }

    public static int[] twoSum(int[] values, int target, ProgressMonitor monitor) {
        AlgorithmSupport.begin(monitor, "two-sum", values.length);
        try {
            Map<Integer, Integer> seen = new HashMap<>();
            for (int index = 0; index < values.length; index++) {
                AlgorithmSupport.tick(monitor);
                long complement = (long) target - values[index];
                Integer match = complement >= Integer.MIN_VALUE
                        && complement <= Integer.MAX_VALUE
                        ? seen.get((int) complement) : null;
                if (match != null) {
                    return new int[] {match, index};
                }
                seen.putIfAbsent(values[index], index);
            }
            return AlgorithmSupport.emptyPair();
        } finally {
            monitor.done();
        }
    }

    public static long maxSubarray(int[] values, ProgressMonitor monitor) {
        if (values.length == 0) {
            throw new IllegalArgumentException("values must not be empty");
        }
        AlgorithmSupport.begin(monitor, "kadane-max-subarray", values.length);
        try {
            long best = values[0];
            long current = values[0];
            monitor.worked(1);
            for (int index = 1; index < values.length; index++) {
                AlgorithmSupport.tick(monitor);
                current = Math.max(values[index], current + values[index]);
                best = Math.max(best, current);
            }
            return best;
        } finally {
            monitor.done();
        }
    }

    public static long[] productExceptSelf(int[] values, ProgressMonitor monitor) {
        AlgorithmSupport.begin(monitor, "product-except-self", (long) values.length * 2L);
        try {
            long[] result = new long[values.length];
            long prefix = 1L;
            for (int index = 0; index < values.length; index++) {
                AlgorithmSupport.tick(monitor);
                result[index] = prefix;
                prefix *= values[index];
            }
            long suffix = 1L;
            for (int index = values.length - 1; index >= 0; index--) {
                AlgorithmSupport.tick(monitor);
                result[index] *= suffix;
                suffix *= values[index];
            }
            return result;
        } finally {
            monitor.done();
        }
    }

    public static int longestConsecutive(int[] values, ProgressMonitor monitor) {
        AlgorithmSupport.begin(monitor, "longest-consecutive", (long) values.length * 2L);
        try {
            Set<Integer> set = new HashSet<>();
            for (int value : values) {
                AlgorithmSupport.tick(monitor);
                set.add(value);
            }
            int best = 0;
            for (int value : set) {
                AlgorithmSupport.tick(monitor);
                if (value != Integer.MIN_VALUE && set.contains(value - 1)) {
                    continue;
                }
                int length = 1;
                int current = value;
                while (current != Integer.MAX_VALUE && set.contains(current + 1)) {
                    monitor.checkCanceled();
                    current++;
                    length++;
                }
                best = Math.max(best, length);
            }
            return best;
        } finally {
            monitor.done();
        }
    }

    public static void sortColors(int[] values, ProgressMonitor monitor) {
        AlgorithmSupport.begin(monitor, "dutch-national-flag", values.length);
        try {
            int low = 0;
            int scan = 0;
            int high = values.length - 1;
            while (scan <= high) {
                AlgorithmSupport.tick(monitor);
                if (values[scan] == 0) {
                    swap(values, low++, scan++);
                } else if (values[scan] == 2) {
                    swap(values, scan, high--);
                } else if (values[scan] == 1) {
                    scan++;
                } else {
                    throw new IllegalArgumentException("expected only 0, 1, 2");
                }
            }
        } finally {
            monitor.done();
        }
    }

    public static List<List<Integer>> threeSum(int[] values, ProgressMonitor monitor) {
        int[] sorted = values.clone();
        Arrays.sort(sorted);
        AlgorithmSupport.begin(monitor, "three-sum", (long) sorted.length * sorted.length);
        try {
            List<List<Integer>> result = new ArrayList<>();
            for (int first = 0; first + 2 < sorted.length; first++) {
                monitor.checkCanceled();
                if (first > 0 && sorted[first] == sorted[first - 1]) {
                    continue;
                }
                int left = first + 1;
                int right = sorted.length - 1;
                while (left < right) {
                    AlgorithmSupport.tick(monitor);
                    long sum = (long) sorted[first] + sorted[left] + sorted[right];
                    if (sum == 0L) {
                        result.add(List.of(sorted[first], sorted[left], sorted[right]));
                        int leftValue = sorted[left];
                        int rightValue = sorted[right];
                        while (left < right && sorted[left] == leftValue) {
                            left++;
                        }
                        while (left < right && sorted[right] == rightValue) {
                            right--;
                        }
                    } else if (sum < 0L) {
                        left++;
                    } else {
                        right--;
                    }
                }
            }
            return List.copyOf(result);
        } finally {
            monitor.done();
        }
    }

    public static long trapRainWater(int[] height, ProgressMonitor monitor) {
        AlgorithmSupport.begin(monitor, "trap-rain-water", height.length);
        try {
            int left = 0;
            int right = height.length - 1;
            int leftMax = 0;
            int rightMax = 0;
            long water = 0L;
            while (left <= right) {
                AlgorithmSupport.tick(monitor);
                if (leftMax <= rightMax) {
                    leftMax = Math.max(leftMax, height[left]);
                    water += leftMax - height[left++];
                } else {
                    rightMax = Math.max(rightMax, height[right]);
                    water += rightMax - height[right--];
                }
            }
            return water;
        } finally {
            monitor.done();
        }
    }

    public static int longestDistinctSubstring(CharSequence input, ProgressMonitor monitor) {
        int[] codePoints = input.toString().codePoints().toArray();
        AlgorithmSupport.begin(monitor, "longest-distinct-window", codePoints.length);
        try {
            Map<Integer, Integer> last = new HashMap<>();
            int start = 0;
            int best = 0;
            for (int end = 0; end < codePoints.length; end++) {
                AlgorithmSupport.tick(monitor);
                Integer previous = last.put(codePoints[end], end);
                if (previous != null && previous >= start) {
                    start = previous + 1;
                }
                best = Math.max(best, end - start + 1);
            }
            return best;
        } finally {
            monitor.done();
        }
    }

    public static int[] maxSlidingWindow(
            int[] values,
            int window,
            ProgressMonitor monitor) {
        if (window <= 0 || window > values.length) {
            throw new IllegalArgumentException("invalid window size");
        }
        AlgorithmSupport.begin(monitor, "sliding-window-maximum", values.length);
        try {
            int[] result = new int[values.length - window + 1];
            ArrayDeque<Integer> deque = new ArrayDeque<>();
            for (int index = 0; index < values.length; index++) {
                AlgorithmSupport.tick(monitor);
                while (!deque.isEmpty() && deque.getFirst() <= index - window) {
                    deque.removeFirst();
                }
                while (!deque.isEmpty() && values[deque.getLast()] <= values[index]) {
                    deque.removeLast();
                }
                deque.addLast(index);
                if (index + 1 >= window) {
                    result[index - window + 1] = values[deque.getFirst()];
                }
            }
            return result;
        } finally {
            monitor.done();
        }
    }

    public static long subarraySumEqualsK(
            int[] values,
            long target,
            ProgressMonitor monitor) {
        AlgorithmSupport.begin(monitor, "prefix-sum-frequency", values.length);
        try {
            Map<Long, Integer> counts = new HashMap<>();
            counts.put(0L, 1);
            long prefix = 0L;
            long result = 0L;
            for (int value : values) {
                AlgorithmSupport.tick(monitor);
                prefix += value;
                result += counts.getOrDefault(prefix - target, 0);
                counts.merge(prefix, 1, Integer::sum);
            }
            return result;
        } finally {
            monitor.done();
        }
    }

    public static int lowerBound(int[] sorted, int target, ProgressMonitor monitor) {
        AlgorithmSupport.begin(monitor, "lower-bound", 64);
        try {
            int low = 0;
            int high = sorted.length;
            while (low < high) {
                AlgorithmSupport.tick(monitor);
                int middle = low + ((high - low) >>> 1);
                if (sorted[middle] < target) {
                    low = middle + 1;
                } else {
                    high = middle;
                }
            }
            return low;
        } finally {
            monitor.done();
        }
    }

    public static int upperBound(int[] sorted, int target, ProgressMonitor monitor) {
        AlgorithmSupport.begin(monitor, "upper-bound", 64);
        try {
            int low = 0;
            int high = sorted.length;
            while (low < high) {
                AlgorithmSupport.tick(monitor);
                int middle = low + ((high - low) >>> 1);
                if (sorted[middle] <= target) {
                    low = middle + 1;
                } else {
                    high = middle;
                }
            }
            return low;
        } finally {
            monitor.done();
        }
    }

    public static int searchRotated(int[] values, int target, ProgressMonitor monitor) {
        AlgorithmSupport.begin(monitor, "rotated-binary-search", 64);
        try {
            int low = 0;
            int high = values.length - 1;
            while (low <= high) {
                AlgorithmSupport.tick(monitor);
                int middle = low + ((high - low) >>> 1);
                if (values[middle] == target) {
                    return middle;
                }
                if (values[low] <= values[middle]) {
                    if (values[low] <= target && target < values[middle]) {
                        high = middle - 1;
                    } else {
                        low = middle + 1;
                    }
                } else if (values[middle] < target && target <= values[high]) {
                    low = middle + 1;
                } else {
                    high = middle - 1;
                }
            }
            return -1;
        } finally {
            monitor.done();
        }
    }

    public static List<Integer> topKFrequent(
            int[] values,
            int k,
            ProgressMonitor monitor) {
        if (k < 0) {
            throw new IllegalArgumentException("k must be non-negative");
        }
        AlgorithmSupport.begin(monitor, "top-k-frequent", (long) values.length * 2L);
        try {
            Map<Integer, Integer> counts = new HashMap<>();
            for (int value : values) {
                AlgorithmSupport.tick(monitor);
                counts.merge(value, 1, Integer::sum);
            }
            if (k > counts.size()) {
                throw new IllegalArgumentException("k exceeds unique value count");
            }
            PriorityQueue<Map.Entry<Integer, Integer>> heap =
                    new PriorityQueue<>((left, right) -> {
                        int byCount = Integer.compare(left.getValue(), right.getValue());
                        return byCount != 0
                                ? byCount
                                : Integer.compare(right.getKey(), left.getKey());
                    });
            for (Map.Entry<Integer, Integer> entry : counts.entrySet()) {
                AlgorithmSupport.tick(monitor);
                heap.add(entry);
                if (heap.size() > k) {
                    heap.remove();
                }
            }
            List<Integer> result = new ArrayList<>(k);
            while (!heap.isEmpty()) {
                result.add(heap.remove().getKey());
            }
            java.util.Collections.reverse(result);
            return List.copyOf(result);
        } finally {
            monitor.done();
        }
    }


    public static int[] sortedTwoSum(
            int[] sorted,
            int target,
            ProgressMonitor monitor) {
        AlgorithmSupport.begin(monitor, "sorted-two-sum", sorted.length);
        try {
            int left = 0;
            int right = sorted.length - 1;
            while (left < right) {
                AlgorithmSupport.tick(monitor);
                long sum = (long) sorted[left] + sorted[right];
                if (sum == target) {
                    return new int[] {left, right};
                }
                if (sum < target) {
                    left++;
                } else {
                    right--;
                }
            }
            return AlgorithmSupport.emptyPair();
        } finally {
            monitor.done();
        }
    }

    public static int minSubarrayLengthAtLeast(
            long target,
            int[] nonNegativeValues,
            ProgressMonitor monitor) {
        if (target <= 0L) {
            return 0;
        }
        AlgorithmSupport.begin(monitor, "minimum-subarray-window", nonNegativeValues.length);
        try {
            int start = 0;
            int best = Integer.MAX_VALUE;
            long sum = 0L;
            for (int end = 0; end < nonNegativeValues.length; end++) {
                AlgorithmSupport.tick(monitor);
                if (nonNegativeValues[end] < 0) {
                    throw new IllegalArgumentException(
                            "minimum sliding-window shape requires non-negative values");
                }
                sum += nonNegativeValues[end];
                while (sum >= target) {
                    monitor.checkCanceled();
                    best = Math.min(best, end - start + 1);
                    sum -= nonNegativeValues[start++];
                }
            }
            return best == Integer.MAX_VALUE ? 0 : best;
        } finally {
            monitor.done();
        }
    }

    public static long[] prefixSums(int[] values, ProgressMonitor monitor) {
        AlgorithmSupport.begin(monitor, "prefix-sums", values.length);
        try {
            long[] prefix = new long[values.length + 1];
            for (int index = 0; index < values.length; index++) {
                AlgorithmSupport.tick(monitor);
                prefix[index + 1] = prefix[index] + values[index];
            }
            return prefix;
        } finally {
            monitor.done();
        }
    }

    public static long rangeSum(
            long[] prefixSums,
            int fromInclusive,
            int toExclusive,
            ProgressMonitor monitor) {
        monitor.checkCanceled();
        if (fromInclusive < 0
                || toExclusive < fromInclusive
                || toExclusive >= prefixSums.length) {
            throw new IndexOutOfBoundsException(
                    fromInclusive + ".." + toExclusive);
        }
        return prefixSums[toExclusive] - prefixSums[fromInclusive];
    }

    public static int pivotIndex(int[] values, ProgressMonitor monitor) {
        AlgorithmSupport.begin(monitor, "pivot-index", values.length);
        try {
            long total = 0L;
            for (int value : values) {
                total += value;
            }
            long left = 0L;
            for (int index = 0; index < values.length; index++) {
                AlgorithmSupport.tick(monitor);
                long right = total - left - values[index];
                if (left == right) {
                    return index;
                }
                left += values[index];
            }
            return -1;
        } finally {
            monitor.done();
        }
    }

    public static int firstTrue(
            int lowInclusive,
            int highExclusive,
            IntPredicate predicate,
            ProgressMonitor monitor) {
        java.util.Objects.requireNonNull(predicate, "predicate");
        if (highExclusive < lowInclusive) {
            throw new IllegalArgumentException("highExclusive < lowInclusive");
        }
        AlgorithmSupport.begin(monitor, "binary-search-first-true", 64);
        try {
            int low = lowInclusive;
            int high = highExclusive;
            while (low < high) {
                AlgorithmSupport.tick(monitor);
                int middle = low + ((high - low) >>> 1);
                if (predicate.test(middle)) {
                    high = middle;
                } else {
                    low = middle + 1;
                }
            }
            return low;
        } finally {
            monitor.done();
        }
    }

    private static void swap(int[] values, int left, int right) {
        int temporary = values[left];
        values[left] = values[right];
        values[right] = temporary;
    }

    //FLLDCIM: FOLD - member PrimitiveArrayAlgorithms.binarySearch from donor blob 4d8f839e45a6 (m3/algorithms/src/main/java/jdk/m3/algorithm/PrimitiveArrayAlgorithms.java); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/primitive-array-fold/FOLD_MAP.tsv
    public static int binarySearch(int[] values, int target) {
        return binarySearch(values, target, ProgressMonitor.none());
    }

    //FLLDCIM: FOLD - member PrimitiveArrayAlgorithms.binarySearch from donor blob 4d8f839e45a6 (m3/algorithms/src/main/java/jdk/m3/algorithm/PrimitiveArrayAlgorithms.java); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/primitive-array-fold/FOLD_MAP.tsv
    public static int binarySearch(int[] values, int target, ProgressMonitor monitor) {
        Objects.requireNonNull(values, "values");
        ProgressMonitor progress = progress(monitor);
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

    //FLLDCIM: FOLD - member PrimitiveArrayAlgorithms.nextGreaterCircular from donor blob 4d8f839e45a6 (m3/algorithms/src/main/java/jdk/m3/algorithm/PrimitiveArrayAlgorithms.java); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/primitive-array-fold/FOLD_MAP.tsv
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

    //FLLDCIM: FOLD - member PrimitiveArrayAlgorithms.maxProductSubarray from donor blob 4d8f839e45a6 (m3/algorithms/src/main/java/jdk/m3/algorithm/PrimitiveArrayAlgorithms.java); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/primitive-array-fold/FOLD_MAP.tsv
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

    //FLLDCIM: FOLD - member PrimitiveArrayAlgorithms.maxProfitOneTransaction from donor blob 4d8f839e45a6 (m3/algorithms/src/main/java/jdk/m3/algorithm/PrimitiveArrayAlgorithms.java); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/primitive-array-fold/FOLD_MAP.tsv
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

    //FLLDCIM: FOLD - member PrimitiveArrayAlgorithms.majorityElement from donor blob 4d8f839e45a6 (m3/algorithms/src/main/java/jdk/m3/algorithm/PrimitiveArrayAlgorithms.java); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/primitive-array-fold/FOLD_MAP.tsv
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

    //FLLDCIM: FOLD - member PrimitiveArrayAlgorithms.countBits from donor blob 4d8f839e45a6 (m3/algorithms/src/main/java/jdk/m3/algorithm/PrimitiveArrayAlgorithms.java); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/primitive-array-fold/FOLD_MAP.tsv
    public static int[] countBits(int n) {
        if (n < 0) throw new IllegalArgumentException("n must be non-negative");
        int[] output = new int[n + 1];
        for (int i = 1; i <= n; i++) output[i] = output[i >>> 1] + (i & 1);
        return output;
    }

    //FLLDCIM: FOLD - member PrimitiveArrayAlgorithms.findMinRotated from donor blob 4d8f839e45a6 (m3/algorithms/src/main/java/jdk/m3/algorithm/PrimitiveArrayAlgorithms.java); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/primitive-array-fold/FOLD_MAP.tsv
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

    //FLLDCIM: FOLD - member PrimitiveArrayAlgorithms.findDuplicateFloyd from donor blob 4d8f839e45a6 (m3/algorithms/src/main/java/jdk/m3/algorithm/PrimitiveArrayAlgorithms.java); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/primitive-array-fold/FOLD_MAP.tsv
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

    //FLLDCIM: FOLD - member PrimitiveArrayAlgorithms.containerMostWater from donor blob 4d8f839e45a6 (m3/algorithms/src/main/java/jdk/m3/algorithm/PrimitiveArrayAlgorithms.java); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/primitive-array-fold/FOLD_MAP.tsv
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

    //FLLDCIM: FOLD - member PrimitiveArrayAlgorithms.moveZeroes from donor blob 4d8f839e45a6 (m3/algorithms/src/main/java/jdk/m3/algorithm/PrimitiveArrayAlgorithms.java); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/primitive-array-fold/FOLD_MAP.tsv
    public static void moveZeroes(int[] values) {
        Objects.requireNonNull(values, "values");
        int write = 0;
        for (int value : values) if (value != 0) values[write++] = value;
        while (write < values.length) values[write++] = 0;
    }

    //FLLDCIM: FOLD - member PrimitiveArrayAlgorithms.rotateRight from donor blob 4d8f839e45a6 (m3/algorithms/src/main/java/jdk/m3/algorithm/PrimitiveArrayAlgorithms.java); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/primitive-array-fold/FOLD_MAP.tsv
    public static void rotateRight(int[] values, int steps) {
        Objects.requireNonNull(values, "values");
        if (values.length == 0) return;
        int normalized = Math.floorMod(steps, values.length);
        if (normalized == 0) return;
        reverse(values, 0, values.length - 1);
        reverse(values, 0, normalized - 1);
        reverse(values, normalized, values.length - 1);
    }

    //FLLDCIM: FOLD - member PrimitiveArrayAlgorithms.reverse from donor blob 4d8f839e45a6 (m3/algorithms/src/main/java/jdk/m3/algorithm/PrimitiveArrayAlgorithms.java); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/primitive-array-fold/FOLD_MAP.tsv
    private static void reverse(int[] values, int left, int right) {
        while (left < right) swap(values, left++, right--);
    }

    //FLLDCIM: FOLD - member PrimitiveArrayAlgorithms.progress from donor blob 4d8f839e45a6 (m3/algorithms/src/main/java/jdk/m3/algorithm/PrimitiveArrayAlgorithms.java); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/primitive-array-fold/FOLD_MAP.tsv
    private static ProgressMonitor progress(ProgressMonitor monitor) {
        return monitor == null ? ProgressMonitor.none() : monitor;
    }
}
