// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import jdk.m3.algorithm.ProgressMonitor;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

//FLLDCIM: RELOCATE - donor blob d8110dab1fe8 at commit 3385d79a2968 (repository, path, URL and digests: m3/algorithms/recipe/families-20261106/PROVENANCE.tsv); recipe m3/algorithms/recipe/families-20261106/relocate.py v1
class AlgorithmFamiliesTest {
    private static final ProgressMonitor NONE = ProgressMonitor.none();

    @Test
    void arraysWindowsSearchAndHeapShapes() {
        assertArrayEquals(new int[] {0, 1},
                ArrayAlgorithms.twoSum(new int[] {2, 7, 11, 15}, 9, NONE));
        assertEquals(6L,
                ArrayAlgorithms.maxSubarray(new int[] {-2, 1, -3, 4, -1, 2, 1, -5, 4}, NONE));
        assertArrayEquals(new long[] {24, 12, 8, 6},
                ArrayAlgorithms.productExceptSelf(new int[] {1, 2, 3, 4}, NONE));
        assertEquals(4, ArrayAlgorithms.longestConsecutive(
                new int[] {100, 4, 200, 1, 3, 2}, NONE));

        int[] colors = {2, 0, 2, 1, 1, 0};
        ArrayAlgorithms.sortColors(colors, NONE);
        assertArrayEquals(new int[] {0, 0, 1, 1, 2, 2}, colors);

        assertEquals(List.of(
                        List.of(-1, -1, 2),
                        List.of(-1, 0, 1)),
                ArrayAlgorithms.threeSum(new int[] {-1, 0, 1, 2, -1, -4}, NONE));
        assertArrayEquals(new int[] {1, 3},
                ArrayAlgorithms.sortedTwoSum(new int[] {1, 2, 4, 7, 11}, 9, NONE));
        assertEquals(2, ArrayAlgorithms.minSubarrayLengthAtLeast(
                7, new int[] {2, 3, 1, 2, 4, 3}, NONE));
        assertEquals(6L, ArrayAlgorithms.trapRainWater(
                new int[] {0, 1, 0, 2, 1, 0, 1, 3, 2, 1, 2, 1}, NONE));
        assertEquals(3, ArrayAlgorithms.longestDistinctSubstring("abcabcbb", NONE));
        assertArrayEquals(new int[] {3, 3, 5, 5, 6, 7},
                ArrayAlgorithms.maxSlidingWindow(
                        new int[] {1, 3, -1, -3, 5, 3, 6, 7}, 3, NONE));
        assertEquals(2L, ArrayAlgorithms.subarraySumEqualsK(
                new int[] {1, 1, 1}, 2, NONE));
        long[] prefix = ArrayAlgorithms.prefixSums(new int[] {3, -1, 4, 2}, NONE);
        assertEquals(5L, ArrayAlgorithms.rangeSum(prefix, 1, 4, NONE));
        assertEquals(3, ArrayAlgorithms.pivotIndex(
                new int[] {1, 7, 3, 6, 5, 6}, NONE));
        assertEquals(1, ArrayAlgorithms.lowerBound(new int[] {1, 2, 2, 4}, 2, NONE));
        assertEquals(3, ArrayAlgorithms.upperBound(new int[] {1, 2, 2, 4}, 2, NONE));
        assertEquals(4, ArrayAlgorithms.searchRotated(
                new int[] {4, 5, 6, 7, 0, 1, 2}, 0, NONE));
        assertEquals(7, ArrayAlgorithms.firstTrue(0, 20, value -> value >= 7, NONE));
        assertEquals(List.of(1, 2),
                ArrayAlgorithms.topKFrequent(new int[] {1, 1, 1, 2, 2, 3}, 2, NONE));
        assertEquals(5, HeapAlgorithms.kthLargest(
                new int[] {3, 2, 1, 5, 6, 4}, 2, NONE));
        assertEquals(List.of(1, 2, 3, 4, 5, 6),
                HeapAlgorithms.mergeSorted(
                        List.of(List.of(1, 4), List.of(2, 5), List.of(3, 6)),
                        Integer::compare,
                        NONE));
    }

