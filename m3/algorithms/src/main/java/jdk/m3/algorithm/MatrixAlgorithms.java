// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import jdk.m3.algorithm.ProgressMonitor;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Objects;

//FLLDCIM: RELOCATE - donor blob 17ba6ecb5a83 at commit 3385d79a2968 (repository, path, URL and digests: m3/algorithms/recipe/families-20261106/PROVENANCE.tsv); recipe m3/algorithms/recipe/families-20261106/relocate.py v1
public final class MatrixAlgorithms {
    private MatrixAlgorithms() {
    }

    public static List<Integer> spiralOrder(int[][] matrix, ProgressMonitor monitor) {
        validateRectangular(matrix);
        int rows = matrix.length;
        int columns = rows == 0 ? 0 : matrix[0].length;
        AlgorithmSupport.begin(monitor, "matrix-spiral", (long) rows * columns);
        try {
            List<Integer> result = new ArrayList<>(rows * columns);
            int top = 0;
            int bottom = rows - 1;
            int left = 0;
            int right = columns - 1;
            while (top <= bottom && left <= right) {
                for (int column = left; column <= right; column++) {
                    AlgorithmSupport.tick(monitor);
                    result.add(matrix[top][column]);
                }
                top++;
                for (int row = top; row <= bottom; row++) {
                    AlgorithmSupport.tick(monitor);
                    result.add(matrix[row][right]);
                }
                right--;
                if (top <= bottom) {
                    for (int column = right; column >= left; column--) {
                        AlgorithmSupport.tick(monitor);
                        result.add(matrix[bottom][column]);
                    }
                    bottom--;
                }
                if (left <= right) {
                    for (int row = bottom; row >= top; row--) {
                        AlgorithmSupport.tick(monitor);
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

    public static void rotateClockwise(int[][] matrix, ProgressMonitor monitor) {
        validateRectangular(matrix);
        if (matrix.length == 0) {
            return;
        }
        if (matrix.length != matrix[0].length) {
            throw new IllegalArgumentException("rotation requires a square matrix");
        }
        int n = matrix.length;
        AlgorithmSupport.begin(monitor, "rotate-square-matrix", (long) n * n);
        try {
            for (int row = 0; row < n; row++) {
                for (int column = row + 1; column < n; column++) {
                    AlgorithmSupport.tick(monitor);
                    int value = matrix[row][column];
                    matrix[row][column] = matrix[column][row];
                    matrix[column][row] = value;
                }
            }
            for (int[] row : matrix) {
                for (int left = 0, right = n - 1; left < right; left++, right--) {
                    AlgorithmSupport.tick(monitor);
                    int value = row[left];
                    row[left] = row[right];
                    row[right] = value;
                }
            }
        } finally {
            monitor.done();
        }
    }

    public static int countIslands(char[][] grid, ProgressMonitor monitor) {
        validateRectangular(grid);
        int rows = grid.length;
        int columns = rows == 0 ? 0 : grid[0].length;
        AlgorithmSupport.begin(monitor, "count-islands", (long) rows * columns);
        try {
            boolean[][] seen = new boolean[rows][columns];
            int islands = 0;
            int[] deltaRow = {-1, 1, 0, 0};
            int[] deltaColumn = {0, 0, -1, 1};
            for (int row = 0; row < rows; row++) {
                for (int column = 0; column < columns; column++) {
                    AlgorithmSupport.tick(monitor);
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


    public static void setZeroes(int[][] matrix, ProgressMonitor monitor) {
        validateRectangular(matrix);
        if (matrix.length == 0 || matrix[0].length == 0) {
            return;
        }
        int rows = matrix.length;
        int columns = matrix[0].length;
        AlgorithmSupport.begin(monitor, "matrix-set-zeroes", (long) rows * columns * 2L);
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
                    AlgorithmSupport.tick(monitor);
                    if (matrix[row][column] == 0) {
                        matrix[row][0] = 0;
                        matrix[0][column] = 0;
                    }
                }
            }
            for (int row = 1; row < rows; row++) {
                for (int column = 1; column < columns; column++) {
                    AlgorithmSupport.tick(monitor);
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
            ProgressMonitor monitor) {
        validateRectangular(matrix);
        if (matrix.length == 0 || matrix[0].length == 0) {
            return false;
        }
        AlgorithmSupport.begin(monitor, "search-sorted-matrix",
                matrix.length + matrix[0].length);
        try {
            int row = 0;
            int column = matrix[0].length - 1;
            while (row < matrix.length && column >= 0) {
                AlgorithmSupport.tick(monitor);
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

    //FLLDCIM: FOLD - member PrimitiveMatrixAlgorithms.DIRECTIONS_4 from donor blob 7d1010c03b68 (m3/algorithms/src/main/java/jdk/m3/algorithm/PrimitiveMatrixAlgorithms.java); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/primitive-matrix-fold/FOLD_MAP.tsv
    private static final int[] DIRECTIONS_4 = {-1, 0, 1, 0, -1};
    //FLLDCIM: FOLD - member PrimitiveMatrixAlgorithms.DIRECTIONS_8 from donor blob 7d1010c03b68 (m3/algorithms/src/main/java/jdk/m3/algorithm/PrimitiveMatrixAlgorithms.java); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/primitive-matrix-fold/FOLD_MAP.tsv
    private static final int[] DIRECTIONS_8 = {-1, -1, -1, 0, 0, 1, 1, 1};
    //FLLDCIM: FOLD - member PrimitiveMatrixAlgorithms.DIRECTIONS_8_Y from donor blob 7d1010c03b68 (m3/algorithms/src/main/java/jdk/m3/algorithm/PrimitiveMatrixAlgorithms.java); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/primitive-matrix-fold/FOLD_MAP.tsv
    private static final int[] DIRECTIONS_8_Y = {-1, 0, 1, -1, 1, -1, 0, 1};

    //FLLDCIM: FOLD - member PrimitiveMatrixAlgorithms.floodFill from donor blob 7d1010c03b68 (m3/algorithms/src/main/java/jdk/m3/algorithm/PrimitiveMatrixAlgorithms.java); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/primitive-matrix-fold/FOLD_MAP.tsv
    public static int[][] floodFill(
            int[][] image,
            int startRow,
            int startColumn,
            int replacement) {
        validateNonEmptyRectangular(image);
        int rows = image.length;
        int columns = image[0].length;
        if (startRow < 0 || startRow >= rows || startColumn < 0 || startColumn >= columns) {
            throw new IndexOutOfBoundsException("start cell out of bounds");
        }
        int[][] output = deepCopy(image);
        int original = output[startRow][startColumn];
        if (original == replacement) return output;
        int[] queue = new int[Math.multiplyExact(rows, columns)];
        int head = 0;
        int tail = 0;
        queue[tail++] = startRow * columns + startColumn;
        output[startRow][startColumn] = replacement;
        while (head < tail) {
            int cell = queue[head++];
            int row = cell / columns;
            int column = cell % columns;
            for (int d = 0; d < 4; d++) {
                int nr = row + DIRECTIONS_4[d];
                int nc = column + DIRECTIONS_4[d + 1];
                if (nr < 0 || nr >= rows || nc < 0 || nc >= columns) continue;
                if (output[nr][nc] == original) {
                    output[nr][nc] = replacement;
                    queue[tail++] = nr * columns + nc;
                }
            }
        }
        return output;
    }

    //FLLDCIM: FOLD - member PrimitiveMatrixAlgorithms.shortestPathBinaryMatrix from donor blob 7d1010c03b68 (m3/algorithms/src/main/java/jdk/m3/algorithm/PrimitiveMatrixAlgorithms.java); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/primitive-matrix-fold/FOLD_MAP.tsv
    public static int shortestPathBinaryMatrix(int[][] grid) {
        validateNonEmptyRectangular(grid);
        int rows = grid.length;
        int columns = grid[0].length;
        if (grid[0][0] != 0 || grid[rows - 1][columns - 1] != 0) return -1;
        int cells = Math.multiplyExact(rows, columns);
        int[] queue = new int[cells];
        int[] distance = new int[cells];
        Arrays.fill(distance, -1);
        int head = 0;
        int tail = 0;
        queue[tail++] = 0;
        distance[0] = 1;
        while (head < tail) {
            int cell = queue[head++];
            int row = cell / columns;
            int column = cell % columns;
            if (row == rows - 1 && column == columns - 1) return distance[cell];
            for (int d = 0; d < DIRECTIONS_8.length; d++) {
                int nr = row + DIRECTIONS_8[d];
                int nc = column + DIRECTIONS_8_Y[d];
                if (nr < 0 || nr >= rows || nc < 0 || nc >= columns) continue;
                int next = nr * columns + nc;
                if (grid[nr][nc] == 0 && distance[next] < 0) {
                    distance[next] = distance[cell] + 1;
                    queue[tail++] = next;
                }
            }
        }
        return -1;
    }

    //FLLDCIM: FOLD - member PrimitiveMatrixAlgorithms.maximalSquareArea from donor blob 7d1010c03b68 (m3/algorithms/src/main/java/jdk/m3/algorithm/PrimitiveMatrixAlgorithms.java); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/primitive-matrix-fold/FOLD_MAP.tsv
    public static int maximalSquareArea(int[][] binary) {
        validateNonEmptyRectangular(binary);
        int columns = binary[0].length;
        int[] dp = new int[columns + 1];
        int best = 0;
        for (int row = 1; row <= binary.length; row++) {
            int upperLeft = 0;
            for (int column = 1; column <= columns; column++) {
                int oldUp = dp[column];
                if (binary[row - 1][column - 1] != 0) {
                    dp[column] = 1 + Math.min(
                            upperLeft,
                            Math.min(dp[column], dp[column - 1]));
                    best = Math.max(best, dp[column]);
                } else {
                    dp[column] = 0;
                }
                upperLeft = oldUp;
            }
        }
        return Math.multiplyExact(best, best);
    }

    //FLLDCIM: FOLD - member PrimitiveMatrixAlgorithms.deepCopy from donor blob 7d1010c03b68 (m3/algorithms/src/main/java/jdk/m3/algorithm/PrimitiveMatrixAlgorithms.java); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/primitive-matrix-fold/FOLD_MAP.tsv
    public static int[][] deepCopy(int[][] matrix) {
        Objects.requireNonNull(matrix, "matrix");
        int[][] copy = new int[matrix.length][];
        for (int row = 0; row < matrix.length; row++) {
            copy[row] = Objects.requireNonNull(matrix[row], "matrix row").clone();
        }
        return copy;
    }

    //FLLDCIM: FOLD - member PrimitiveMatrixAlgorithms.validateRectangular from donor blob 7d1010c03b68 (m3/algorithms/src/main/java/jdk/m3/algorithm/PrimitiveMatrixAlgorithms.java); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/primitive-matrix-fold/FOLD_MAP.tsv
    private static void validateNonEmptyRectangular(int[][] matrix) {
        Objects.requireNonNull(matrix, "matrix");
        if (matrix.length == 0) throw new IllegalArgumentException("matrix must not be empty");
        Objects.requireNonNull(matrix[0], "matrix row");
        if (matrix[0].length == 0) throw new IllegalArgumentException("matrix rows must not be empty");
        int columns = matrix[0].length;
        for (int[] row : matrix) {
            Objects.requireNonNull(row, "matrix row");
            if (row.length != columns) throw new IllegalArgumentException("matrix must be rectangular");
        }
    }
}
