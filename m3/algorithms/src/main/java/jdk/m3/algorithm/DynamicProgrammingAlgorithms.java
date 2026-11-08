// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import jdk.m3.algorithm.ProgressMonitor;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

//FLLDCIM: RELOCATE - donor blob 233ca9858e0e at commit 3385d79a2968 (repository, path, URL and digests: m3/algorithms/recipe/families-20261106/PROVENANCE.tsv); recipe m3/algorithms/recipe/families-20261106/relocate.py v1
public final class DynamicProgrammingAlgorithms {
    private DynamicProgrammingAlgorithms() {
    }

    public static int coinChange(int[] coins, int amount, ProgressMonitor monitor) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount must be non-negative");
        }
        AlgorithmSupport.begin(monitor, "coin-change", (long) coins.length * (amount + 1L));
        try {
            int sentinel = amount + 1;
            int[] dp = new int[amount + 1];
            Arrays.fill(dp, sentinel);
            dp[0] = 0;
            for (int coin : coins) {
                if (coin <= 0) {
                    throw new IllegalArgumentException("coin values must be positive");
                }
                for (int value = coin; value <= amount; value++) {
                    AlgorithmSupport.tick(monitor);
                    dp[value] = Math.min(dp[value], dp[value - coin] + 1);
                }
            }
            return dp[amount] == sentinel ? -1 : dp[amount];
        } finally {
            monitor.done();
        }
    }

    public static int longestIncreasingSubsequence(int[] values, ProgressMonitor monitor) {
        AlgorithmSupport.begin(monitor, "longest-increasing-subsequence", values.length);
        try {
            int[] tails = new int[values.length];
            int length = 0;
            for (int value : values) {
                AlgorithmSupport.tick(monitor);
                int low = 0;
                int high = length;
                while (low < high) {
                    monitor.checkCanceled();
                    int middle = low + ((high - low) >>> 1);
                    if (tails[middle] < value) {
                        low = middle + 1;
                    } else {
                        high = middle;
                    }
                }
                tails[low] = value;
                if (low == length) {
                    length++;
                }
            }
            return length;
        } finally {
            monitor.done();
        }
    }

    public static int longestCommonSubsequence(
            CharSequence left,
            CharSequence right,
            ProgressMonitor monitor) {
        if (right.length() > left.length()) {
            return longestCommonSubsequence(right, left, monitor);
        }
        AlgorithmSupport.begin(monitor, "longest-common-subsequence",
                (long) left.length() * right.length());
        try {
            if (left instanceof String leftString && right instanceof String rightString) {
                return immutableLcs(leftString, rightString, monitor);
            }
            int[] previous = new int[right.length() + 1];
            int[] current = new int[right.length() + 1];
            for (int i = 1; i <= left.length(); i++) {
                for (int j = 1; j <= right.length(); j++) {
                    AlgorithmSupport.tick(monitor);
                    current[j] = left.charAt(i - 1) == right.charAt(j - 1)
                            ? previous[j - 1] + 1
                            : Math.max(previous[j], current[j - 1]);
                }
                int[] temporary = previous;
                previous = current;
                current = temporary;
                Arrays.fill(current, 0);
            }
            return previous[right.length()];
        } finally {
            monitor.done();
        }
    }

    /** One rolling row for immutable UTF-16 strings; caller owns begin/done. */
    private static int immutableLcs(String left, String right, ProgressMonitor monitor) {
        int width = right.length();
        int[] row = new int[width + 1];
        for (int i = 0; i < left.length(); i++) {
            char character = left.charAt(i);
            int diagonal = 0;
            for (int j = 1; j <= width; j++) {
                AlgorithmSupport.tick(monitor);
                int above = row[j];
                row[j] = character == right.charAt(j - 1)
                        ? diagonal + 1 : Math.max(above, row[j - 1]);
                diagonal = above;
            }
        }
        return row[width];
    }

    public static int editDistance(
            CharSequence left,
            CharSequence right,
            ProgressMonitor monitor) {
        if (right.length() > left.length()) {
            return editDistance(right, left, monitor);
        }
        AlgorithmSupport.begin(monitor, "edit-distance",
                (long) left.length() * right.length());
        try {
            int[] previous = new int[right.length() + 1];
            int[] current = new int[right.length() + 1];
            for (int j = 0; j <= right.length(); j++) {
                previous[j] = j;
            }
            for (int i = 1; i <= left.length(); i++) {
                current[0] = i;
                for (int j = 1; j <= right.length(); j++) {
                    AlgorithmSupport.tick(monitor);
                    int replace = previous[j - 1]
                            + (left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1);
                    int delete = previous[j] + 1;
                    int insert = current[j - 1] + 1;
                    current[j] = Math.min(replace, Math.min(delete, insert));
                }
                int[] temporary = previous;
                previous = current;
                current = temporary;
            }
            return previous[right.length()];
        } finally {
            monitor.done();
        }
    }

    public static long houseRobber(int[] values, ProgressMonitor monitor) {
        AlgorithmSupport.begin(monitor, "house-robber", values.length);
        try {
            long previousTwo = 0L;
            long previousOne = 0L;
            for (int value : values) {
                AlgorithmSupport.tick(monitor);
                long current = Math.max(previousOne, previousTwo + value);
                previousTwo = previousOne;
                previousOne = current;
            }
            return previousOne;
        } finally {
            monitor.done();
        }
    }

    public static boolean canPartition(int[] values, ProgressMonitor monitor) {
        long sum = 0L;
        for (int value : values) {
            if (value < 0) {
                throw new IllegalArgumentException("partition values must be non-negative");
            }
            sum += value;
        }
        if ((sum & 1L) != 0L || sum / 2L > Integer.MAX_VALUE) {
            return false;
        }
        int target = (int) (sum / 2L);
        AlgorithmSupport.begin(monitor, "equal-subset-partition",
                (long) values.length * (target + 1L));
        try {
            boolean[] possible = new boolean[target + 1];
            possible[0] = true;
            for (int value : values) {
                for (int total = target; total >= value; total--) {
                    AlgorithmSupport.tick(monitor);
                    possible[total] |= possible[total - value];
                }
            }
            return possible[target];
        } finally {
            monitor.done();
        }
    }


    public static boolean wordBreak(
            String value,
            List<String> dictionary,
            ProgressMonitor monitor) {
        Set<String> words = new HashSet<>(dictionary);
        int maxLength = words.stream().mapToInt(String::length).max().orElse(0);
        AlgorithmSupport.begin(monitor, "word-break", (long) value.length() * Math.max(1, maxLength));
        try {
            boolean[] reachable = new boolean[value.length() + 1];
            reachable[0] = true;
            for (int end = 1; end <= value.length(); end++) {
                int startMin = Math.max(0, end - maxLength);
                for (int start = end - 1; start >= startMin; start--) {
                    AlgorithmSupport.tick(monitor);
                    if (reachable[start] && words.contains(value.substring(start, end))) {
                        reachable[end] = true;
                        break;
                    }
                }
            }
            return reachable[value.length()];
        } finally {
            monitor.done();
        }
    }

    public static long uniquePaths(
            int rows,
            int columns,
            ProgressMonitor monitor) {
        if (rows <= 0 || columns <= 0) {
            throw new IllegalArgumentException("rows/columns must be positive");
        }
        AlgorithmSupport.begin(monitor, "unique-grid-paths", (long) rows * columns);
        try {
            long[] dp = new long[columns];
            Arrays.fill(dp, 1L);
            for (int row = 1; row < rows; row++) {
                for (int column = 1; column < columns; column++) {
                    AlgorithmSupport.tick(monitor);
                    dp[column] = Math.addExact(dp[column], dp[column - 1]);
                }
            }
            return dp[columns - 1];
        } finally {
            monitor.done();
        }
    }

    public static long knapsack01(
            int[] weights,
            int[] values,
            int capacity,
            ProgressMonitor monitor) {
        if (weights.length != values.length || capacity < 0) {
            throw new IllegalArgumentException("invalid knapsack inputs");
        }
        AlgorithmSupport.begin(monitor, "zero-one-knapsack",
                (long) weights.length * (capacity + 1L));
        try {
            long[] dp = new long[capacity + 1];
            for (int item = 0; item < weights.length; item++) {
                if (weights[item] < 0) {
                    throw new IllegalArgumentException("weights must be non-negative");
                }
                for (int current = capacity; current >= weights[item]; current--) {
                    AlgorithmSupport.tick(monitor);
                    dp[current] = Math.max(
                            dp[current],
                            dp[current - weights[item]] + values[item]);
                }
            }
            return dp[capacity];
        } finally {
            monitor.done();
        }
    }
}
