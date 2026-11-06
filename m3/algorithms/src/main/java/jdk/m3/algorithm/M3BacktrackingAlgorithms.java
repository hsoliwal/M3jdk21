// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import jdk.m3.algorithm.M3ProgressMonitor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//FLLDCIM: RELOCATE - donor blob 7a613543376e at commit 3385d79a2968 (repository, path, URL and digests: m3/algorithms/recipe/families-20261106/PROVENANCE.tsv); recipe m3/algorithms/recipe/families-20261106/relocate.py v1
public final class M3BacktrackingAlgorithms {
    private M3BacktrackingAlgorithms() {
    }

    public static List<List<Integer>> subsets(int[] values, M3ProgressMonitor monitor) {
        M3AlgorithmSupport.begin(monitor, "subsets", 1L << Math.min(values.length, 30));
        try {
            List<List<Integer>> result = new ArrayList<>();
            ArrayList<Integer> current = new ArrayList<>();
            subsets(values, 0, current, result, monitor);
            return List.copyOf(result);
        } finally {
            monitor.done();
        }
    }

    public static List<List<Integer>> permutations(int[] values, M3ProgressMonitor monitor) {
        M3AlgorithmSupport.begin(monitor, "permutations", M3ProgressMonitor.UNKNOWN);
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
            M3ProgressMonitor monitor) {
        int[] sorted = candidates.clone();
        Arrays.sort(sorted);
        M3AlgorithmSupport.begin(monitor, "combination-sum-unique", M3ProgressMonitor.UNKNOWN);
        try {
            List<List<Integer>> result = new ArrayList<>();
            combine(sorted, target, 0, new ArrayList<>(), result, monitor);
            return List.copyOf(result);
        } finally {
            monitor.done();
        }
    }

    public static List<List<String>> nQueens(int n, M3ProgressMonitor monitor) {
        if (n < 0) {
            throw new IllegalArgumentException("n must be non-negative");
        }
        M3AlgorithmSupport.begin(monitor, "n-queens", M3ProgressMonitor.UNKNOWN);
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
            M3ProgressMonitor monitor) {
        if (pairs < 0) {
            throw new IllegalArgumentException("pairs must be non-negative");
        }
        M3AlgorithmSupport.begin(monitor, "generate-parentheses", M3ProgressMonitor.UNKNOWN);
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
            M3ProgressMonitor monitor) {
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
            M3ProgressMonitor monitor) {
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
            M3ProgressMonitor monitor) {
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
            M3ProgressMonitor monitor) {
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
            M3ProgressMonitor monitor) {
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
}
