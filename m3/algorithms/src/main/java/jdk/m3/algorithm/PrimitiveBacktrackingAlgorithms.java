// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** Backtracking shapes with explicit mutable primitive work buffers. */
//FLLDCIM: RELOCATE - donor blob 9f34c9fdfbd3 at commit 3385d79a2968 (repository, path, URL and digests: m3/algorithms/recipe/algorithm-extras-20261106/PROVENANCE.tsv); recipe m3/algorithms/recipe/algorithm-extras-20261106/relocate.py v1
public final class PrimitiveBacktrackingAlgorithms {
    private PrimitiveBacktrackingAlgorithms() {}

    public static int[][] combinations(int n, int k) {
        if (n < 0 || k < 0 || k > n) throw new IllegalArgumentException("requires 0<=k<=n");
        List<int[]> output = new ArrayList<>();
        combine(1, n, 0, new int[k], output);
        return output.toArray(int[][]::new);
    }

    public static int[][] combinationSum(int[] candidates, int target) {
        Objects.requireNonNull(candidates, "candidates");
        if (target < 0) throw new IllegalArgumentException("target must be non-negative");
        int[] sorted = candidates.clone();
        Arrays.sort(sorted);
        for (int value : sorted) if (value <= 0) throw new IllegalArgumentException("candidates must be positive");
        List<int[]> output = new ArrayList<>();
        int[] path = new int[target == 0 ? 0 : target / Math.max(1, sorted[0]) + 1];
        combinationSum(sorted, target, 0, path, 0, output);
        return output.toArray(int[][]::new);
    }

    public static int nQueensCount(int n) {
        if (n < 0 || n > 63) throw new IllegalArgumentException("n must be in [0,63]");
        if (n == 0) return 1;
        long mask = n == 63 ? Long.MAX_VALUE : (1L << n) - 1L;
        long count = queensCount(mask, 0L, 0L, 0L);
        if (count > Integer.MAX_VALUE) throw new ArithmeticException("solution count exceeds int");
        return (int) count;
    }

    public static boolean solveSudoku(char[][] board) {
        validateSudoku(board);
        int[] rowMask = new int[9];
        int[] columnMask = new int[9];
        int[] boxMask = new int[9];
        int[] empty = new int[81];
        int count = 0;
        for (int row = 0; row < 9; row++) {
            for (int column = 0; column < 9; column++) {
                char ch = board[row][column];
                if (ch == '.') {
                    empty[count++] = row * 9 + column;
                    continue;
                }
                int bit = 1 << (ch - '1');
                int box = (row / 3) * 3 + column / 3;
                if ((rowMask[row] & bit) != 0
                        || (columnMask[column] & bit) != 0
                        || (boxMask[box] & bit) != 0) {
                    throw new IllegalArgumentException("invalid Sudoku board");
                }
                rowMask[row] |= bit;
                columnMask[column] |= bit;
                boxMask[box] |= bit;
            }
        }
        return solveSudoku(board, empty, count, 0, rowMask, columnMask, boxMask);
    }

    private static void combine(
            int start,
            int n,
            int depth,
            int[] path,
            List<int[]> output) {
        if (depth == path.length) {
            output.add(path.clone());
            return;
        }
        int remaining = path.length - depth;
        for (int value = start; value <= n - remaining + 1; value++) {
            path[depth] = value;
            combine(value + 1, n, depth + 1, path, output);
        }
    }

    private static void combinationSum(
            int[] candidates,
            int remaining,
            int start,
            int[] path,
            int depth,
            List<int[]> output) {
        if (remaining == 0) {
            output.add(Arrays.copyOf(path, depth));
            return;
        }
        for (int i = start; i < candidates.length; i++) {
            if (i > start && candidates[i] == candidates[i - 1]) continue;
            int value = candidates[i];
            if (value > remaining) break;
            path[depth] = value;
            combinationSum(candidates, remaining - value, i, path, depth + 1, output);
        }
    }

    private static long queensCount(long mask, long columns, long left, long right) {
        if (columns == mask) return 1L;
        long available = mask & ~(columns | left | right);
        long count = 0L;
        while (available != 0L) {
            long bit = available & -available;
            available -= bit;
            count = Math.addExact(
                    count,
                    queensCount(
                            mask,
                            columns | bit,
                            ((left | bit) << 1) & mask,
                            (right | bit) >>> 1));
        }
        return count;
    }

    private static boolean solveSudoku(
            char[][] board,
            int[] empty,
            int count,
            int index,
            int[] rowMask,
            int[] columnMask,
            int[] boxMask) {
        if (index == count) return true;

        int best = index;
        int bestMask = 0;
        int bestCount = 10;
        for (int candidateIndex = index; candidateIndex < count; candidateIndex++) {
            int cell = empty[candidateIndex];
            int row = cell / 9;
            int column = cell % 9;
            int box = (row / 3) * 3 + column / 3;
            int available = 0x1ff & ~(rowMask[row] | columnMask[column] | boxMask[box]);
            int options = Integer.bitCount(available);
            if (options < bestCount) {
                best = candidateIndex;
                bestMask = available;
                bestCount = options;
                if (options <= 1) break;
            }
        }
        if (bestCount == 0) return false;

        int swap = empty[index];
        empty[index] = empty[best];
        empty[best] = swap;
        int cell = empty[index];
        int row = cell / 9;
        int column = cell % 9;
        int box = (row / 3) * 3 + column / 3;
        int available = best == index
                ? bestMask
                : 0x1ff & ~(rowMask[row] | columnMask[column] | boxMask[box]);

        while (available != 0) {
            int bit = available & -available;
            available -= bit;
            int digit = Integer.numberOfTrailingZeros(bit);
            board[row][column] = (char) ('1' + digit);
            rowMask[row] |= bit;
            columnMask[column] |= bit;
            boxMask[box] |= bit;
            if (solveSudoku(board, empty, count, index + 1, rowMask, columnMask, boxMask)) {
                return true;
            }
            rowMask[row] ^= bit;
            columnMask[column] ^= bit;
            boxMask[box] ^= bit;
            board[row][column] = '.';
        }

        swap = empty[index];
        empty[index] = empty[best];
        empty[best] = swap;
        return false;
    }

    private static void validateSudoku(char[][] board) {
        Objects.requireNonNull(board, "board");
        if (board.length != 9) throw new IllegalArgumentException("Sudoku requires 9 rows");
        for (char[] row : board) {
            Objects.requireNonNull(row, "row");
            if (row.length != 9) throw new IllegalArgumentException("Sudoku requires 9 columns");
            for (char ch : row) {
                if (ch != '.' && (ch < '1' || ch > '9')) {
                    throw new IllegalArgumentException("invalid Sudoku character");
                }
            }
        }
    }

}
