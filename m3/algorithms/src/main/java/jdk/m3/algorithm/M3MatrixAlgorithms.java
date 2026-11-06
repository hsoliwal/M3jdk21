// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import jdk.m3.algorithm.M3ProgressMonitor;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

//FLLDCIM: RELOCATE - donor blob 17ba6ecb5a83 at commit 3385d79a2968 (repository, path, URL and digests: m3/algorithms/recipe/families-20261106/PROVENANCE.tsv); recipe m3/algorithms/recipe/families-20261106/relocate.py v1
public final class M3MatrixAlgorithms {
    private M3MatrixAlgorithms() {
    }

    public static List<Integer> spiralOrder(int[][] matrix, M3ProgressMonitor monitor) {
        validateRectangular(matrix);
        int rows = matrix.length;
        int columns = rows == 0 ? 0 : matrix[0].length;
        M3AlgorithmSupport.begin(monitor, "matrix-spiral", (long) rows * columns);
        try {
            List<Integer> result = new ArrayList<>(rows * columns);
            int top = 0;
            int bottom = rows - 1;
            int left = 0;
            int right = columns - 1;
            while (top <= bottom && left <= right) {
                for (int column = left; column <= right; column++) {
                    M3AlgorithmSupport.tick(monitor);
                    result.add(matrix[top][column]);
                }
                top++;
                for (int row = top; row <= bottom; row++) {
                    M3AlgorithmSupport.tick(monitor);
                    result.add(matrix[row][right]);
                }
                right--;
                if (top <= bottom) {
                    for (int column = right; column >= left; column--) {
                        M3AlgorithmSupport.tick(monitor);
                        result.add(matrix[bottom][column]);
                    }
                    bottom--;
                }
                if (left <= right) {
                    for (int row = bottom; row >= top; row--) {
                        M3AlgorithmSupport.tick(monitor);
                        result.add(matrix[row][left]);
                    }
                    left++;
                }
            }
            return List.copyOf(result);
        } finally {
            monitor.done();
        }
    }

    public static void rotateClockwise(int[][] matrix, M3ProgressMonitor monitor) {
        validateRectangular(matrix);
        if (matrix.length == 0) {
            return;
        }
        if (matrix.length != matrix[0].length) {
            throw new IllegalArgumentException("rotation requires a square matrix");
        }
        int n = matrix.length;
        M3AlgorithmSupport.begin(monitor, "rotate-square-matrix", (long) n * n);
        try {
            for (int row = 0; row < n; row++) {
                for (int column = row + 1; column < n; column++) {
                    M3AlgorithmSupport.tick(monitor);
                    int value = matrix[row][column];
                    matrix[row][column] = matrix[column][row];
                    matrix[column][row] = value;
                }
            }
            for (int[] row : matrix) {
                for (int left = 0, right = n - 1; left < right; left++, right--) {
                    M3AlgorithmSupport.tick(monitor);
                    int value = row[left];
                    row[left] = row[right];
                    row[right] = value;
                }
            }
        } finally {
            monitor.done();
        }
    }

    public static int countIslands(char[][] grid, M3ProgressMonitor monitor) {
        validateRectangular(grid);
        int rows = grid.length;
        int columns = rows == 0 ? 0 : grid[0].length;
        M3AlgorithmSupport.begin(monitor, "count-islands", (long) rows * columns);
        try {
            boolean[][] seen = new boolean[rows][columns];
            int islands = 0;
            int[] deltaRow = {-1, 1, 0, 0};
            int[] deltaColumn = {0, 0, -1, 1};
            for (int row = 0; row < rows; row++) {
                for (int column = 0; column < columns; column++) {
                    M3AlgorithmSupport.tick(monitor);
                    if (grid[row][column] != '1' || seen[row][column]) {
                        continue;
                    }
                    islands++;
                    ArrayDeque<Integer> queue = new ArrayDeque<>();
                    seen[row][column] = true;
                    queue.add(row * columns + column);
                    while (!queue.isEmpty()) {
                        monitor.checkCanceled();
                        int encoded = queue.removeFirst();
                        int currentRow = encoded / columns;
                        int currentColumn = encoded % columns;
                        for (int direction = 0; direction < 4; direction++) {
                            int nextRow = currentRow + deltaRow[direction];
                            int nextColumn = currentColumn + deltaColumn[direction];
                            if (nextRow >= 0 && nextRow < rows
                                    && nextColumn >= 0 && nextColumn < columns
                                    && grid[nextRow][nextColumn] == '1'
                                    && !seen[nextRow][nextColumn]) {
                                seen[nextRow][nextColumn] = true;
                                queue.addLast(nextRow * columns + nextColumn);
                            }
                        }
                    }
                }
            }
            return islands;
        } finally {
            monitor.done();
        }
    }


