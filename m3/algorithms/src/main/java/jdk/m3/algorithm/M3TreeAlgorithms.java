// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import jdk.m3.algorithm.M3ProgressMonitor;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

//FLLDCIM: RELOCATE - donor blob 0d3dc6dd568b at commit 3385d79a2968 (repository, path, URL and digests: m3/algorithms/recipe/families-20261106/PROVENANCE.tsv); recipe m3/algorithms/recipe/families-20261106/relocate.py v1
public final class M3TreeAlgorithms {
    private M3TreeAlgorithms() {
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

    public static <T> List<T> inorder(Node<T> root, M3ProgressMonitor monitor) {
        M3AlgorithmSupport.begin(monitor, "tree-inorder", M3ProgressMonitor.UNKNOWN);
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

    public static <T> List<List<T>> levelOrder(Node<T> root, M3ProgressMonitor monitor) {
        M3AlgorithmSupport.begin(monitor, "tree-level-order", M3ProgressMonitor.UNKNOWN);
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
                    M3AlgorithmSupport.tick(monitor);
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

    public static <T> int maxDepth(Node<T> root, M3ProgressMonitor monitor) {
        M3AlgorithmSupport.begin(monitor, "tree-max-depth", M3ProgressMonitor.UNKNOWN);
        try {
            if (root == null) {
                return 0;
            }
            record Frame<T>(Node<T> node, int depth) {}
            ArrayDeque<Frame<T>> stack = new ArrayDeque<>();
            stack.push(new Frame<>(root, 1));
            int best = 0;
            while (!stack.isEmpty()) {
                M3AlgorithmSupport.tick(monitor);
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

    public static <T> boolean isBalanced(Node<T> root, M3ProgressMonitor monitor) {
        M3AlgorithmSupport.begin(monitor, "tree-balanced", M3ProgressMonitor.UNKNOWN);
        try {
            if (root == null) {
                return true;
            }
            record Frame<T>(Node<T> node, boolean exit) {}
            ArrayDeque<Frame<T>> stack = new ArrayDeque<>();
            Map<Node<T>, Integer> heights = new IdentityHashMap<>();
            stack.push(new Frame<>(root, false));
            while (!stack.isEmpty()) {
                M3AlgorithmSupport.tick(monitor);
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
            M3ProgressMonitor monitor) {
        Objects.requireNonNull(comparator, "comparator");
        M3AlgorithmSupport.begin(monitor, "validate-bst", M3ProgressMonitor.UNKNOWN);
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
            M3ProgressMonitor monitor) {
        if (k <= 0) {
            throw new IllegalArgumentException("k must be positive");
        }
        M3AlgorithmSupport.begin(monitor, "bst-kth-smallest", M3ProgressMonitor.UNKNOWN);
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
            M3ProgressMonitor monitor) {
        M3AlgorithmSupport.begin(monitor, "tree-lowest-common-ancestor", M3ProgressMonitor.UNKNOWN);
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
                M3AlgorithmSupport.tick(monitor);
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
}
