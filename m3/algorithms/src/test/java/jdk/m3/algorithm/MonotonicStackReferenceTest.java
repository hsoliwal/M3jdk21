// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Random;
import org.junit.jupiter.api.Test;

//FLLDCIM: TOP - M3JDK-authored reference oracle of recipe/monotonic-stack-fold (the donors carry no tests): every folded
// monotonic-stack kernel against a quadratic or exhaustive formulation on small random inputs
final class MonotonicStackReferenceTest {
    private static final int TRIALS = 400;
    private static final int MOD = 1_000_000_007;

    private static int[] randomValues(Random random, int bound) {
        int[] values = new int[random.nextInt(9)];
        for (int i = 0; i < values.length; i++) {
            values[i] = random.nextInt(bound);
        }
        return values;
    }

    @Test
    void neighbourKernelsAgreeWithQuadraticScan() {
        Random random = new Random(0x5AC4);
        for (int trial = 0; trial < TRIALS; trial++) {
            int[] values = randomValues(random, 7);
            int n = values.length;
            int[] nextSmaller = new int[n];
            int[] previousGreater = new int[n];
            int[] previousSmaller = new int[n];
            int[] nextGreaterIndex = new int[n];
            int[] previousSmallerIndex = new int[n];
            int[] stockSpan = new int[n];
            for (int i = 0; i < n; i++) {
                nextSmaller[i] = -1;
                nextGreaterIndex[i] = -1;
                for (int j = i + 1; j < n; j++) {
                    if (nextSmaller[i] == -1 && values[j] < values[i]) {
                        nextSmaller[i] = values[j];
                    }
                    if (nextGreaterIndex[i] == -1 && values[j] > values[i]) {
                        nextGreaterIndex[i] = j;
                    }
                }
                previousGreater[i] = -1;
                previousSmaller[i] = -1;
                previousSmallerIndex[i] = -1;
                for (int j = i - 1; j >= 0; j--) {
                    if (previousGreater[i] == -1 && values[j] > values[i]) {
                        previousGreater[i] = values[j];
                    }
                    if (previousSmaller[i] == -1 && values[j] < values[i]) {
                        previousSmaller[i] = values[j];
                        previousSmallerIndex[i] = j;
                    }
                }
                int span = 1;
                while (i - span >= 0 && values[i - span] <= values[i]) {
                    span++;
                }
                stockSpan[i] = span;
            }
            assertArrayEquals(nextSmaller, StackAlgorithms.nextSmaller(values), "nextSmaller");
            assertArrayEquals(previousGreater, StackAlgorithms.previousGreater(values), "previousGreater");
            assertArrayEquals(previousSmaller, StackAlgorithms.previousSmaller(values), "previousSmaller");
            assertArrayEquals(nextGreaterIndex, StackAlgorithms.nextGreaterIndex(values), "nextGreaterIndex");
            assertArrayEquals(previousSmallerIndex, StackAlgorithms.previousSmallerIndex(values), "previousSmallerIndex");
            assertArrayEquals(stockSpan, StackAlgorithms.stockSpan(values), "stockSpan");
        }
    }

    @Test
    void subarrayExtremeSumsAgreeWithEnumeration() {
        Random random = new Random(0x5B5);
        for (int trial = 0; trial < TRIALS; trial++) {
            int[] values = randomValues(random, 50);
            long mins = 0;
            long maxs = 0;
            for (int start = 0; start < values.length; start++) {
                int min = values[start];
                int max = values[start];
                for (int end = start; end < values.length; end++) {
                    min = Math.min(min, values[end]);
                    max = Math.max(max, values[end]);
                    mins += min;
                    maxs += max;
                }
            }
            assertEquals((int) (mins % MOD), StackAlgorithms.sumSubarrayMins(values), "sumSubarrayMins");
            assertEquals((int) (maxs % MOD), StackAlgorithms.sumSubarrayMaxs(values), "sumSubarrayMaxs");
        }
    }

    @Test
    void maximalRectangleAgreesWithExhaustiveRectangles() {
        Random random = new Random(0x2EC7);
        for (int trial = 0; trial < TRIALS; trial++) {
            int rows = 1 + random.nextInt(5);
            int columns = 1 + random.nextInt(5);
            int[][] matrix = new int[rows][columns];
            for (int[] row : matrix) {
                for (int j = 0; j < columns; j++) {
                    row[j] = random.nextInt(3) == 0 ? 0 : 1;
                }
            }
            int best = 0;
            for (int top = 0; top < rows; top++) {
                for (int bottom = top; bottom < rows; bottom++) {
                    for (int left = 0; left < columns; left++) {
                        for (int right = left; right < columns; right++) {
                            boolean full = true;
                            for (int r = top; r <= bottom && full; r++) {
                                for (int c = left; c <= right; c++) {
                                    if (matrix[r][c] == 0) {
                                        full = false;
                                        break;
                                    }
                                }
                            }
                            if (full) {
                                best = Math.max(best, (bottom - top + 1) * (right - left + 1));
                            }
                        }
                    }
                }
            }
            assertEquals(best, StackAlgorithms.maximalRectangle(matrix), "maximalRectangle");
        }
        assertEquals(0, StackAlgorithms.maximalRectangle(new int[0][0]));
    }

    @Test
    void parenthesesAndSlidingMinimumAgreeWithEnumeration() {
        Random random = new Random(0x9A4E);
        for (int trial = 0; trial < TRIALS; trial++) {
            int length = random.nextInt(11);
            StringBuilder text = new StringBuilder();
            for (int i = 0; i < length; i++) {
                text.append(random.nextBoolean() ? '(' : ')');
            }
            String s = text.toString();
            int longest = 0;
            for (int start = 0; start < length; start++) {
                for (int end = start + 2; end <= length; end += 2) {
                    if (balanced(s.substring(start, end))) {
                        longest = Math.max(longest, end - start);
                    }
                }
            }
            assertEquals(longest, StackAlgorithms.longestValidParentheses(s), "longestValidParentheses " + s);
            int keep = 0;
            for (int mask = 0; mask < (1 << length); mask++) {
                StringBuilder kept = new StringBuilder();
                for (int i = 0; i < length; i++) {
                    if ((mask & (1 << i)) != 0) {
                        kept.append(s.charAt(i));
                    }
                }
                if (balanced(kept.toString())) {
                    keep = Math.max(keep, kept.length());
                }
            }
            assertEquals(length - keep, StackAlgorithms.minRemoveToMakeValid(s), "minRemoveToMakeValid " + s);

            int[] values = randomValues(random, 100);
            if (values.length == 0) {
                assertArrayEquals(new int[0], ArrayAlgorithms.minSlidingWindow(values, 1));
                continue;
            }
            int k = 1 + random.nextInt(values.length);
            int[] expected = new int[values.length - k + 1];
            for (int start = 0; start < expected.length; start++) {
                int min = values[start];
                for (int i = start; i < start + k; i++) {
                    min = Math.min(min, values[i]);
                }
                expected[start] = min;
            }
            assertArrayEquals(expected, ArrayAlgorithms.minSlidingWindow(values, k), "minSlidingWindow");
        }
    }

    private static boolean balanced(String s) {
        int depth = 0;
        for (int i = 0; i < s.length(); i++) {
            depth += s.charAt(i) == '(' ? 1 : -1;
            if (depth < 0) {
                return false;
            }
        }
        return depth == 0;
    }
}
