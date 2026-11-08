// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import java.util.Arrays;
import java.util.Objects;

/** Binary-tree algorithms over indexed primitive arrays rather than per-node objects. */
//FLLDCIM: RELOCATE - donor blob 06946b243ab4 at commit 3385d79a2968 (repository, path, URL and digests: m3/algorithms/recipe/algorithm-extras-20261106/PROVENANCE.tsv); recipe m3/algorithms/recipe/algorithm-extras-20261106/relocate.py v1
public final class PrimitiveTreeAlgorithms {
    private PrimitiveTreeAlgorithms() {}

    public record BinaryTree(int[] values, int[] left, int[] right, int root) {
        public BinaryTree {
            Objects.requireNonNull(values, "values");
            Objects.requireNonNull(left, "left");
            Objects.requireNonNull(right, "right");
            if (values.length != left.length || values.length != right.length) {
                throw new IllegalArgumentException("tree array lengths differ");
            }
            values = values.clone();
            left = left.clone();
            right = right.clone();
            validateTree(values.length, left, right, root);
        }

        @Override public int[] values() { return values.clone(); }
        @Override public int[] left() { return left.clone(); }
        @Override public int[] right() { return right.clone(); }

        int[] rawValues() { return values; }
        int[] rawLeft() { return left; }
        int[] rawRight() { return right; }
    }

    public static int[] preorder(BinaryTree tree) {
        Objects.requireNonNull(tree, "tree");
        int n = tree.rawValues().length;
        if (n == 0) return new int[0];
        int[] output = new int[n];
        int[] stack = new int[n];
        int size = 0;
        int cursor = 0;
        stack[size++] = tree.root();
        int[] values = tree.rawValues();
        int[] left = tree.rawLeft();
        int[] right = tree.rawRight();
        while (size > 0) {
            int node = stack[--size];
            output[cursor++] = values[node];
            if (right[node] >= 0) stack[size++] = right[node];
            if (left[node] >= 0) stack[size++] = left[node];
        }
        return output;
    }

    public static int[] postorder(BinaryTree tree) {
        Objects.requireNonNull(tree, "tree");
        int n = tree.rawValues().length;
        if (n == 0) return new int[0];
        int[] nodeOrder = nodeOrder(tree);
        int[] output = new int[n];
        int[] values = tree.rawValues();
        for (int i = 0; i < n; i++) output[i] = values[nodeOrder[n - 1 - i]];
        return output;
    }

    public static int diameterEdges(BinaryTree tree) {
        Objects.requireNonNull(tree, "tree");
        int n = tree.rawValues().length;
        if (n == 0) return 0;
        int[] order = nodeOrder(tree);
        int[] height = new int[n];
        int[] left = tree.rawLeft();
        int[] right = tree.rawRight();
        int diameter = 0;
        for (int i = n - 1; i >= 0; i--) {
            int node = order[i];
            int leftHeight = left[node] < 0 ? 0 : height[left[node]];
            int rightHeight = right[node] < 0 ? 0 : height[right[node]];
            diameter = Math.max(diameter, leftHeight + rightHeight);
            height[node] = Math.max(leftHeight, rightHeight) + 1;
        }
        return diameter;
    }

    public static int lowestCommonAncestorBstByValue(
            BinaryTree tree,
            int firstValue,
            int secondValue) {
        Objects.requireNonNull(tree, "tree");
        if (!containsBstValue(tree, firstValue) || !containsBstValue(tree, secondValue)) {
            return -1;
        }
        int low = Math.min(firstValue, secondValue);
        int high = Math.max(firstValue, secondValue);
        int node = tree.root();
        int[] values = tree.rawValues();
        int[] left = tree.rawLeft();
        int[] right = tree.rawRight();
        while (node >= 0) {
            int value = values[node];
            if (value < low) node = right[node];
            else if (value > high) node = left[node];
            else return node;
        }
        return -1;
    }

