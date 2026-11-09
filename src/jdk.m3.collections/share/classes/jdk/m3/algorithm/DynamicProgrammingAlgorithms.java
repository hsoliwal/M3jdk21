// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import jdk.m3.algorithm.ProgressMonitor;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Objects;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Random;

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

    //FLLDCIM: FOLD a04e0a09f6de PrimitiveDynamicProgrammingAlgorithms.climbStairs
    public static long climbStairs(int steps) {
        if (steps < 0) throw new IllegalArgumentException("steps must be non-negative");
        if (steps <= 1) return 1L;
        long previous = 1L;
        long current = 1L;
        for (int i = 2; i <= steps; i++) {
            long next = Math.addExact(previous, current);
            previous = current;
            current = next;
        }
        return current;
    }

    //FLLDCIM: FOLD a04e0a09f6de PrimitiveDynamicProgrammingAlgorithms.minCostClimbingStairs
    public static int minCostClimbingStairs(int[] cost) {
        Objects.requireNonNull(cost, "cost");
        if (cost.length <= 1) return 0;
        long previousTwo = 0L;
        long previousOne = 0L;
        for (int i = 2; i <= cost.length; i++) {
            long fromOne = previousOne + cost[i - 1];
            long fromTwo = previousTwo + cost[i - 2];
            long current = Math.min(fromOne, fromTwo);
            previousTwo = previousOne;
            previousOne = current;
        }
        if (previousOne > Integer.MAX_VALUE) throw new ArithmeticException("cost overflow");
        return (int) previousOne;
    }

    //FLLDCIM: FOLD a04e0a09f6de PrimitiveDynamicProgrammingAlgorithms.coinChangeWays
    public static long coinChangeWays(int[] coins, int amount) {
        Objects.requireNonNull(coins, "coins");
        if (amount < 0) throw new IllegalArgumentException("amount must be non-negative");
        long[] dp = new long[amount + 1];
        dp[0] = 1L;
        for (int coin : coins) {
            if (coin <= 0) throw new IllegalArgumentException("coins must be positive");
            for (int value = coin; value <= amount; value++) {
                dp[value] = Math.addExact(dp[value], dp[value - coin]);
            }
        }
        return dp[amount];
    }

    //FLLDCIM: FOLD a04e0a09f6de PrimitiveDynamicProgrammingAlgorithms.minPathSum
    public static long minPathSum(int[][] grid) {
        validateRectangular(grid);
        int rows = grid.length;
        int columns = grid[0].length;
        long[] dp = new long[columns];
        dp[0] = grid[0][0];
        for (int column = 1; column < columns; column++) {
            dp[column] = Math.addExact(dp[column - 1], grid[0][column]);
        }
        for (int row = 1; row < rows; row++) {
            dp[0] = Math.addExact(dp[0], grid[row][0]);
            for (int column = 1; column < columns; column++) {
                dp[column] = Math.addExact(
                        Math.min(dp[column], dp[column - 1]),
                        grid[row][column]);
            }
        }
        return dp[columns - 1];
    }

    //FLLDCIM: FOLD a04e0a09f6de PrimitiveDynamicProgrammingAlgorithms.targetSumWays
    public static long targetSumWays(int[] values, int target) {
        Objects.requireNonNull(values, "values");
        int sum = 0;
        for (int value : values) {
            if (value < 0) throw new IllegalArgumentException("values must be non-negative");
            sum = Math.addExact(sum, value);
        }
        if (Math.abs((long) target) > sum) return 0L;
        long transformedLong = (long) sum + target;
        if ((transformedLong & 1L) != 0L || transformedLong < 0L) return 0L;
        long subsetLong = transformedLong / 2L;
        if (subsetLong > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("target subset sum too large");
        }
        int subset = (int) subsetLong;
        long[] dp = new long[subset + 1];
        dp[0] = 1L;
        for (int value : values) {
            for (int current = subset; current >= value; current--) {
                dp[current] = Math.addExact(dp[current], dp[current - value]);
            }
        }
        return dp[subset];
    }

    //FLLDCIM: FOLD a04e0a09f6de PrimitiveDynamicProgrammingAlgorithms.decodeWays
    public static int decodeWays(String digits) {
        Objects.requireNonNull(digits, "digits");
        if (digits.isEmpty()) return 0;
        for (int index = 0; index < digits.length(); index++) {
            if (!Character.isDigit(digits.charAt(index))) {
                throw new IllegalArgumentException("digits must contain only decimal digits");
            }
        }
        if (digits.charAt(0) == '0') return 0;
        int previousTwo = 1;
        int previousOne = 1;
        for (int i = 1; i < digits.length(); i++) {
            char currentChar = digits.charAt(i);
            char previousChar = digits.charAt(i - 1);
            int current = 0;
            if (currentChar != '0') current += previousOne;
            int two = (previousChar - '0') * 10 + (currentChar - '0');
            if (two >= 10 && two <= 26) current += previousTwo;
            previousTwo = previousOne;
            previousOne = current;
        }
        return previousOne;
    }

    //FLLDCIM: FOLD a04e0a09f6de PrimitiveDynamicProgrammingAlgorithms.validateRectangular
    private static void validateRectangular(int[][] grid) {
        Objects.requireNonNull(grid, "grid");
        if (grid.length == 0) throw new IllegalArgumentException("grid must not be empty");
        Objects.requireNonNull(grid[0], "grid row");
        if (grid[0].length == 0) throw new IllegalArgumentException("grid rows must not be empty");
        int columns = grid[0].length;
        for (int[] row : grid) {
            Objects.requireNonNull(row, "grid row");
            if (row.length != columns) throw new IllegalArgumentException("grid must be rectangular");
        }
    }

    //FLLDCIM: FOLD 84cab3ebbc04 IntLcsWitness.MAX_LENGTH
    private static final int LCS_WITNESS_MAX_LENGTH = 4_096;
    //FLLDCIM: FOLD 84cab3ebbc04 IntLcsWitness.MAX_CELLS
    private static final long LCS_WITNESS_MAX_CELLS = 8_388_608L;

    //FLLDCIM: FOLD 84cab3ebbc04 IntLcsWitness.admit
    private static void admitLcsWitness(int[] a, int[] b) {
        if (a == null || b == null) throw new NullPointerException("integer sequences");
        if (a.length > LCS_WITNESS_MAX_LENGTH || b.length > LCS_WITNESS_MAX_LENGTH
                || (long) a.length * b.length > LCS_WITNESS_MAX_CELLS) {
            throw new IllegalArgumentException("bounded integer LCS witnesses only");
        }
    }

    //FLLDCIM: FOLD 84cab3ebbc04 IntLcsWitness.javaWitness
    /** O(a.length*b.length) time and O(min(a.length,b.length)) work space, excluding output. */
    public static int[] lcsWitness(int[] a, int[] b) {
        admitLcsWitness(a, b);
        if (a.length == 0 || b.length == 0) return new int[0];
        // Hirschberg's second-dimension vector is chosen as the shorter input.
        // This swap does not change the accepted witness contract.
        int[] left = a.length >= b.length ? a : b;
        int[] right = a.length >= b.length ? b : a;
        int[] output = new int[right.length];
        int count = lcsWitnessBuild(left, 0, left.length, right, 0, right.length, output, 0);
        return Arrays.copyOf(output, count);
    }

    //FLLDCIM: FOLD 84cab3ebbc04 IntLcsWitness.split
    private static int lcsWitnessSplit(int[] a, int begin, int mid, int end,
                             int[] b, int bBegin, int bEnd) {
        int length = bEnd - bBegin;
        int[] forward = new int[length + 1];
        int[] backward = new int[length + 1];
        for (int ai = begin; ai < mid; ai++) {
            int northwest = 0;
            for (int j = 0; j < length; j++) {
                int prior = forward[j + 1];
                if (a[ai] == b[bBegin + j]) forward[j + 1] = northwest + 1;
                else forward[j + 1] = Math.max(forward[j], prior);
                northwest = prior;
            }
        }
        for (int ai = end - 1; ai >= mid; ai--) {
            int southeast = 0;
            for (int j = length - 1; j >= 0; j--) {
                int prior = backward[j];
                if (a[ai] == b[bBegin + j]) backward[j] = southeast + 1;
                else backward[j] = Math.max(prior, backward[j + 1]);
                southeast = prior;
            }
        }
        int best = -1;
        int index = 0;
        for (int j = 0; j <= length; j++) {
            int score = forward[j] + backward[j];
            // Earliest split in b breaks ties; no lexicographic claim.
            if (score > best) { best = score; index = j; }
        }
        return bBegin + index;
    }

    //FLLDCIM: FOLD 84cab3ebbc04 IntLcsWitness.build
    private static int lcsWitnessBuild(int[] a, int begin, int end, int[] b,
                             int bBegin, int bEnd, int[] output, int used) {
        if (begin == end || bBegin == bEnd) return used;
        if (end - begin == 1) {
            for (int j = bBegin; j < bEnd; j++) {
                if (a[begin] == b[j]) { output[used++] = a[begin]; break; }
            }
            return used;
        }
        int mid = begin + (end - begin) / 2;
        int cut = lcsWitnessSplit(a, begin, mid, end, b, bBegin, bEnd);
        int next = lcsWitnessBuild(a, begin, mid, b, bBegin, cut, output, used);
        return lcsWitnessBuild(a, mid, end, b, cut, bEnd, output, next);
    }

    //FLLDCIM: FOLD 84cab3ebbc04 IntLcsWitness.referenceLength
    /** Independent rolling-row length oracle, not derived from Hirschberg split logic. */
    public static int lcsLength(int[] a, int[] b) {
        admitLcsWitness(a, b);
        int[] shorter = a.length <= b.length ? a : b;
        int[] longer = a.length <= b.length ? b : a;
        int[] row = new int[shorter.length + 1];
        for (int value : longer) {
            int diagonal = 0;
            for (int j = 0; j < shorter.length; j++) {
                int old = row[j + 1];
                if (value == shorter[j]) row[j + 1] = diagonal + 1;
                else row[j + 1] = Math.max(row[j], old);
                diagonal = old;
            }
        }
        return row[shorter.length];
    }

    //FLLDCIM: FOLD 84cab3ebbc04 IntLcsWitness.isSubsequence
    /** Test-only subsequence predicate; input equality is exact signed int equality. */
    public static boolean isSubsequence(int[] needle, int[] haystack) {
        int found = 0;
        for (int value : haystack) if (found < needle.length && value == needle[found]) found++;
        return found == needle.length;
    }

}
