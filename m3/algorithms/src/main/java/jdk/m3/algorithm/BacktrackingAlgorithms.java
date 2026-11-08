// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import jdk.m3.algorithm.ProgressMonitor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

//FLLDCIM: RELOCATE - donor blob 7a613543376e at commit 3385d79a2968 (repository, path, URL and digests: m3/algorithms/recipe/families-20261106/PROVENANCE.tsv); recipe m3/algorithms/recipe/families-20261106/relocate.py v1
public final class BacktrackingAlgorithms {
    private BacktrackingAlgorithms() {
    }

    public static List<List<Integer>> subsets(int[] values, ProgressMonitor monitor) {
        AlgorithmSupport.begin(monitor, "subsets", 1L << Math.min(values.length, 30));
        try {
            List<List<Integer>> result = new ArrayList<>();
            ArrayList<Integer> current = new ArrayList<>();
            subsets(values, 0, current, result, monitor);
            return List.copyOf(result);
        } finally {
            monitor.done();
        }
    }

    public static List<List<Integer>> permutations(int[] values, ProgressMonitor monitor) {
        AlgorithmSupport.begin(monitor, "permutations", ProgressMonitor.UNKNOWN);
        try {
            List<List<Integer>> result = new ArrayList<>();
            int[] working = values.clone();
            permute(working, 0, result, monitor);
            return List.copyOf(result);
        } finally {
            monitor.done();
        }
    }

    public static List<List<Integer>> combinationSumUnique(
            int[] candidates,
            int target,
            ProgressMonitor monitor) {
        int[] sorted = candidates.clone();
        Arrays.sort(sorted);
        AlgorithmSupport.begin(monitor, "combination-sum-unique", ProgressMonitor.UNKNOWN);
        try {
            List<List<Integer>> result = new ArrayList<>();
            combine(sorted, target, 0, new ArrayList<>(), result, monitor);
            return List.copyOf(result);
        } finally {
            monitor.done();
        }
    }

    public static List<List<String>> nQueens(int n, ProgressMonitor monitor) {
        if (n < 0) {
            throw new IllegalArgumentException("n must be non-negative");
        }
        AlgorithmSupport.begin(monitor, "n-queens", ProgressMonitor.UNKNOWN);
        try {
            List<List<String>> result = new ArrayList<>();
            int[] columns = new int[n];
            Arrays.fill(columns, -1);
            boolean[] usedColumns = new boolean[n];
            boolean[] descending = new boolean[Math.max(1, 2 * n - 1)];
            boolean[] ascending = new boolean[Math.max(1, 2 * n - 1)];
            queens(0, n, columns, usedColumns, descending, ascending, result, monitor);
            return List.copyOf(result);
        } finally {
            monitor.done();
        }
    }


    public static List<String> generateParentheses(
            int pairs,
            ProgressMonitor monitor) {
        if (pairs < 0) {
            throw new IllegalArgumentException("pairs must be non-negative");
        }
        AlgorithmSupport.begin(monitor, "generate-parentheses", ProgressMonitor.UNKNOWN);
        try {
            List<String> result = new ArrayList<>();
            char[] buffer = new char[pairs * 2];
            parentheses(buffer, 0, 0, 0, pairs, result, monitor);
            return List.copyOf(result);
        } finally {
            monitor.done();
        }
    }

    private static void subsets(
            int[] values,
            int index,
            ArrayList<Integer> current,
            List<List<Integer>> result,
            ProgressMonitor monitor) {
        monitor.checkCanceled();
        if (index == values.length) {
            result.add(List.copyOf(current));
            monitor.worked(1);
            return;
        }
        subsets(values, index + 1, current, result, monitor);
        current.add(values[index]);
        subsets(values, index + 1, current, result, monitor);
        current.removeLast();
    }

    private static void permute(
            int[] values,
            int index,
            List<List<Integer>> result,
            ProgressMonitor monitor) {
        monitor.checkCanceled();
        if (index == values.length) {
            result.add(Arrays.stream(values).boxed().toList());
            monitor.worked(1);
            return;
        }
        for (int candidate = index; candidate < values.length; candidate++) {
            swap(values, index, candidate);
            permute(values, index + 1, result, monitor);
            swap(values, index, candidate);
        }
    }

    private static void combine(
            int[] values,
            int remaining,
            int start,
            ArrayList<Integer> current,
            List<List<Integer>> result,
            ProgressMonitor monitor) {
        monitor.checkCanceled();
        if (remaining == 0) {
            result.add(List.copyOf(current));
            monitor.worked(1);
            return;
        }
        for (int index = start; index < values.length; index++) {
            if (index > start && values[index] == values[index - 1]) {
                continue;
            }
            if (values[index] > remaining) {
                break;
            }
            current.add(values[index]);
            combine(values, remaining - values[index], index + 1, current, result, monitor);
            current.removeLast();
        }
    }

