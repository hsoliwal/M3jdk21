// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import static jdk.m3.algorithm.SortingRangeStreamingReferenceTest.finding;
import static jdk.m3.algorithm.SortingRangeStreamingReferenceTest.report;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;

//FLLDCIM: ORACLE - M3JDK-authored reference oracle of recipe/algorithm-extras-20261106 (the donor catalogue has no tests)
/**
 * Reference oracle of the remaining relocated members (M3PrimitiveArrayAlgorithms, M3PrimitiveTreeAlgorithms,
 * M3PrimitiveDynamicProgrammingAlgorithms, M3PrimitiveBacktrackingAlgorithms, M3PrimitiveMatrixAlgorithms,
 * M3PrimitiveBitAlgorithms, M3LinkedListAlgorithms, M3PrimitiveTrie) against brute force on small instances,
 * exhaustive where feasible (subsets, paths, colourings, all pairs).
 */
class ArrayTreeDpReferenceTest {
    private static final long SEED = 20261106L;
    private static final int CASES = 20_000;

    private static int[] ints(SplittableRandom random, int n, int bound) {
        int[] values = new int[n];
        for (int i = 0; i < n; i++) {
            values[i] = random.nextInt(-bound, bound + 1);
        }
        return values;
    }

    @Test
    void arrayMembersMatchBruteForce() {
        SplittableRandom random = new SplittableRandom(SEED);
        for (int i = 0; i < CASES; i++) {
            int[] values = ints(random, random.nextInt(0, 14), 9);
            int n = values.length;
            int[] sorted = values.clone();
            Arrays.sort(sorted);
            int target = random.nextInt(-10, 11);
            int index = ArrayAlgorithms.binarySearch(sorted, target);
            assertEquals(Arrays.stream(sorted).anyMatch(v -> v == target), index >= 0);
            assertTrue(index < 0 || sorted[index] == target);
            assertEquals(index, ArrayAlgorithms.binarySearch(sorted, target, ProgressMonitor.none()));
            int[] circular = new int[n];
            for (int k = 0; k < n; k++) {
                circular[k] = -1;
                boolean found = false;
                for (int step = 1; step < n && !found; step++) {
                    found = values[(k + step) % n] > values[k];
                    circular[k] = found ? values[(k + step) % n] : -1;
                }
            }
            assertArrayEquals(circular, ArrayAlgorithms.nextGreaterCircular(values));
            if (n > 0) {
                BigInteger best = null;
                for (int a = 0; a < n; a++) {
                    BigInteger product = BigInteger.ONE;
                    for (int b = a; b < n; b++) {
                        product = product.multiply(BigInteger.valueOf(values[b]));
                        best = best == null || product.compareTo(best) > 0 ? product : best;
                    }
                }
                assertEquals(best.longValueExact(), ArrayAlgorithms.maxProductSubarray(values));
            }
            long profit = 0;
            long water = 0;
            for (int a = 0; a < n; a++) {
                for (int b = a + 1; b < n; b++) {
                    profit = Math.max(profit, (long) values[b] - values[a]);
                    water = Math.max(water, (long) Math.min(values[a], values[b]) * (b - a));
                }
            }
            assertEquals(profit, ArrayAlgorithms.maxProfitOneTransaction(values));
            assertEquals(water, ArrayAlgorithms.containerMostWater(values));
            int[] moved = values.clone();
            ArrayAlgorithms.moveZeroes(moved);
            int[] expectedMoved = new int[n];
            int w = 0;
            for (int v : values) {
                if (v != 0) {
                    expectedMoved[w++] = v;
                }
            }
            assertArrayEquals(expectedMoved, moved);
            int steps = random.nextInt(-30, 31);
            int[] rotated = values.clone();
            ArrayAlgorithms.rotateRight(rotated, steps);
            for (int k = 0; k < n; k++) {
                assertEquals(values[k], rotated[Math.floorMod(k + steps, n)]);
            }
            Integer majority = null;
            for (int v : values) {
                int count = 0;
                for (int u : values) {
                    count += u == v ? 1 : 0;
                }
                majority = count > n / 2 ? Integer.valueOf(v) : majority;
            }
            if (majority == null) {
                assertThrows(IllegalArgumentException.class, () -> ArrayAlgorithms.majorityElement(values));
            } else {
                assertEquals(majority.intValue(), ArrayAlgorithms.majorityElement(values));
            }
        }
        report("array", "binarySearch(x2)+nextGreaterCircular+maxProductSubarray+maxProfitOneTransaction+"
                + "containerMostWater+moveZeroes+rotateRight+majorityElement", 10L * CASES, 10L * CASES);
        finding("nextGreaterCircular", "the LC 503 sentinel -1 also is a value: nextGreaterCircular([-2, -1]) = "
                + Arrays.toString(ArrayAlgorithms.nextGreaterCircular(new int[] {-2, -1}))
                + " (index 0 has the greater value -1, index 1 has none)");
        long more = 0;
        for (int i = 0; i < CASES; i++, more += 3) {
            int n = random.nextInt(1, 30);
            int[] base = random.ints(-500, 500).distinct().limit(n).sorted().toArray();
            int shift = random.nextInt(n);
            int[] rotated = new int[n];
            for (int k = 0; k < n; k++) {
                rotated[k] = base[(k + shift) % n];
            }
            assertEquals(base[0], ArrayAlgorithms.findMinRotated(rotated));
            int[] duplicate = new int[n + 1];
            int repeated = random.nextInt(1, n + 1);
            for (int k = 0; k < n; k++) {
                duplicate[k] = k + 1;
            }
            duplicate[n] = repeated;
            for (int k = duplicate.length - 1; k > 0; k--) {
                int j = random.nextInt(k + 1);
                int swap = duplicate[k];
                duplicate[k] = duplicate[j];
                duplicate[j] = swap;
            }
            assertEquals(repeated, ArrayAlgorithms.findDuplicateFloyd(duplicate));
            int bits = random.nextInt(0, 2_000);
            int[] counts = ArrayAlgorithms.countBits(bits);
            for (int k = 0; k <= bits; k++) {
                assertEquals(Integer.bitCount(k), counts[k]);
            }
        }
        report("array", "findMinRotated+findDuplicateFloyd+countBits", more, more);
    }

