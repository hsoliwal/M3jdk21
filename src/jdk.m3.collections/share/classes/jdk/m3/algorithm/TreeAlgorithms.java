// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import jdk.m3.algorithm.ProgressMonitor;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Arrays;

//FLLDCIM: RELOCATE - donor blob 0d3dc6dd568b at commit 3385d79a2968 (repository, path, URL and digests: m3/algorithms/recipe/families-20261106/PROVENANCE.tsv); recipe m3/algorithms/recipe/families-20261106/relocate.py v1
public final class TreeAlgorithms {
    private TreeAlgorithms() {
    }

    public static final class Node<T> {
        private final T value;
        private Node<T> left;
        private Node<T> right;

        public Node(T value) {
            this.value = value;
        }

        public T value() {
            return value;
        }

        public Node<T> left() {
            return left;
        }

        public Node<T> right() {
            return right;
        }

        public Node<T> left(Node<T> value) {
            left = value;
            return this;
        }

        public Node<T> right(Node<T> value) {
            right = value;
            return this;
        }
    }

    public static <T> List<T> inorder(Node<T> root, ProgressMonitor monitor) {
        AlgorithmSupport.begin(monitor, "tree-inorder", ProgressMonitor.UNKNOWN);
        try {
            List<T> result = new ArrayList<>();
            ArrayDeque<Node<T>> stack = new ArrayDeque<>();
            Node<T> current = root;
            while (current != null || !stack.isEmpty()) {
                monitor.checkCanceled();
                while (current != null) {
                    stack.push(current);
                    current = current.left;
                }
                current = stack.pop();
                monitor.worked(1);
                result.add(current.value);
                current = current.right;
            }
            return List.copyOf(result);
        } finally {
            monitor.done();
        }
    }

    public static <T> List<List<T>> levelOrder(Node<T> root, ProgressMonitor monitor) {
        AlgorithmSupport.begin(monitor, "tree-level-order", ProgressMonitor.UNKNOWN);
        try {
            if (root == null) {
                return List.of();
            }
            List<List<T>> result = new ArrayList<>();
            ArrayDeque<Node<T>> queue = new ArrayDeque<>();
            queue.add(root);
            while (!queue.isEmpty()) {
                monitor.checkCanceled();
                int size = queue.size();
                List<T> level = new ArrayList<>(size);
                for (int index = 0; index < size; index++) {
                    AlgorithmSupport.tick(monitor);
                    Node<T> node = queue.removeFirst();
                    level.add(node.value);
                    if (node.left != null) {
                        queue.addLast(node.left);
                    }
                    if (node.right != null) {
                        queue.addLast(node.right);
                    }
                }
                result.add(List.copyOf(level));
            }
            return List.copyOf(result);
        } finally {
            monitor.done();
        }
    }

    public static <T> int maxDepth(Node<T> root, ProgressMonitor monitor) {
        AlgorithmSupport.begin(monitor, "tree-max-depth", ProgressMonitor.UNKNOWN);
        try {
            if (root == null) {
                return 0;
            }
            record Frame<T>(Node<T> node, int depth) {}
            ArrayDeque<Frame<T>> stack = new ArrayDeque<>();
            stack.push(new Frame<>(root, 1));
            int best = 0;
            while (!stack.isEmpty()) {
                AlgorithmSupport.tick(monitor);
                Frame<T> frame = stack.pop();
                best = Math.max(best, frame.depth());
                if (frame.node().left != null) {
                    stack.push(new Frame<>(frame.node().left, frame.depth() + 1));
                }
                if (frame.node().right != null) {
                    stack.push(new Frame<>(frame.node().right, frame.depth() + 1));
                }
            }
            return best;
        } finally {
            monitor.done();
        }
    }

    public static <T> boolean isBalanced(Node<T> root, ProgressMonitor monitor) {
        AlgorithmSupport.begin(monitor, "tree-balanced", ProgressMonitor.UNKNOWN);
        try {
            if (root == null) {
                return true;
            }
            record Frame<T>(Node<T> node, boolean exit) {}
            ArrayDeque<Frame<T>> stack = new ArrayDeque<>();
            Map<Node<T>, Integer> heights = new IdentityHashMap<>();
            stack.push(new Frame<>(root, false));
            while (!stack.isEmpty()) {
                AlgorithmSupport.tick(monitor);
                Frame<T> frame = stack.pop();
                Node<T> node = frame.node();
                if (!frame.exit()) {
                    stack.push(new Frame<>(node, true));
                    if (node.right != null) {
                        stack.push(new Frame<>(node.right, false));
                    }
                    if (node.left != null) {
                        stack.push(new Frame<>(node.left, false));
                    }
                } else {
                    int left = node.left == null ? 0 : heights.get(node.left);
                    int right = node.right == null ? 0 : heights.get(node.right);
                    if (Math.abs(left - right) > 1) {
                        return false;
                    }
                    heights.put(node, Math.max(left, right) + 1);
                }
            }
            return true;
        } finally {
            monitor.done();
        }
    }