    private static void queens(
            int row,
            int n,
            int[] columns,
            boolean[] usedColumns,
            boolean[] descending,
            boolean[] ascending,
            List<List<String>> result,
            ProgressMonitor monitor) {
        monitor.checkCanceled();
        if (row == n) {
            List<String> board = new ArrayList<>(n);
            for (int currentRow = 0; currentRow < n; currentRow++) {
                char[] line = new char[n];
                Arrays.fill(line, '.');
                line[columns[currentRow]] = 'Q';
                board.add(new String(line));
            }
            result.add(List.copyOf(board));
            monitor.worked(1);
            return;
        }
        for (int column = 0; column < n; column++) {
            int down = row - column + n - 1;
            int up = row + column;
            if (usedColumns[column] || descending[down] || ascending[up]) {
                continue;
            }
            columns[row] = column;
            usedColumns[column] = descending[down] = ascending[up] = true;
            queens(row + 1, n, columns, usedColumns, descending, ascending, result, monitor);
            usedColumns[column] = descending[down] = ascending[up] = false;
            columns[row] = -1;
        }
    }


    private static void parentheses(
            char[] buffer,
            int index,
            int open,
            int close,
            int pairs,
            List<String> result,
            ProgressMonitor monitor) {
        monitor.checkCanceled();
        if (index == buffer.length) {
            result.add(new String(buffer));
            monitor.worked(1);
            return;
        }
        if (open < pairs) {
            buffer[index] = '(';
            parentheses(buffer, index + 1, open + 1, close, pairs, result, monitor);
        }
        if (close < open) {
            buffer[index] = ')';
            parentheses(buffer, index + 1, open, close + 1, pairs, result, monitor);
        }
    }

    private static void swap(int[] values, int left, int right) {
        int value = values[left];
        values[left] = values[right];
        values[right] = value;
    }

    //FLLDCIM: FOLD - member PrimitiveBacktrackingAlgorithms.combinations from donor blob db414577380e (m3/algorithms/src/main/java/jdk/m3/algorithm/PrimitiveBacktrackingAlgorithms.java); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/primitive-backtracking-fold/FOLD_MAP.tsv
    public static int[][] combinations(int n, int k) {
        if (n < 0 || k < 0 || k > n) throw new IllegalArgumentException("requires 0<=k<=n");
        List<int[]> output = new ArrayList<>();
        combine(1, n, 0, new int[k], output);
        return output.toArray(int[][]::new);
    }

    //FLLDCIM: FOLD - member PrimitiveBacktrackingAlgorithms.combinationSum from donor blob db414577380e (m3/algorithms/src/main/java/jdk/m3/algorithm/PrimitiveBacktrackingAlgorithms.java); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/primitive-backtracking-fold/FOLD_MAP.tsv
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

    //FLLDCIM: FOLD - member PrimitiveBacktrackingAlgorithms.nQueensCount from donor blob db414577380e (m3/algorithms/src/main/java/jdk/m3/algorithm/PrimitiveBacktrackingAlgorithms.java); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/primitive-backtracking-fold/FOLD_MAP.tsv
    public static int nQueensCount(int n) {
        if (n < 0 || n > 63) throw new IllegalArgumentException("n must be in [0,63]");
        if (n == 0) return 1;
        long mask = n == 63 ? Long.MAX_VALUE : (1L << n) - 1L;
        long count = queensCount(mask, 0L, 0L, 0L);
        if (count > Integer.MAX_VALUE) throw new ArithmeticException("solution count exceeds int");
        return (int) count;
    }

    //FLLDCIM: FOLD - member PrimitiveBacktrackingAlgorithms.solveSudoku from donor blob db414577380e (m3/algorithms/src/main/java/jdk/m3/algorithm/PrimitiveBacktrackingAlgorithms.java); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/primitive-backtracking-fold/FOLD_MAP.tsv
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

    //FLLDCIM: FOLD - member PrimitiveBacktrackingAlgorithms.combine from donor blob db414577380e (m3/algorithms/src/main/java/jdk/m3/algorithm/PrimitiveBacktrackingAlgorithms.java); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/primitive-backtracking-fold/FOLD_MAP.tsv
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

    //FLLDCIM: FOLD - member PrimitiveBacktrackingAlgorithms.combinationSum from donor blob db414577380e (m3/algorithms/src/main/java/jdk/m3/algorithm/PrimitiveBacktrackingAlgorithms.java); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/primitive-backtracking-fold/FOLD_MAP.tsv
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

    //FLLDCIM: FOLD - member PrimitiveBacktrackingAlgorithms.queensCount from donor blob db414577380e (m3/algorithms/src/main/java/jdk/m3/algorithm/PrimitiveBacktrackingAlgorithms.java); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/primitive-backtracking-fold/FOLD_MAP.tsv
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

    //FLLDCIM: FOLD - member PrimitiveBacktrackingAlgorithms.solveSudoku from donor blob db414577380e (m3/algorithms/src/main/java/jdk/m3/algorithm/PrimitiveBacktrackingAlgorithms.java); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/primitive-backtracking-fold/FOLD_MAP.tsv
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

    //FLLDCIM: FOLD - member PrimitiveBacktrackingAlgorithms.validateSudoku from donor blob db414577380e (m3/algorithms/src/main/java/jdk/m3/algorithm/PrimitiveBacktrackingAlgorithms.java); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/primitive-backtracking-fold/FOLD_MAP.tsv
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
