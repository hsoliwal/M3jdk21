// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import java.util.List;
import java.util.Objects;

/** Stable catalog of canonical reusable algorithm shapes rather than problem-number wrappers. */
//FLLDCIM: RELOCATE - donor blob 1501a5fc01b0 at commit 3385d79a2968 (repository, path, URL and digests: m3/algorithms/recipe/families-20261106/PROVENANCE.tsv); recipe m3/algorithms/recipe/families-20261106/relocate.py v1
public final class M3AlgorithmShapeCatalog {
    private M3AlgorithmShapeCatalog() {
    }

    public static List<Shape> shapes() {
        return SHAPES;
    }

    public static List<Shape> family(Family family) {
        Objects.requireNonNull(family, "family");
        return SHAPES.stream().filter(shape -> shape.family() == family).toList();
    }

    public enum Family {
        ARRAY,
        TWO_POINTER,
        SLIDING_WINDOW,
        PREFIX_SUM,
        BINARY_SEARCH,
        LINKED_LIST,
        STACK_QUEUE,
        TREE_BST,
        HEAP,
        GRAPH,
        UNION_FIND,
        TRIE,
        BACKTRACKING,
        DYNAMIC_PROGRAMMING,
        GREEDY_INTERVAL,
        BITWISE,
        STRING,
        MATRIX
    }

    public record Shape(
            String id,
            Family family,
            String time,
            String space,
            String entryPoint) {
        public Shape {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(family, "family");
            Objects.requireNonNull(time, "time");
            Objects.requireNonNull(space, "space");
            Objects.requireNonNull(entryPoint, "entryPoint");
        }
    }

