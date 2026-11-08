// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import java.util.List;
import java.util.Objects;

/** Stable catalog of canonical reusable algorithm shapes rather than problem-number wrappers. */
//FLLDCIM: RELOCATE - donor blob 1501a5fc01b0 at commit 3385d79a2968 (repository, path, URL and digests: m3/algorithms/recipe/families-20261106/PROVENANCE.tsv); recipe m3/algorithms/recipe/families-20261106/relocate.py v1
public final class AlgorithmShapeCatalog {
    private AlgorithmShapeCatalog() {
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
            new Shape("hash-two-sum", Family.ARRAY, "O(n)", "O(n)", "ArrayAlgorithms.twoSum"),
            new Shape("kadane", Family.ARRAY, "O(n)", "O(1)", "ArrayAlgorithms.maxSubarray"),
            new Shape("product-except-self", Family.PREFIX_SUM, "O(n)", "O(1) aux", "ArrayAlgorithms.productExceptSelf"),
            new Shape("dutch-flag", Family.TWO_POINTER, "O(n)", "O(1)", "ArrayAlgorithms.sortColors"),
            new Shape("sorted-two-sum", Family.TWO_POINTER, "O(n)", "O(1)", "ArrayAlgorithms.sortedTwoSum"),
            new Shape("three-sum", Family.TWO_POINTER, "O(n^2)", "O(n) output excluded", "ArrayAlgorithms.threeSum"),
            new Shape("rain-water", Family.TWO_POINTER, "O(n)", "O(1)", "ArrayAlgorithms.trapRainWater"),
            new Shape("distinct-window", Family.SLIDING_WINDOW, "O(n)", "O(k)", "ArrayAlgorithms.longestDistinctSubstring"),
            new Shape("minimum-sum-window", Family.SLIDING_WINDOW, "O(n)", "O(1)", "ArrayAlgorithms.minSubarrayLengthAtLeast"),
            new Shape("monotonic-window-max", Family.SLIDING_WINDOW, "O(n)", "O(k)", "ArrayAlgorithms.maxSlidingWindow"),
            new Shape("prefix-frequency", Family.PREFIX_SUM, "O(n)", "O(n)", "ArrayAlgorithms.subarraySumEqualsK"),
            new Shape("prefix-range-sum", Family.PREFIX_SUM, "O(n) build/O(1) query", "O(n)", "ArrayAlgorithms.prefixSums"),
            new Shape("pivot-index", Family.PREFIX_SUM, "O(n)", "O(1)", "ArrayAlgorithms.pivotIndex"),
            new Shape("lower-upper-bound", Family.BINARY_SEARCH, "O(log n)", "O(1)", "ArrayAlgorithms.lowerBound"),
            new Shape("rotated-search", Family.BINARY_SEARCH, "O(log n)", "O(1)", "ArrayAlgorithms.searchRotated"),
            new Shape("first-true-predicate", Family.BINARY_SEARCH, "O(log n)", "O(1)", "ArrayAlgorithms.firstTrue"),
            new Shape("floyd-cycle", Family.LINKED_LIST, "O(n)", "O(1)", "ListAlgorithms.cycleStart"),
            new Shape("reverse-list", Family.LINKED_LIST, "O(n)", "O(1)", "ListAlgorithms.reverse"),
            new Shape("monotonic-stack", Family.STACK_QUEUE, "O(n)", "O(n)", "StackAlgorithms.nextGreater"),
            new Shape("largest-histogram", Family.STACK_QUEUE, "O(n)", "O(n)", "StackAlgorithms.largestRectangleArea"),
            new Shape("iterative-tree-traversal", Family.TREE_BST, "O(n)", "O(h)", "TreeAlgorithms.inorder"),
            new Shape("bst-validation", Family.TREE_BST, "O(n)", "O(h)", "TreeAlgorithms.isValidBst"),
            new Shape("bst-kth-smallest", Family.TREE_BST, "O(h+k)", "O(h)", "TreeAlgorithms.kthSmallest"),
            new Shape("top-k-heap", Family.HEAP, "O(n log k)", "O(k)", "ArrayAlgorithms.topKFrequent"),
            new Shape("kth-largest-heap", Family.HEAP, "O(n log k)", "O(k)", "HeapAlgorithms.kthLargest"),
            new Shape("merge-k-sorted", Family.HEAP, "O(n log k)", "O(k)", "HeapAlgorithms.mergeSorted"),
            new Shape("bfs-shortest", Family.GRAPH, "O(V+E)", "O(V)", "GraphAlgorithms.shortestPaths"),
            new Shape("dijkstra", Family.GRAPH, "O((V+E) log V)", "O(V+E)", "GraphAlgorithms.dijkstra"),
            new Shape("topological-kahn", Family.GRAPH, "O(V+E)", "O(V)", "GraphAlgorithms.topologicalOrder"),
            new Shape("kosaraju-scc", Family.GRAPH, "O(V+E)", "O(V+E)", "GraphAlgorithms.stronglyConnectedComponents"),
            new Shape("disjoint-set", Family.UNION_FIND, "amortized α(n)", "O(n)", "IntUnionFind.union"),
            new Shape("prefix-trie", Family.TRIE, "O(m)", "O(total chars)", "StringTrie.insert"),
            new Shape("byte-prefix-count", Family.TRIE, "O(m)", "O(total pattern bytes)", "PrefixIndex.prefixCount"),
            new Shape("subsets", Family.BACKTRACKING, "O(2^n)", "O(n)", "BacktrackingAlgorithms.subsets"),
            new Shape("permutations", Family.BACKTRACKING, "O(n*n!)", "O(n)", "BacktrackingAlgorithms.permutations"),
            new Shape("n-queens", Family.BACKTRACKING, "exponential", "O(n)", "BacktrackingAlgorithms.nQueens"),
            new Shape("balanced-parentheses", Family.BACKTRACKING, "Catalan", "O(n)", "BacktrackingAlgorithms.generateParentheses"),
            new Shape("coin-change", Family.DYNAMIC_PROGRAMMING, "O(amount*coins)", "O(amount)", "DynamicProgrammingAlgorithms.coinChange"),
            new Shape("lis-patience", Family.DYNAMIC_PROGRAMMING, "O(n log n)", "O(n)", "DynamicProgrammingAlgorithms.longestIncreasingSubsequence"),
            new Shape("lcs", Family.DYNAMIC_PROGRAMMING, "O(mn)", "O(min(m,n))", "DynamicProgrammingAlgorithms.longestCommonSubsequence"),
            new Shape("lcs-witness", Family.DYNAMIC_PROGRAMMING, "O(mn)", "O(min(m,n))", "IntLcsWitness.javaWitness"),
            new Shape("edit-distance", Family.DYNAMIC_PROGRAMMING, "O(mn)", "O(min(m,n))", "DynamicProgrammingAlgorithms.editDistance"),
            new Shape("word-break", Family.DYNAMIC_PROGRAMMING, "O(n*L)", "O(n)", "DynamicProgrammingAlgorithms.wordBreak"),
            new Shape("unique-grid-paths", Family.DYNAMIC_PROGRAMMING, "O(mn)", "O(n)", "DynamicProgrammingAlgorithms.uniquePaths"),
            new Shape("interval-merge", Family.GREEDY_INTERVAL, "O(n log n)", "O(n)", "IntervalGreedyAlgorithms.merge"),
            new Shape("interval-insert", Family.GREEDY_INTERVAL, "O(n)", "O(n)", "IntervalGreedyAlgorithms.insert"),
            new Shape("jump-greedy", Family.GREEDY_INTERVAL, "O(n)", "O(1)", "IntervalGreedyAlgorithms.minimumJumps"),
            new Shape("bit-count", Family.BITWISE, "O(set bits)", "O(1)", "BitAlgorithms.hammingWeight"),
            new Shape("kmp", Family.STRING, "O(n+m)", "O(m)", "StringAlgorithms.kmpSearch"),
            new Shape("manacher", Family.STRING, "O(n)", "O(n)", "StringAlgorithms.longestPalindrome"),
            new Shape("z-search", Family.STRING, "O(n+m)", "O(m)", "ZAlgorithm.zSearch"),
            new Shape("one-mismatch-window", Family.STRING, "O(n+m)", "O(n+m)", "OneMismatchWindowSearch.allIndices"),
            new Shape("aho-corasick-first-offsets", Family.STRING, "O(n + total pattern bytes)", "O(total pattern bytes)", "MultiPatternSearch.firstOffsets"),
            new Shape("matrix-spiral", Family.MATRIX, "O(mn)", "O(1) aux", "MatrixAlgorithms.spiralOrder"),
            new Shape("island-flood", Family.MATRIX, "O(mn)", "O(mn)", "MatrixAlgorithms.countIslands"),
            new Shape("matrix-zeroes", Family.MATRIX, "O(mn)", "O(1)", "MatrixAlgorithms.setZeroes")
    );
}