    // ---------------------------------------------------------------- trees

    @Test
    void treeMembersMatchRecursiveBruteForce() {
        SplittableRandom random = new SplittableRandom(SEED + 1);
        for (int i = 0; i < CASES; i++) {
            int n = random.nextInt(1, 25);
            boolean bst = random.nextBoolean();
            int[][] t = AlgorithmExtrasDump.randomTree(random, n, bst);
            PrimitiveTreeAlgorithms.BinaryTree tree = new PrimitiveTreeAlgorithms.BinaryTree(t[0], t[1], t[2],
                    t[3][0]);
            List<Integer> pre = new ArrayList<>();
            List<Integer> post = new ArrayList<>();
            walk(t, t[3][0], pre, post);
            assertEquals(pre, box(PrimitiveTreeAlgorithms.preorder(tree)));
            assertEquals(post, box(PrimitiveTreeAlgorithms.postorder(tree)));
            int[] parent = new int[n];
            Arrays.fill(parent, -1);
            for (int v = 0; v < n; v++) {
                if (t[1][v] >= 0) {
                    parent[t[1][v]] = v;
                }
                if (t[2][v] >= 0) {
                    parent[t[2][v]] = v;
                }
            }
            int diameter = 0;
            long bestPath = Long.MIN_VALUE;
            for (int a = 0; a < n; a++) {
                for (int b = a; b < n; b++) {
                    List<Integer> path = path(parent, a, b);
                    diameter = Math.max(diameter, path.size() - 1);
                    long sum = 0;
                    for (int v : path) {
                        sum += t[0][v];
                    }
                    bestPath = Math.max(bestPath, sum);
                }
            }
            assertEquals(diameter, PrimitiveTreeAlgorithms.diameterEdges(tree));
            assertEquals(bestPath, PrimitiveTreeAlgorithms.maxPathSum(tree));
            PrimitiveTreeAlgorithms.BinaryTree inverted = PrimitiveTreeAlgorithms.inverted(tree);
            assertArrayEquals(t[2], inverted.left());
            assertArrayEquals(t[1], inverted.right());
            if (bst) {
                int x = t[0][random.nextInt(n)];
                int y = random.nextInt(4) == 0 ? 5_000 : t[0][random.nextInt(n)];
                int expected = -1;
                if (y != 5_000) {
                    List<Integer> px = path(parent, t[3][0], indexOfValue(t[0], x));
                    List<Integer> py = path(parent, t[3][0], indexOfValue(t[0], y));
                    for (int k = 0; k < Math.min(px.size(), py.size()) && px.get(k).equals(py.get(k)); k++) {
                        expected = px.get(k);
                    }
                }
                assertEquals(expected, PrimitiveTreeAlgorithms.lowestCommonAncestorBstByValue(tree, x, y));
            }
        }
        report("tree", "preorder+postorder+diameterEdges+maxPathSum+inverted+lowestCommonAncestorBstByValue",
                6L * CASES, 6L * CASES);
    }

