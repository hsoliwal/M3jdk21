/* *****************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.m3.collections.vavr.collection;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Random;
import jdk.m3.collections.vavr.collection.Tree.Node;
import jdk.m3.collections.vavr.collection.Tree.Order;

/**
 * Oracles for TreeTraversalLanesTest: the pre-patch recursive TreeModule traversal bodies, an
 * independent iterative model written from the Order javadoc, and recursion-free tree builders.
 */
//FLLDCIM: LEETCODIFY — reference, model and shapes for the vavr Tree traversal lanes
final class TreeTraversalOracle {

    private static final Object[] VALUES = {null, 0, 1, 1, 2, 3};

    private TreeTraversalOracle() {
    }

    /** One step of the model: expand a node, or emit it. */
    private record Task(Node<Object> node, boolean emit) {
    }

    /**
     * Pre-patch TreeModule traversal bodies, copied with line breaks only, and the switch of
     * Tree.traverse(Order) for a non-empty tree.
     */
    static final class RecursiveTraversal {

        private RecursiveTraversal() {
        }

        static <T extends Object> Stream<Node<T>> traverse(Node<T> node, Order order) {
            switch (order) {
                case PRE_ORDER:
                    return traversePreOrder(node);
                case IN_ORDER:
                    return traverseInOrder(node);
                case POST_ORDER:
                    return traversePostOrder(node);
                case LEVEL_ORDER:
                    return traverseLevelOrder(node);
                default:
                    throw new IllegalStateException("Unknown order: " + order.name());
            }
        }

        static <T extends Object> Stream<Node<T>> traversePreOrder(Node<T> node) {
            return node.getChildren().foldLeft(Stream.of(node),
                    (acc, child) -> acc.appendAll(traversePreOrder(child)));
        }

        static <T extends Object> Stream<Node<T>> traverseInOrder(Node<T> node) {
            if (node.isLeaf()) {
                return Stream.of(node);
            } else {
                final jdk.m3.collections.vavr.collection.List<Node<T>> children =
                        node.getChildren();
                return children
                        .tail()
                        .foldLeft(Stream.<Node<T>> empty(),
                                (acc, child) -> acc.appendAll(traverseInOrder(child)))
                        .prepend(node)
                        .prependAll(traverseInOrder(children.head()));
            }
        }

        static <T extends Object> Stream<Node<T>> traversePostOrder(Node<T> node) {
            return node
                    .getChildren()
                    .foldLeft(Stream.<Node<T>> empty(),
                            (acc, child) -> acc.appendAll(traversePostOrder(child)))
                    .append(node);
        }

        static <T extends Object> Stream<Node<T>> traverseLevelOrder(Node<T> node) {
            Stream<Node<T>> result = Stream.empty();
            final java.util.Queue<Node<T>> queue = new java.util.LinkedList<>();
            queue.add(node);
            while (!queue.isEmpty()) {
                final Node<T> next = queue.remove();
                result = result.prepend(next);
                queue.addAll(next.getChildren().toJavaList());
            }
            return result.reverse();
        }
    }

    /** Order javadoc as a task stack (depth-first) or a FIFO (level order); no recursion. */
    static java.util.List<Node<Object>> model(Node<Object> root, Order order) {
        final java.util.List<Node<Object>> out = new ArrayList<>();
        final ArrayDeque<Task> todo = new ArrayDeque<>();
        todo.push(new Task(root, false));
        while (!todo.isEmpty()) {
            final Task task = todo.pop();
            if (task.emit()) {
                out.add(task.node());
            } else if (order == Order.LEVEL_ORDER) {
                out.add(task.node());
                task.node().getChildren().forEach(kid -> todo.addLast(new Task(kid, false)));
            } else {
                expand(task.node(), order, todo);
            }
        }
        return out;
    }

    /** PRE: n, c1..ck. IN: c1, n, c2..ck. POST: c1..ck, n. A leaf emits itself. */
    private static void expand(Node<Object> node, Order order, ArrayDeque<Task> todo) {
        final java.util.List<Node<Object>> kids = node.getChildren().toJavaList();
        if (kids.isEmpty() || order == Order.POST_ORDER) {
            todo.push(new Task(node, true));
        }
        final int stop = order == Order.IN_ORDER ? 1 : 0;
        for (int i = kids.size() - 1; i >= stop; i--) {
            todo.push(new Task(kids.get(i), false));
        }
        if (!kids.isEmpty() && order != Order.POST_ORDER) {
            todo.push(new Task(node, true));
        }
        if (!kids.isEmpty() && order == Order.IN_ORDER) {
            todo.push(new Task(kids.get(0), false));
        }
    }