    private static final List<Shape> SHAPES = List.of(
            new Shape("hash-two-sum", Family.ARRAY, "O(n)", "O(n)", "M3ArrayAlgorithms.twoSum"),
            new Shape("kadane", Family.ARRAY, "O(n)", "O(1)", "M3ArrayAlgorithms.maxSubarray"),
            new Shape("product-except-self", Family.PREFIX_SUM, "O(n)", "O(1) aux", "M3ArrayAlgorithms.productExceptSelf"),
            new Shape("dutch-flag", Family.TWO_POINTER, "O(n)", "O(1)", "M3ArrayAlgorithms.sortColors"),
            new Shape("sorted-two-sum", Family.TWO_POINTER, "O(n)", "O(1)", "M3ArrayAlgorithms.sortedTwoSum"),
            new Shape("three-sum", Family.TWO_POINTER, "O(n^2)", "O(n) output excluded", "M3ArrayAlgorithms.threeSum"),
            new Shape("rain-water", Family.TWO_POINTER, "O(n)", "O(1)", "M3ArrayAlgorithms.trapRainWater"),
            new Shape("distinct-window", Family.SLIDING_WINDOW, "O(n)", "O(k)", "M3ArrayAlgorithms.longestDistinctSubstring"),
            new Shape("minimum-sum-window", Family.SLIDING_WINDOW, "O(n)", "O(1)", "M3ArrayAlgorithms.minSubarrayLengthAtLeast"),
            new Shape("monotonic-window-max", Family.SLIDING_WINDOW, "O(n)", "O(k)", "M3ArrayAlgorithms.maxSlidingWindow"),
            new Shape("prefix-frequency", Family.PREFIX_SUM, "O(n)", "O(n)", "M3ArrayAlgorithms.subarraySumEqualsK"),
            new Shape("prefix-range-sum", Family.PREFIX_SUM, "O(n) build/O(1) query", "O(n)", "M3ArrayAlgorithms.prefixSums"),
            new Shape("pivot-index", Family.PREFIX_SUM, "O(n)", "O(1)", "M3ArrayAlgorithms.pivotIndex"),
            new Shape("lower-upper-bound", Family.BINARY_SEARCH, "O(log n)", "O(1)", "M3ArrayAlgorithms.lowerBound"),
            new Shape("rotated-search", Family.BINARY_SEARCH, "O(log n)", "O(1)", "M3ArrayAlgorithms.searchRotated"),
            new Shape("first-true-predicate", Family.BINARY_SEARCH, "O(log n)", "O(1)", "M3ArrayAlgorithms.firstTrue"),
            new Shape("floyd-cycle", Family.LINKED_LIST, "O(n)", "O(1)", "M3ListAlgorithms.cycleStart"),
            new Shape("reverse-list", Family.LINKED_LIST, "O(n)", "O(1)", "M3ListAlgorithms.reverse"),
            new Shape("monotonic-stack", Family.STACK_QUEUE, "O(n)", "O(n)", "M3StackAlgorithms.nextGreater"),
            new Shape("largest-histogram", Family.STACK_QUEUE, "O(n)", "O(n)", "M3StackAlgorithms.largestRectangleArea"),
            new Shape("iterative-tree-traversal", Family.TREE_BST, "O(n)", "O(h)", "M3TreeAlgorithms.inorder"),
            new Shape("bst-validation", Family.TREE_BST, "O(n)", "O(h)", "M3TreeAlgorithms.isValidBst"),
            new Shape("bst-kth-smallest", Family.TREE_BST, "O(h+k)", "O(h)", "M3TreeAlgorithms.kthSmallest"),
            new Shape("top-k-heap", Family.HEAP, "O(n log k)", "O(k)", "M3ArrayAlgorithms.topKFrequent"),
            new Shape("kth-largest-heap", Family.HEAP, "O(n log k)", "O(k)", "M3HeapAlgorithms.kthLargest"),
            new Shape("merge-k-sorted", Family.HEAP, "O(n log k)", "O(k)", "M3HeapAlgorithms.mergeSorted"),
            new Shape("bfs-shortest", Family.GRAPH, "O(V+E)", "O(V)", "GraphAlgorithms.shortestPaths"),
            new Shape("dijkstra", Family.GRAPH, "O((V+E) log V)", "O(V+E)", "GraphAlgorithms.dijkstra"),
            new Shape("topological-kahn", Family.GRAPH, "O(V+E)", "O(V)", "GraphAlgorithms.topologicalOrder"),
            new Shape("kosaraju-scc", Family.GRAPH, "O(V+E)", "O(V+E)", "GraphAlgorithms.stronglyConnectedComponents"),
            new Shape("disjoint-set", Family.UNION_FIND, "amortized α(n)", "O(n)", "IntUnionFind.union"),
            new Shape("prefix-trie", Family.TRIE, "O(m)", "O(total chars)", "StringTrie.insert"),
            new Shape("subsets", Family.BACKTRACKING, "O(2^n)", "O(n)", "M3BacktrackingAlgorithms.subsets"),
            new Shape("permutations", Family.BACKTRACKING, "O(n*n!)", "O(n)", "M3BacktrackingAlgorithms.permutations"),
            new Shape("n-queens", Family.BACKTRACKING, "exponential", "O(n)", "M3BacktrackingAlgorithms.nQueens"),
            new Shape("balanced-parentheses", Family.BACKTRACKING, "Catalan", "O(n)", "M3BacktrackingAlgorithms.generateParentheses"),
            new Shape("coin-change", Family.DYNAMIC_PROGRAMMING, "O(amount*coins)", "O(amount)", "M3DynamicProgrammingAlgorithms.coinChange"),
            new Shape("lis-patience", Family.DYNAMIC_PROGRAMMING, "O(n log n)", "O(n)", "M3DynamicProgrammingAlgorithms.longestIncreasingSubsequence"),
            new Shape("lcs", Family.DYNAMIC_PROGRAMMING, "O(mn)", "O(min(m,n))", "M3DynamicProgrammingAlgorithms.longestCommonSubsequence"),
            new Shape("lcs-witness", Family.DYNAMIC_PROGRAMMING, "O(mn)", "O(min(m,n))", "M3IntLcsWitness.javaWitness"),
            new Shape("edit-distance", Family.DYNAMIC_PROGRAMMING, "O(mn)", "O(min(m,n))", "M3DynamicProgrammingAlgorithms.editDistance"),
            new Shape("word-break", Family.DYNAMIC_PROGRAMMING, "O(n*L)", "O(n)", "M3DynamicProgrammingAlgorithms.wordBreak"),
            new Shape("unique-grid-paths", Family.DYNAMIC_PROGRAMMING, "O(mn)", "O(n)", "M3DynamicProgrammingAlgorithms.uniquePaths"),
            new Shape("interval-merge", Family.GREEDY_INTERVAL, "O(n log n)", "O(n)", "M3IntervalGreedyAlgorithms.merge"),
            new Shape("interval-insert", Family.GREEDY_INTERVAL, "O(n)", "O(n)", "M3IntervalGreedyAlgorithms.insert"),
            new Shape("jump-greedy", Family.GREEDY_INTERVAL, "O(n)", "O(1)", "M3IntervalGreedyAlgorithms.minimumJumps"),
            new Shape("bit-count", Family.BITWISE, "O(set bits)", "O(1)", "M3BitAlgorithms.hammingWeight"),
            new Shape("kmp", Family.STRING, "O(n+m)", "O(m)", "M3StringAlgorithms.kmpSearch"),
            new Shape("manacher", Family.STRING, "O(n)", "O(n)", "M3StringAlgorithms.longestPalindrome"),
            new Shape("z-search", Family.STRING, "O(n+m)", "O(m)", "M3ZAlgorithm.zSearch"),
            new Shape("one-mismatch-window", Family.STRING, "O(n+m)", "O(n+m)", "M3OneMismatchWindowSearch.allIndices"),
            new Shape("matrix-spiral", Family.MATRIX, "O(mn)", "O(1) aux", "M3MatrixAlgorithms.spiralOrder"),
            new Shape("island-flood", Family.MATRIX, "O(mn)", "O(mn)", "M3MatrixAlgorithms.countIslands"),
            new Shape("matrix-zeroes", Family.MATRIX, "O(mn)", "O(1)", "M3MatrixAlgorithms.setZeroes")
    );
}