    private static void walk(int[][] t, int node, List<Integer> pre, List<Integer> post) {
        if (node < 0) {
            return;
        }
        pre.add(t[0][node]);
        walk(t, t[1][node], pre, post);
        walk(t, t[2][node], pre, post);
        post.add(t[0][node]);
    }

    /** Nodes on the tree path from a to b (inclusive), from the root side of a. */
    private static List<Integer> path(int[] parent, int a, int b) {
        List<Integer> up = new ArrayList<>();
        for (int v = a; v >= 0; v = parent[v]) {
            up.add(0, v);
        }
        List<Integer> other = new ArrayList<>();
        for (int v = b; v >= 0; v = parent[v]) {
            other.add(0, v);
        }
        int common = 0;
        while (common < up.size() && common < other.size() && up.get(common).equals(other.get(common))) {
            common++;
        }
        List<Integer> path = new ArrayList<>();
        for (int k = up.size() - 1; k >= common - 1; k--) {
            path.add(up.get(k));
        }
        for (int k = common; k < other.size(); k++) {
            path.add(other.get(k));
        }
        return path;
    }

    private static int indexOfValue(int[] values, int value) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] == value) {
                return i;
            }
        }
        return -1;
    }

    private static List<Integer> box(int[] values) {
        List<Integer> out = new ArrayList<>();
        for (int v : values) {
            out.add(v);
        }
        return out;
    }

    // ---------------------------------------------------------------- dynamic programming

    @Test
    void dynamicProgrammingMatchesExhaustiveSearch() {
        SplittableRandom random = new SplittableRandom(SEED + 2);
        BigInteger a = BigInteger.ONE;
        BigInteger b = BigInteger.ONE;
        for (int n = 0; n <= 100; n++) {
            if (a.bitLength() < 64) {
                assertEquals(a.longValueExact(), DynamicProgrammingAlgorithms.climbStairs(n), "" + n);
            } else {
                int steps = n;
                assertThrows(ArithmeticException.class, () -> DynamicProgrammingAlgorithms.climbStairs(steps));
            }
            BigInteger next = a.add(b);
            a = b;
            b = next;
        }
        long cases = 101;
        long decodeOverflows = 0;
        for (int i = 0; i < CASES; i++, cases += 5) {
            int[] cost = ints(random, random.nextInt(0, 12), 50);
            for (int k = 0; k < cost.length; k++) {
                cost[k] = Math.abs(cost[k]);
            }
            assertEquals(minCost(cost, cost.length), DynamicProgrammingAlgorithms.minCostClimbingStairs(cost));
            int[] coins = random.ints(1, 12).distinct().limit(random.nextInt(0, 4)).toArray();
            int amount = random.nextInt(0, 40);
            assertEquals(ways(coins, 0, amount), DynamicProgrammingAlgorithms.coinChangeWays(coins, amount));
            int rows = random.nextInt(1, 6);
            int columns = random.nextInt(1, 6);
            int[][] grid = new int[rows][columns];
            for (int[] row : grid) {
                for (int c = 0; c < columns; c++) {
                    row[c] = random.nextInt(-20, 50);
                }
            }
            assertEquals(minPath(grid, 0, 0), DynamicProgrammingAlgorithms.minPathSum(grid));
            int[] values = ints(random, random.nextInt(0, 12), 6);
            for (int k = 0; k < values.length; k++) {
                values[k] = Math.abs(values[k]);
            }
            int target = random.nextInt(-20, 21);
            long expected = 0;
            for (int mask = 0; mask < 1 << values.length; mask++) {
                long sum = 0;
                for (int k = 0; k < values.length; k++) {
                    sum += ((mask >> k) & 1) == 1 ? values[k] : -values[k];
                }
                expected += sum == target ? 1 : 0;
            }
            assertEquals(expected, DynamicProgrammingAlgorithms.targetSumWays(values, target));
            char[] digits = new char[random.nextInt(0, 16)];
            for (int k = 0; k < digits.length; k++) {
                digits[k] = (char) ('0' + random.nextInt(random.nextBoolean() ? 3 : 10));
            }
            String text = new String(digits);
            assertEquals(text.isEmpty() ? 0 : decode(text, 0), DynamicProgrammingAlgorithms.decodeWays(text));
        }
        String ones = "1".repeat(60);
        BigInteger exact = BigInteger.ONE;
        BigInteger previous = BigInteger.ONE;
        for (int k = 1; k < ones.length(); k++) {
            BigInteger next = exact.add(previous);
            previous = exact;
            exact = next;
        }
        decodeOverflows += exact.bitLength() > 31 ? 1 : 0;
        int wrapped = DynamicProgrammingAlgorithms.decodeWays(ones);
        report("dynamic-programming", "climbStairs+minCostClimbingStairs+coinChangeWays+minPathSum+targetSumWays+"
                + "decodeWays", cases, cases);
        finding("decodeWays", "int arithmetic wraps without refusal: 60 x '1' has " + exact + " decodings, the member "
                + "returns " + wrapped + " (overflowing inputs=" + decodeOverflows + "); Character.isDigit admits "
                + "non-ASCII digits: decodeWays(\"\\u0661\") = " + DynamicProgrammingAlgorithms.decodeWays("\u0661"));
    }

    private static long minCost(int[] cost, int top) {
        return top <= 1 ? 0 : Math.min(minCost(cost, top - 1) + cost[top - 1], minCost(cost, top - 2) + cost[top - 2]);
    }

    private static long ways(int[] coins, int from, int amount) {
        if (amount == 0) {
            return 1;
        }
        long total = 0;
        for (int k = from; k < coins.length; k++) {
            total += coins[k] <= amount ? ways(coins, k, amount - coins[k]) : 0;
        }
        return total;
    }

    private static long minPath(int[][] grid, int r, int c) {
        long here = grid[r][c];
        if (r == grid.length - 1 && c == grid[0].length - 1) {
            return here;
        }
        long down = r + 1 < grid.length ? minPath(grid, r + 1, c) : Long.MAX_VALUE;
        long right = c + 1 < grid[0].length ? minPath(grid, r, c + 1) : Long.MAX_VALUE;
        return here + Math.min(down, right);
    }

    private static int decode(String text, int at) {
        if (at == text.length()) {
            return 1;
        }
        if (text.charAt(at) == '0') {
            return 0;
        }
        int total = decode(text, at + 1);
        if (at + 1 < text.length() && Integer.parseInt(text.substring(at, at + 2)) <= 26) {
            total += decode(text, at + 2);
        }
        return total;
    }

    // ---------------------------------------------------------------- backtracking, matrices, bits

    @Test
    void backtrackingMatrixAndBitMembersMatchBruteForce() {
        SplittableRandom random = new SplittableRandom(SEED + 3);
        long cases = 0;
        for (int n = 0; n <= 12; n++) {
            for (int k = 0; k <= n; k++, cases++) {
                List<String> expected = new ArrayList<>();
                for (int mask = 0; mask < 1 << n; mask++) {
                    if (Integer.bitCount(mask) == k) {
                        int[] pick = new int[k];
                        int w = 0;
                        for (int bit = 0; bit < n; bit++) {
                            if (((mask >> bit) & 1) == 1) {
                                pick[w++] = bit + 1;
                            }
                        }
                        expected.add(Arrays.toString(pick));
                    }
                }
                List<String> got = new ArrayList<>();
                for (int[] row : BacktrackingAlgorithms.combinations(n, k)) {
                    got.add(Arrays.toString(row));
                }
                expected.sort(null);
                got.sort(null);
                assertEquals(expected, got, n + "C" + k);
            }
        }
        int[] queens = {1, 1, 0, 0, 2, 10, 4, 40, 92, 352, 724};
        for (int n = 0; n < queens.length; n++, cases++) {
            assertEquals(queens[n], BacktrackingAlgorithms.nQueensCount(n), "queens " + n);
        }
        for (int i = 0; i < 2_000; i++, cases++) {
            int[] candidates = random.ints(1, 9).limit(random.nextInt(1, 5)).toArray();
            int target = random.nextInt(0, 20);
            Set<String> expected = new HashSet<>();
            sums(Arrays.stream(candidates).distinct().sorted().toArray(), 0, target, new ArrayList<>(), expected);
            Set<String> got = new HashSet<>();
            for (int[] row : BacktrackingAlgorithms.combinationSum(candidates, target)) {
                got.add(Arrays.toString(row));
            }
            assertEquals(expected, got, Arrays.toString(candidates) + " " + target);
        }
        for (int i = 0; i < 150; i++, cases++) {
            char[][] board = AlgorithmExtrasDump.sudoku(random);
            boolean wellFormed = board.length == 9 && Arrays.stream(board).allMatch(row -> row.length == 9)
                    && Arrays.stream(board).allMatch(row -> new String(row).matches("[1-9.]{9}"));
            if (!wellFormed || !consistent(board)) {
                assertThrows(IllegalArgumentException.class, () -> BacktrackingAlgorithms.solveSudoku(board));
                continue;
            }
            char[][] clues = Arrays.stream(board).map(char[]::clone).toArray(char[][]::new);
            boolean solvable = solve(Arrays.stream(board).map(char[]::clone).toArray(char[][]::new));
            assertEquals(solvable, BacktrackingAlgorithms.solveSudoku(board));
            if (!solvable) {
                continue;
            }
            for (int r = 0; r < 9; r++) {
                for (int c = 0; c < 9; c++) {
                    assertTrue(clues[r][c] == '.' || clues[r][c] == board[r][c]);
                }
            }
            assertTrue(consistent(board) && Arrays.stream(board).noneMatch(row -> new String(row).contains(".")));
        }
        report("backtracking", "combinations+nQueensCount+combinationSum+solveSudoku", cases, cases);
        String empty;
        try {
            empty = "returns " + BacktrackingAlgorithms.combinationSum(new int[0], 5).length + " rows";
        } catch (RuntimeException e) {
            empty = "throws " + e.getClass().getSimpleName();
        }
        finding("combinationSum", "no candidates and a positive target " + empty + " (sorted[0] is read before the "
                + "search); an empty result is the LC 39 answer");
        long matrix = 0;
        for (int i = 0; i < CASES; i++, matrix += 3) {
            int rows = random.nextInt(1, 7);
            int columns = random.nextInt(1, 7);
            int[][] grid = new int[rows][columns];
            for (int[] row : grid) {
                for (int c = 0; c < columns; c++) {
                    row[c] = random.nextInt(3) == 0 ? 1 : 0;
                }
            }
            assertEquals(bfs8(grid), MatrixAlgorithms.shortestPathBinaryMatrix(grid));
            int best = 0;
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < columns; c++) {
                    for (int size = 1; r + size <= rows && c + size <= columns; size++) {
                        boolean full = true;
                        for (int y = r; y < r + size; y++) {
                            for (int x = c; x < c + size; x++) {
                                full &= grid[y][x] == 0;
                            }
                        }
                        best = full ? Math.max(best, size) : best;
                    }
                }
            }
            int[][] inverted = new int[rows][columns];
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < columns; c++) {
                    inverted[r][c] = 1 - grid[r][c];
                }
            }
            assertEquals(best * best, MatrixAlgorithms.maximalSquareArea(inverted));
            int sr = random.nextInt(rows);
            int sc = random.nextInt(columns);
            int[][] filled = MatrixAlgorithms.floodFill(grid, sr, sc, 7);
            int[][] expected = MatrixAlgorithms.deepCopy(grid);
            fill(expected, sr, sc, grid[sr][sc], 7);
            assertTrue(Arrays.deepEquals(expected, filled));
        }
        report("matrix", "shortestPathBinaryMatrix+maximalSquareArea+floodFill+deepCopy", matrix, matrix);
        long bits = 0;
        for (int i = 0; i < CASES; i++, bits += 4) {
            int x = random.nextInt();
            int y = random.nextInt();
            assertEquals(x > 0 && Integer.bitCount(x) == 1, BitAlgorithms.isPowerOfTwo(x));
            assertEquals(x + y, BitAlgorithms.addWithoutPlus(x, y));
            int low = random.nextInt(0, 100_000);
            int high = low + random.nextInt(0, 300);
            int and = low;
            for (int v = low; v <= high; v++) {
                and &= v;
            }
            assertEquals(and, BitAlgorithms.rangeBitwiseAnd(low, high));
            int first = random.nextInt();
            int second = random.nextInt();
            if (first == second || first == 5 || first == 9 || second == 5 || second == 9) {
                continue;
            }
            int[] values = {first, 5, 9, second, 9, 5};
            assertArrayEquals(new int[] {Math.min(first, second), Math.max(first, second)},
                    BitAlgorithms.twoSingleNumbers(values));
        }
        report("bit", "isPowerOfTwo+addWithoutPlus+rangeBitwiseAnd+twoSingleNumbers", bits, bits);
    }

    private static void sums(int[] candidates, int from, int remaining, List<Integer> path, Set<String> out) {
        if (remaining == 0) {
            out.add(path.toString());
            return;
        }
        for (int k = from; k < candidates.length; k++) {
            if (candidates[k] <= remaining) {
                path.add(candidates[k]);
                sums(candidates, k, remaining - candidates[k], path, out);
                path.remove(path.size() - 1);
            }
        }
    }

    /** Independent solver: the empty cell with the fewest candidates first, digits tried in order. */
    private static boolean solve(char[][] board) {
        int bestRow = -1;
        int bestColumn = -1;
        List<Character> bestOptions = null;
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                if (board[r][c] != '.') {
                    continue;
                }
                List<Character> options = new ArrayList<>();
                for (char d = '1'; d <= '9'; d++) {
                    board[r][c] = d;
                    if (consistent(board)) {
                        options.add(d);
                    }
                    board[r][c] = '.';
                }
                if (bestOptions == null || options.size() < bestOptions.size()) {
                    bestRow = r;
                    bestColumn = c;
                    bestOptions = options;
                }
            }
        }
        if (bestOptions == null) {
            return true;
        }
        for (char d : bestOptions) {
            board[bestRow][bestColumn] = d;
            if (solve(board)) {
                return true;
            }
        }
        board[bestRow][bestColumn] = '.';
        return false;
    }

    private static boolean consistent(char[][] board) {
        for (int i = 0; i < 9; i++) {
            int row = 0;
            int column = 0;
            int box = 0;
            for (int j = 0; j < 9; j++) {
                int a = bit(board[i][j]);
                int b = bit(board[j][i]);
                int c = bit(board[i / 3 * 3 + j / 3][i % 3 * 3 + j % 3]);
                if ((row & a) != 0 || (column & b) != 0 || (box & c) != 0) {
                    return false;
                }
                row |= a;
                column |= b;
                box |= c;
            }
        }
        return true;
    }

    private static int bit(char cell) {
        return cell == '.' ? 0 : 1 << (cell - '1');
    }

    private static int bfs8(int[][] grid) {
        int rows = grid.length;
        int columns = grid[0].length;
        if (grid[0][0] != 0 || grid[rows - 1][columns - 1] != 0) {
            return -1;
        }
        int[][] distance = new int[rows][columns];
        for (int[] row : distance) {
            Arrays.fill(row, -1);
        }
        distance[0][0] = 1;
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < columns; c++) {
                    for (int dr = -1; dr <= 1 && distance[r][c] > 0; dr++) {
                        for (int dc = -1; dc <= 1; dc++) {
                            int y = r + dr;
                            int x = c + dc;
                            if (y >= 0 && y < rows && x >= 0 && x < columns && grid[y][x] == 0
                                    && (distance[y][x] < 0 || distance[y][x] > distance[r][c] + 1)) {
                                distance[y][x] = distance[r][c] + 1;
                                changed = true;
                            }
                        }
                    }
                }
            }
        }
        return distance[rows - 1][columns - 1];
    }

    private static void fill(int[][] image, int r, int c, int original, int replacement) {
        if (r < 0 || r >= image.length || c < 0 || c >= image[0].length || image[r][c] != original
                || original == replacement) {
            return;
        }
        image[r][c] = replacement;
        fill(image, r + 1, c, original, replacement);
        fill(image, r - 1, c, original, replacement);
        fill(image, r, c + 1, original, replacement);
        fill(image, r, c - 1, original, replacement);
    }

    // ---------------------------------------------------------------- linked lists and the primitive trie

    @Test
    void listAndTrieMembersMatchModels() {
        SplittableRandom random = new SplittableRandom(SEED + 4);
        for (int i = 0; i < CASES; i++) {
            int n = random.nextInt(0, 15);
            int[] values = ints(random, n, 20);
            int[] next = new int[n];
            for (int k = 0; k < n; k++) {
                next[k] = random.nextInt(3) == 0 ? random.nextInt(-1, n) : k + 1 < n ? k + 1 : -1;
            }
            int head = n == 0 ? -1 : random.nextInt(n);
            LinkedListAlgorithms.IntLinkedList list = new LinkedListAlgorithms.IntLinkedList(values, next, head);
            List<Integer> walk = new ArrayList<>();
            Set<Integer> seen = new HashSet<>();
            int cycle = -1;
            for (int node = head; node >= 0; node = next[node]) {
                if (!seen.add(node)) {
                    cycle = node;
                    break;
                }
                walk.add(values[node]);
            }
            assertEquals(cycle, LinkedListAlgorithms.cycleStartIndex(list));
            assertEquals(cycle >= 0, LinkedListAlgorithms.hasCycle(list));
            if (cycle >= 0) {
                assertThrows(IllegalArgumentException.class, () -> LinkedListAlgorithms.toArray(list));
                continue;
            }
            assertEquals(walk, box(LinkedListAlgorithms.toArray(list)));
            List<Integer> reordered = new ArrayList<>();
            for (int l = 0, r = walk.size() - 1; l <= r; l++, r--) {
                reordered.add(walk.get(l));
                if (l != r) {
                    reordered.add(walk.get(r));
                }
            }
            assertEquals(reordered, box(LinkedListAlgorithms.toArray(LinkedListAlgorithms.reordered(list))));
            assertEquals(walk, box(LinkedListAlgorithms.toArray(LinkedListAlgorithms.IntLinkedList.linear(
                    walk.stream().mapToInt(Integer::intValue).toArray()))));
        }
        report("linked-list", "cycleStartIndex+hasCycle+toArray+reordered+IntLinkedList.linear", 5L * CASES,
                5L * CASES);
        long trieOps = 0;
        for (int run = 0; run < 2_000; run++) {
            PrimitiveTrie trie = new PrimitiveTrie();
            Set<String> words = new HashSet<>();
            Set<String> prefixes = new HashSet<>(Set.of(""));
            for (int op = 0; op < 50; op++, trieOps++) {
                String word = randomWord(random);
                if (random.nextInt(3) == 0) {
                    trie.add(word);
                    words.add(word);
                    for (int k = 0; k <= word.length(); k++) {
                        prefixes.add(word.substring(0, k));
                    }
                    assertEquals(prefixes.size(), trie.nodeCount());
                    assertEquals(prefixes.size() - 1, trie.edgeCount());
                }
                assertEquals(words.contains(word), trie.contains(word));
                assertEquals(prefixes.contains(word), trie.hasPrefix(word));
                int longest = -1;
                for (int k = 0; k <= word.length(); k++) {
                    longest = words.contains(word.substring(0, k)) ? k : longest;
                }
                assertEquals(longest, trie.longestStoredPrefixLength(word));
            }
        }
        report("trie", "M3PrimitiveTrie add+contains+hasPrefix+longestStoredPrefixLength+nodeCount+edgeCount",
                trieOps, trieOps);
    }

    private static String randomWord(SplittableRandom random) {
        char[] chars = new char[random.nextInt(0, 6)];
        String alphabet = "ab\u00e9\ud83d\ude00";
        for (int k = 0; k < chars.length; k++) {
            chars[k] = alphabet.charAt(random.nextInt(alphabet.length()));
        }
        return new String(chars);
    }
}