    /** Post-order offset of the old AppendElements queue: leftmost leaf's parent size - 1. */
    static int postSplit(Node<Object> root) {
        Node<Object> parent = root;
        while (!parent.getChildren().head().getChildren().isEmpty()) {
            parent = parent.getChildren().head();
        }
        return parent.length() - 1;
    }

    static Node<Object> leaf(Object value) {
        return new Node<>(value, List.empty());
    }

    @SuppressWarnings("varargs")
    @SafeVarargs
    static Node<Object> node(Object value, Node<Object>... kids) {
        return new Node<>(value, List.of(kids));
    }

    /** 1(2(4(7), 5), 3(6(8, 9))) from the Order javadoc. */
    static Node<Object> javadocTree() {
        return node(1, node(2, node(4, leaf(7)), leaf(5)), node(3, node(6, leaf(8), leaf(9))));
    }

    static Node<Object> path(int size) {
        Node<Object> tip = leaf(size % 10);
        for (int i = size - 1; i > 0; i--) {
            tip = node(i % 10, tip);
        }
        return tip;
    }

    /** Branches whose first child is the next branch and whose second child is a leaf. */
    static Node<Object> leftSpine(int depth) {
        Node<Object> tip = leaf(0);
        for (int i = depth; i > 0; i--) {
            tip = node(i % 10, tip, leaf(-(i % 10)));
        }
        return tip;
    }

    /** Branches whose first child is a leaf and whose second child is the next branch. */
    static Node<Object> rightComb(int depth) {
        Node<Object> tip = leaf(0);
        for (int i = depth; i > 0; i--) {
            tip = node(i % 10, leaf(-(i % 10)), tip);
        }
        return tip;
    }

    static Node<Object> star(int leaves) {
        final java.util.List<Node<Object>> kids = new ArrayList<>();
        for (int i = 0; i < leaves; i++) {
            kids.add(leaf(i));
        }
        return new Node<>("star", List.ofAll(kids));
    }

    static Node<Object> complete(int arity, int levels) {
        java.util.List<Node<Object>> level = new ArrayList<>();
        for (int i = 0; i < (int) Math.pow(arity, levels - 1); i++) {
            level.add(leaf(i));
        }
        while (level.size() > 1) {
            final java.util.List<Node<Object>> up = new ArrayList<>();
            for (int i = 0; i < level.size(); i += arity) {
                up.add(new Node<>(i, List.ofAll(level.subList(i, i + arity))));
            }
            level = up;
        }
        return level.get(0);
    }

    /** One subtree under several parents and depths, twice in one list, and a shared list. */
    static Node<Object> dag() {
        final Node<Object> shared = node("shared", leaf("a"), leaf(null));
        final Node<Object> mid = node("mid", shared, leaf(new String("a")));
        final Node<Object> alias = new Node<>("alias", mid.getChildren());
        return node("root", shared, mid, node("deeper", mid, alias), node("twins", shared, shared));
    }

    /** Breadth-first random rose tree, 0-4 children per node (geometric), built bottom-up. */
    static Node<Object> randomTree(Random rnd, int size) {
        final int[] first = new int[size];
        final int[] count = new int[size];
        int next = 1;
        for (int i = 0; next < size; i++) {
            int kids = 0;
            while (kids < 4 && rnd.nextInt(3) != 0) {
                kids++;
            }
            first[i] = next;
            count[i] = Math.min(kids == 0 && i == next - 1 ? 1 : kids, size - next);
            next += count[i];
        }
        return assemble(rnd, first, count);
    }

    private static Node<Object> assemble(Random rnd, int[] first, int[] count) {
        final int size = first.length;
        final java.util.List<Node<Object>> built = new ArrayList<>(size);
        for (int i = size - 1; i >= 0; i--) {
            List<Node<Object>> kids = List.empty();
            for (int c = first[i] + count[i] - 1; c >= first[i]; c--) {
                kids = kids.prepend(built.get(size - 1 - c));
            }
            built.add(new Node<>(randomValue(rnd), kids));
        }
        return built.get(size - 1);
    }

    /** null, shared Integers and equal-but-distinct Strings. */
    private static Object randomValue(Random rnd) {
        final int pick = rnd.nextInt(VALUES.length + 3);
        return pick < VALUES.length ? VALUES[pick] : new String("s" + (pick - VALUES.length));
    }
}