    public static <T> boolean isValidBst(
            Node<T> root,
            Comparator<? super T> comparator,
            ProgressMonitor monitor) {
        Objects.requireNonNull(comparator, "comparator");
        AlgorithmSupport.begin(monitor, "validate-bst", ProgressMonitor.UNKNOWN);
        try {
            ArrayDeque<Node<T>> stack = new ArrayDeque<>();
            Node<T> current = root;
            T previous = null;
            boolean hasPrevious = false;
            while (current != null || !stack.isEmpty()) {
                monitor.checkCanceled();
                while (current != null) {
                    stack.push(current);
                    current = current.left;
                }
                current = stack.pop();
                monitor.worked(1);
                if (hasPrevious && comparator.compare(previous, current.value) >= 0) {
                    return false;
                }
                previous = current.value;
                hasPrevious = true;
                current = current.right;
            }
            return true;
        } finally {
            monitor.done();
        }
    }


    public static <T> T kthSmallest(
            Node<T> root,
            int k,
            ProgressMonitor monitor) {
        if (k <= 0) {
            throw new IllegalArgumentException("k must be positive");
        }
        AlgorithmSupport.begin(monitor, "bst-kth-smallest", ProgressMonitor.UNKNOWN);
        try {
            ArrayDeque<Node<T>> stack = new ArrayDeque<>();
            Node<T> current = root;
            int seen = 0;
            while (current != null || !stack.isEmpty()) {
                monitor.checkCanceled();
                while (current != null) {
                    stack.push(current);
                    current = current.left;
                }
                current = stack.pop();
                monitor.worked(1);
                if (++seen == k) {
                    return current.value;
                }
                current = current.right;
            }
            throw new IllegalArgumentException("k exceeds tree size");
        } finally {
            monitor.done();
        }
    }

    public static <T> Node<T> lowestCommonAncestor(
            Node<T> root,
            Node<T> first,
            Node<T> second,
            ProgressMonitor monitor) {
        AlgorithmSupport.begin(monitor, "tree-lowest-common-ancestor", ProgressMonitor.UNKNOWN);
        try {
            if (root == null || first == null || second == null) {
                return null;
            }
            Map<Node<T>, Node<T>> parent = new IdentityHashMap<>();
            ArrayDeque<Node<T>> stack = new ArrayDeque<>();
            parent.put(root, null);
            stack.push(root);
            while (!parent.containsKey(first) || !parent.containsKey(second)) {
                if (stack.isEmpty()) {
                    return null;
                }
                AlgorithmSupport.tick(monitor);
                Node<T> node = stack.pop();
                if (node.left != null) {
                    parent.put(node.left, node);
                    stack.push(node.left);
                }
                if (node.right != null) {
                    parent.put(node.right, node);
                    stack.push(node.right);
                }
            }
            java.util.Set<Node<T>> ancestors =
                    java.util.Collections.newSetFromMap(new IdentityHashMap<>());
            for (Node<T> node = first; node != null; node = parent.get(node)) {
                monitor.checkCanceled();
                ancestors.add(node);
            }
            for (Node<T> node = second; node != null; node = parent.get(node)) {
                monitor.checkCanceled();
                if (ancestors.contains(node)) {
                    return node;
                }
            }
            return null;
        } finally {
            monitor.done();
        }
    }

    //FLLDCIM: FOLD cec7b47283c6 PrimitiveTreeAlgorithms.BinaryTree
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

    //FLLDCIM: FOLD cec7b47283c6 PrimitiveTreeAlgorithms.preorder
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

    //FLLDCIM: FOLD cec7b47283c6 PrimitiveTreeAlgorithms.postorder
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

    //FLLDCIM: FOLD cec7b47283c6 PrimitiveTreeAlgorithms.diameterEdges
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

    //FLLDCIM: FOLD cec7b47283c6 PrimitiveTreeAlgorithms.lowestCommonAncestorBstByValue
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

    //FLLDCIM: FOLD cec7b47283c6 PrimitiveTreeAlgorithms.maxPathSum
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

    //FLLDCIM: FOLD cec7b47283c6 PrimitiveTreeAlgorithms.inverted
    public static BinaryTree inverted(BinaryTree tree) {
        Objects.requireNonNull(tree, "tree");
        return new BinaryTree(
                tree.rawValues(),
                tree.rawRight(),
                tree.rawLeft(),
                tree.root());
    }

    //FLLDCIM: FOLD cec7b47283c6 PrimitiveTreeAlgorithms.containsBstValue
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

    //FLLDCIM: FOLD cec7b47283c6 PrimitiveTreeAlgorithms.nodeOrder
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

    //FLLDCIM: FOLD cec7b47283c6 PrimitiveTreeAlgorithms.validateTree
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

    //FLLDCIM: FOLD cec7b47283c6 PrimitiveTreeAlgorithms.validateChild
    private static void validateChild(int n, int child) {
        if (child < -1 || child >= n) throw new IllegalArgumentException("child index out of range");
    }
}