    @Test
    void listStackAndTreeShapes() {
        ListAlgorithms.Node<Integer> first = new ListAlgorithms.Node<>(1);
        ListAlgorithms.Node<Integer> second = new ListAlgorithms.Node<>(2);
        ListAlgorithms.Node<Integer> third = new ListAlgorithms.Node<>(3);
        first.next(second);
        second.next(third);
        assertEquals(2, ListAlgorithms.middle(first, NONE).value());
        ListAlgorithms.Node<Integer> reversed = ListAlgorithms.reverse(first, NONE);
        assertEquals(3, reversed.value());
        assertEquals(2, reversed.next().value());

        first.next(second);
        second.next(third);
        third.next(second);
        assertEquals(second, ListAlgorithms.cycleStart(first, NONE));

        assertTrue(StackAlgorithms.validBrackets("([]{})", NONE));
        assertFalse(StackAlgorithms.validBrackets("([)]", NONE));
        assertArrayEquals(new int[] {4, 2, 4, -1, -1},
                StackAlgorithms.nextGreater(new int[] {2, 1, 2, 4, 3}, NONE));
        assertEquals(10L,
                StackAlgorithms.largestRectangleArea(new int[] {2, 1, 5, 6, 2, 3}, NONE));
        assertEquals(9L, StackAlgorithms.evaluateRpn(
                new String[] {"2", "1", "+", "3", "*"}, NONE));

        TreeAlgorithms.Node<Integer> root = new TreeAlgorithms.Node<>(4)
                .left(new TreeAlgorithms.Node<>(2)
                        .left(new TreeAlgorithms.Node<>(1))
                        .right(new TreeAlgorithms.Node<>(3)))
                .right(new TreeAlgorithms.Node<>(6)
                        .left(new TreeAlgorithms.Node<>(5))
                        .right(new TreeAlgorithms.Node<>(7)));
        assertEquals(List.of(1, 2, 3, 4, 5, 6, 7), TreeAlgorithms.inorder(root, NONE));
        assertEquals(3, TreeAlgorithms.maxDepth(root, NONE));
        assertTrue(TreeAlgorithms.isBalanced(root, NONE));
        assertTrue(TreeAlgorithms.isValidBst(root, Integer::compare, NONE));
        assertEquals(5, TreeAlgorithms.kthSmallest(root, 5, NONE));
        assertEquals(4, TreeAlgorithms.lowestCommonAncestor(
                root, root.left().left(), root.right().right(), NONE).value());
    }

    @Test
    void backtrackingDynamicProgrammingAndGreedyShapes() {
        assertEquals(8, BacktrackingAlgorithms.subsets(new int[] {1, 2, 3}, NONE).size());
        assertEquals(6, BacktrackingAlgorithms.permutations(new int[] {1, 2, 3}, NONE).size());
        assertEquals(List.of(
                        List.of(1, 1, 6),
                        List.of(1, 2, 5),
                        List.of(1, 7),
                        List.of(2, 6)),
                BacktrackingAlgorithms.combinationSumUnique(
                        new int[] {10, 1, 2, 7, 6, 1, 5}, 8, NONE));
        assertEquals(2, BacktrackingAlgorithms.nQueens(4, NONE).size());
        assertEquals(List.of("((()))", "(()())", "(())()", "()(())", "()()()"),
                BacktrackingAlgorithms.generateParentheses(3, NONE));

        assertEquals(3,
                DynamicProgrammingAlgorithms.coinChange(new int[] {1, 2, 5}, 11, NONE));
        assertEquals(4,
                DynamicProgrammingAlgorithms.longestIncreasingSubsequence(
                        new int[] {10, 9, 2, 5, 3, 7, 101, 18}, NONE));
        assertEquals(3,
                DynamicProgrammingAlgorithms.longestCommonSubsequence("abcde", "ace", NONE));
        assertEquals(3,
                DynamicProgrammingAlgorithms.editDistance("horse", "ros", NONE));
        assertTrue(DynamicProgrammingAlgorithms.wordBreak(
                "leetcode", List.of("leet", "code"), NONE));
        assertEquals(28L, DynamicProgrammingAlgorithms.uniquePaths(3, 7, NONE));
        assertEquals(12L,
                DynamicProgrammingAlgorithms.houseRobber(new int[] {2, 7, 9, 3, 1}, NONE));
        assertTrue(DynamicProgrammingAlgorithms.canPartition(
                new int[] {1, 5, 11, 5}, NONE));
        assertEquals(9L, DynamicProgrammingAlgorithms.knapsack01(
                new int[] {1, 3, 4, 5},
                new int[] {1, 4, 5, 7},
                7,
                NONE));

        List<IntervalGreedyAlgorithms.Interval> merged =
                IntervalGreedyAlgorithms.merge(List.of(
                        new IntervalGreedyAlgorithms.Interval(1, 3),
                        new IntervalGreedyAlgorithms.Interval(2, 6),
                        new IntervalGreedyAlgorithms.Interval(8, 10),
                        new IntervalGreedyAlgorithms.Interval(15, 18)), NONE);
        assertEquals(List.of(
                        new IntervalGreedyAlgorithms.Interval(1, 6),
                        new IntervalGreedyAlgorithms.Interval(8, 10),
                        new IntervalGreedyAlgorithms.Interval(15, 18)), merged);
        assertEquals(
                List.of(
                        new IntervalGreedyAlgorithms.Interval(1, 5),
                        new IntervalGreedyAlgorithms.Interval(6, 9)),
                IntervalGreedyAlgorithms.insert(
                        List.of(
                                new IntervalGreedyAlgorithms.Interval(1, 3),
                                new IntervalGreedyAlgorithms.Interval(6, 9)),
                        new IntervalGreedyAlgorithms.Interval(2, 5),
                        NONE));
        assertTrue(IntervalGreedyAlgorithms.canReachEnd(new int[] {2, 3, 1, 1, 4}, NONE));
        assertEquals(2, IntervalGreedyAlgorithms.minimumJumps(
                new int[] {2, 3, 1, 1, 4}, NONE));
        assertEquals(3, IntervalGreedyAlgorithms.gasStationStart(
                new int[] {1, 2, 3, 4, 5},
                new int[] {3, 4, 5, 1, 2}, NONE));
    }