    public static void setZeroes(int[][] matrix, M3ProgressMonitor monitor) {
        validateRectangular(matrix);
        if (matrix.length == 0 || matrix[0].length == 0) {
            return;
        }
        int rows = matrix.length;
        int columns = matrix[0].length;
        M3AlgorithmSupport.begin(monitor, "matrix-set-zeroes", (long) rows * columns * 2L);
        try {
            boolean firstRowZero = false;
            boolean firstColumnZero = false;
            for (int column = 0; column < columns; column++) {
                if (matrix[0][column] == 0) {
                    firstRowZero = true;
                }
            }
            for (int row = 0; row < rows; row++) {
                if (matrix[row][0] == 0) {
                    firstColumnZero = true;
                }
            }
            for (int row = 1; row < rows; row++) {
                for (int column = 1; column < columns; column++) {
                    M3AlgorithmSupport.tick(monitor);
                    if (matrix[row][column] == 0) {
                        matrix[row][0] = 0;
                        matrix[0][column] = 0;
                    }
                }
            }
            for (int row = 1; row < rows; row++) {
                for (int column = 1; column < columns; column++) {
                    M3AlgorithmSupport.tick(monitor);
                    if (matrix[row][0] == 0 || matrix[0][column] == 0) {
                        matrix[row][column] = 0;
                    }
                }
            }
            if (firstRowZero) {
                java.util.Arrays.fill(matrix[0], 0);
            }
            if (firstColumnZero) {
                for (int row = 0; row < rows; row++) {
                    matrix[row][0] = 0;
                }
            }
        } finally {
            monitor.done();
        }
    }

    public static boolean searchSorted(
            int[][] matrix,
            int target,
            M3ProgressMonitor monitor) {
        validateRectangular(matrix);
        if (matrix.length == 0 || matrix[0].length == 0) {
            return false;
        }
        M3AlgorithmSupport.begin(monitor, "search-sorted-matrix",
                matrix.length + matrix[0].length);
        try {
            int row = 0;
            int column = matrix[0].length - 1;
            while (row < matrix.length && column >= 0) {
                M3AlgorithmSupport.tick(monitor);
                int value = matrix[row][column];
                if (value == target) {
                    return true;
                }
                if (value > target) {
                    column--;
                } else {
                    row++;
                }
            }
            return false;
        } finally {
            monitor.done();
        }
    }

    private static void validateRectangular(int[][] matrix) {
        if (matrix == null) {
            throw new NullPointerException("matrix");
        }
        int width = matrix.length == 0 ? 0 : matrix[0].length;
        for (int[] row : matrix) {
            if (row == null || row.length != width) {
                throw new IllegalArgumentException("matrix must be rectangular");
            }
        }
    }

    private static void validateRectangular(char[][] matrix) {
        if (matrix == null) {
            throw new NullPointerException("matrix");
        }
        int width = matrix.length == 0 ? 0 : matrix[0].length;
        for (char[] row : matrix) {
            if (row == null || row.length != width) {
                throw new IllegalArgumentException("matrix must be rectangular");
            }
        }
    }
}
