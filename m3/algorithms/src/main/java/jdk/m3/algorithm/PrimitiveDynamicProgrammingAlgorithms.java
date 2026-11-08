// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import java.util.Objects;

/** Canonical dynamic-programming shapes with bounded primitive state where practical. */
//FLLDCIM: RELOCATE - donor blob 43b24728c870 at commit 3385d79a2968 (repository, path, URL and digests: m3/algorithms/recipe/algorithm-extras-20261106/PROVENANCE.tsv); recipe m3/algorithms/recipe/algorithm-extras-20261106/relocate.py v1
public final class PrimitiveDynamicProgrammingAlgorithms {
    private PrimitiveDynamicProgrammingAlgorithms() {}

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
}
