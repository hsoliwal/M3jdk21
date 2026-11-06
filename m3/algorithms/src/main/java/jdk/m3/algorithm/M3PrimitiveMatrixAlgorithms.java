// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import java.util.Arrays;
import java.util.Objects;

/** Grid/matrix traversal and in-place transform algorithms. */
//FLLDCIM: RELOCATE - donor blob 3442e04bb8cf at commit 3385d79a2968 (repository, path, URL and digests: m3/algorithms/recipe/algorithm-extras-20261106/PROVENANCE.tsv); recipe m3/algorithms/recipe/algorithm-extras-20261106/relocate.py v1
public final class M3PrimitiveMatrixAlgorithms {
    private static final int[] DIRECTIONS_4 = {-1, 0, 1, 0, -1};
    private static final int[] DIRECTIONS_8 = {-1, -1, -1, 0, 0, 1, 1, 1};
    private static final int[] DIRECTIONS_8_Y = {-1, 0, 1, -1, 1, -1, 0, 1};

    private M3PrimitiveMatrixAlgorithms() {}

    public static int[][] floodFill(
            int[][] image,
            int startRow,
            int startColumn,
            int replacement) {
        validateRectangular(image);
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

    public static int shortestPathBinaryMatrix(int[][] grid) {
        validateRectangular(grid);
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

    public static int maximalSquareArea(int[][] binary) {
        validateRectangular(binary);
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

    public static int[][] deepCopy(int[][] matrix) {
        Objects.requireNonNull(matrix, "matrix");
        int[][] copy = new int[matrix.length][];
        for (int row = 0; row < matrix.length; row++) {
            copy[row] = Objects.requireNonNull(matrix[row], "matrix row").clone();
        }
        return copy;
    }

    private static void validateRectangular(int[][] matrix) {
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