    @Test
    void stringBitAndMatrixShapes() {
        assertEquals(List.of(0, 2), StringAlgorithms.kmpSearch("ababa", "aba", NONE));
        assertEquals("bab", StringAlgorithms.longestPalindrome("babad", NONE));
        assertTrue(StringAlgorithms.wildcardMatch("adceb", "*a*b", NONE));
        assertFalse(StringAlgorithms.wildcardMatch("acdcb", "a*c?b", NONE));

        assertEquals(3, BitAlgorithms.hammingWeight(0b1011, NONE));
        assertEquals(3, BitAlgorithms.singleNumberTriples(new int[] {2, 2, 3, 2}, NONE));
        assertEquals(2, BitAlgorithms.missingNumber(new int[] {3, 0, 1}, NONE));

        int[][] matrix = {
                {1, 2, 3},
                {4, 5, 6},
                {7, 8, 9}
        };
        assertEquals(List.of(1, 2, 3, 6, 9, 8, 7, 4, 5),
                MatrixAlgorithms.spiralOrder(matrix, NONE));
        MatrixAlgorithms.rotateClockwise(matrix, NONE);
        assertArrayEquals(new int[] {7, 4, 1}, matrix[0]);
        assertTrue(MatrixAlgorithms.searchSorted(new int[][] {
                {1, 4, 7},
                {2, 5, 8},
                {3, 6, 9}
        }, 6, NONE));
        int[][] zeroes = {
                {1, 1, 1},
                {1, 0, 1},
                {1, 1, 1}
        };
        MatrixAlgorithms.setZeroes(zeroes, NONE);
        assertArrayEquals(new int[] {1, 0, 1}, zeroes[0]);
        assertArrayEquals(new int[] {0, 0, 0}, zeroes[1]);
        assertEquals(3, MatrixAlgorithms.countIslands(new char[][] {
                {'1', '1', '0', '0'},
                {'1', '0', '0', '1'},
                {'0', '0', '1', '1'},
                {'1', '0', '0', '0'}
        }, NONE));
    }

    @Test
    void cancellationIsObservedInsideAlgorithmLoops() {
        ProgressMonitor cancelImmediately = new ProgressMonitor() {
            @Override
            public boolean isCanceled() {
                return true;
            }
        };
        assertThrows(CancellationException.class, () ->
                ArrayAlgorithms.maxSlidingWindow(
                        new int[] {1, 2, 3, 4}, 2, cancelImmediately));
    }

    @Test
    void catalogCoversAllCanonicalFamilies() {
        Set<AlgorithmShapeCatalog.Family> families = AlgorithmShapeCatalog.shapes().stream()
                .map(AlgorithmShapeCatalog.Shape::family)
                .collect(java.util.stream.Collectors.toSet());
        assertEquals(Set.of(AlgorithmShapeCatalog.Family.values()), families);
        assertTrue(AlgorithmShapeCatalog.shapes().size() >= 45);
    }
}