    public static long maxPathSum(BinaryTree tree) {
        Objects.requireNonNull(tree, "tree");
        int n = tree.rawValues().length;
        if (n == 0) throw new IllegalArgumentException("tree must not be empty");
        int[] order = nodeOrder(tree);
        long[] branch = new long[n];
        int[] values = tree.rawValues();
        int[] left = tree.rawLeft();
        int[] right = tree.rawRight();
        long best = Long.MIN_VALUE;
        for (int i = n - 1; i >= 0; i--) {
            int node = order[i];
            long leftBranch = left[node] < 0 ? 0L : Math.max(0L, branch[left[node]]);
            long rightBranch = right[node] < 0 ? 0L : Math.max(0L, branch[right[node]]);
            long through = Math.addExact(values[node], Math.addExact(leftBranch, rightBranch));
            best = Math.max(best, through);
            branch[node] = Math.addExact(values[node], Math.max(leftBranch, rightBranch));
        }
        return best;
    }

    public static BinaryTree inverted(BinaryTree tree) {
        Objects.requireNonNull(tree, "tree");
        return new BinaryTree(
                tree.rawValues(),
                tree.rawRight(),
                tree.rawLeft(),
                tree.root());
    }

    private static boolean containsBstValue(BinaryTree tree, int target) {
        int node = tree.root();
        int[] values = tree.rawValues();
        int[] left = tree.rawLeft();
        int[] right = tree.rawRight();
        while (node >= 0) {
            int value = values[node];
            if (value == target) return true;
            node = target < value ? left[node] : right[node];
        }
        return false;
    }

    private static int[] nodeOrder(BinaryTree tree) {
        int n = tree.rawValues().length;
        if (n == 0) return new int[0];
        int[] order = new int[n];
        int[] stack = new int[n];
        int size = 0;
        int count = 0;
        stack[size++] = tree.root();
        int[] left = tree.rawLeft();
        int[] right = tree.rawRight();
        while (size > 0) {
            int node = stack[--size];
            order[count++] = node;
            if (left[node] >= 0) stack[size++] = left[node];
            if (right[node] >= 0) stack[size++] = right[node];
        }
        if (count != n) throw new IllegalStateException("tree validation mismatch");
        return order;
    }

    private static void validateTree(int n, int[] left, int[] right, int root) {
        if (n == 0) {
            if (root != -1) throw new IllegalArgumentException("empty tree root must be -1");
            return;
        }
        if (root < 0 || root >= n) throw new IllegalArgumentException("root out of range");
        int[] indegree = new int[n];
        for (int node = 0; node < n; node++) {
            validateChild(n, left[node]);
            validateChild(n, right[node]);
            if (left[node] >= 0 && ++indegree[left[node]] > 1) {
                throw new IllegalArgumentException("node has multiple parents");
            }
            if (right[node] >= 0 && ++indegree[right[node]] > 1) {
                throw new IllegalArgumentException("node has multiple parents");
            }
        }
        if (indegree[root] != 0) throw new IllegalArgumentException("root has a parent");
        for (int node = 0; node < n; node++) {
            if (node != root && indegree[node] != 1) {
                throw new IllegalArgumentException("tree contains unreachable root/forest node");
            }
        }

        boolean[] visited = new boolean[n];
        int[] stack = new int[n];
        int size = 0;
        int count = 0;
        stack[size++] = root;
        while (size > 0) {
            int node = stack[--size];
            if (visited[node]) throw new IllegalArgumentException("tree contains a cycle");
            visited[node] = true;
            count++;
            if (left[node] >= 0) stack[size++] = left[node];
            if (right[node] >= 0) stack[size++] = right[node];
        }
        if (count != n) throw new IllegalArgumentException("tree contains unreachable nodes");
    }

    private static void validateChild(int n, int child) {
        if (child < -1 || child >= n) throw new IllegalArgumentException("child index out of range");
    }
}
