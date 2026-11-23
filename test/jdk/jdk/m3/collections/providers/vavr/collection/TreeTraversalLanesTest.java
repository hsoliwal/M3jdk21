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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UncheckedIOException;
import java.util.ArrayDeque;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import jdk.m3.collections.vavr.collection.Tree.Node;
import jdk.m3.collections.vavr.collection.Tree.Order;
import jdk.m3.collections.vavr.collection.TreeTraversalOracle.RecursiveTraversal;
import jdk.m3.collections.vavr.control.Option;
import org.junit.jupiter.api.Test;

/**
 * TreeModule traversal lanes (exact-size ArrayList, (owner, cursor) frame stack, BFS read offset)
 * against the pre-patch recursive fold driven in lockstep: same nodes, stream class, toString at
 * every evaluation state, exceptions, serialized bytes and tree views; deep trees on small stacks.
 */
//FLLDCIM: LEETCODIFY — differential proof for vavr TreeModule traversal lanes
class TreeTraversalLanesTest {

    private static final long SEED = 0x7EE5_1A4EL;
    private static final long BIG_STACK = 1L << 30;
    private static final long SMALL_STACK = 256L << 10;
    private static final int RANDOM_TREES = 2_000;
    private static final int MAX_RANDOM_SIZE = 400;
    private static final int DEEP = 3_000;
    private static final int HUGE = 200_000;
    private static final int OVERFLOW_DEPTH = 20_000;
    private static final int HELD = 3;
    private static final int CHEAP_SPLIT = 3;
    private static final String MODULE = "jdk.m3.collections.vavr.collection.StreamModule$";
    private static final String APPEND_ELEMENTS = MODULE + "AppendElements";
    private static final String CONS_IMPL = MODULE + "ConsImpl";
    private static final Node<Object> ABSENT = TreeTraversalOracle.leaf("absent");

    private record Pair(Stream<Node<Object>> ref, Stream<Node<Object>> act) {
    }

    @Test
    void javadocTreeOrders() {
        final Node<Object> tree = TreeTraversalOracle.javadocTree();
        assertEquals("[1, 2, 4, 7, 5, 3, 6, 8, 9]", valuesOf(tree, Order.PRE_ORDER));
        assertEquals("[7, 4, 2, 5, 1, 8, 6, 9, 3]", valuesOf(tree, Order.IN_ORDER));
        assertEquals("[7, 4, 5, 2, 8, 9, 6, 3, 1]", valuesOf(tree, Order.POST_ORDER));
        assertEquals("[1, 2, 3, 4, 5, 6, 7, 8, 9]", valuesOf(tree, Order.LEVEL_ORDER));
        final Stream<Node<Object>> post = traverse(tree, Order.POST_ORDER);
        assertEquals(APPEND_ELEMENTS, post.getClass().getName());
        assertEquals(9, post.toJavaList().size());
        assertEquals("Stream(Tree(7), Tree(4, 7), ?)", post.toString());
    }

    @Test
    void randomTreesMatchReference() {
        final Random rnd = new Random(SEED);
        onStack(BIG_STACK, () -> {
            for (int t = 0; t < RANDOM_TREES; t++) {
                final int size = 1 + rnd.nextInt(MAX_RANDOM_SIZE);
                final int ops = 10 + rnd.nextInt(21);
                matchAll(TreeTraversalOracle.randomTree(rnd, size), rnd, ops, true);
            }
        });
    }

    @Test
    void fixedShapesMatchReference() {
        final Random rnd = new Random(SEED + 1);
        onStack(BIG_STACK, () -> java.util.List.of(TreeTraversalOracle.leaf(0),
                TreeTraversalOracle.javadocTree(), TreeTraversalOracle.leftSpine(40),
                TreeTraversalOracle.rightComb(40), TreeTraversalOracle.star(300),
                TreeTraversalOracle.complete(2, 7), TreeTraversalOracle.complete(3, 5),
                TreeTraversalOracle.dag(), TreeTraversalOracle.path(60))
                .forEach(tree -> matchAll(tree, rnd, 30, true)));
    }

    @Test
    void deepShapesMatchReference() {
        final Random rnd = new Random(SEED + 2);
        onStack(BIG_STACK, () -> java.util.List.of(TreeTraversalOracle.path(DEEP),
                TreeTraversalOracle.leftSpine(DEEP), TreeTraversalOracle.rightComb(DEEP))
                .forEach(tree -> matchAll(tree, rnd, 8, false)));
    }

    @Test
    void deepTreesFinishOnSmallStack() {
        for (Node<Object> tree : java.util.List.of(TreeTraversalOracle.path(HUGE),
                TreeTraversalOracle.leftSpine(HUGE), TreeTraversalOracle.rightComb(HUGE))) {
            for (Order order : Order.values()) {
                onStack(SMALL_STACK, () -> sameAsModel(tree, order));
            }
        }
    }

    @Test
    void referenceOverflowsOnSmallStack() {
        final Node<Object> tree = TreeTraversalOracle.path(OVERFLOW_DEPTH);
        for (Order order : java.util.List.of(Order.PRE_ORDER, Order.IN_ORDER, Order.POST_ORDER)) {
            final AtomicReference<Throwable> seen = new AtomicReference<>();
            onStack(SMALL_STACK, () -> seen.set(overflow(tree, order)));
            assertInstanceOf(StackOverflowError.class, seen.get(), order + " reference");
        }
    }

    private static Throwable overflow(Node<Object> tree, Order order) {
        try {
            return new AssertionError(RecursiveTraversal.traverse(tree, order).toJavaList().size());
        } catch (StackOverflowError expected) {
            return expected;
        }
    }

    /** Patched lanes against the independent model; POST keeps the AppendElements shape. */
    private static void sameAsModel(Node<Object> tree, Order order) {
        final java.util.List<Node<Object>> expected = TreeTraversalOracle.model(tree, order);
        final Stream<Node<Object>> act = traverse(tree, order);
        final boolean post = order == Order.POST_ORDER;
        final String kind = post ? APPEND_ELEMENTS : CONS_IMPL;
        assertEquals(kind, act.getClass().getName(), order + " class");
        sameElements(expected, act.toJavaList(), order + " nodes");
        final int split = post ? TreeTraversalOracle.postSplit(tree) : CHEAP_SPLIT;
        if (split < CHEAP_SPLIT) {
            final StringBuilder base = new StringBuilder("Stream(");
            expected.subList(0, split + 1).forEach(n -> base.append(n).append(", "));
            assertEquals(base.append("?)").toString(), act.toString(), "POST after iteration");
        }
    }

    private static String valuesOf(Node<Object> tree, Order order) {
        return tree.values(order).toJavaList().toString();
    }

    private static void matchAll(Node<Object> tree, Random rnd, int ops, boolean full) {
        sameTreeViews(tree);
        for (Order order : Order.values()) {
            lockstep(tree, order, rnd, ops, full);
        }
    }

    private static void lockstep(Node<Object> tree, Order order, Random rnd, int ops,
            boolean full) {
        final Pair fresh = pair(tree, order);
        assertEquals(fresh.ref().getClass(), fresh.act().getClass(), order + " class");
        assertEquals(fresh.ref().toString(), fresh.act().toString(), order + " fresh");
        new Script(pair(tree, order), tree.length(), rnd, full).run(ops);
        sameNodes(pair(tree, order), tree.length());
        sameFailures(pair(tree, order), tree.length());
        sameBytes(pair(tree, order), full);
        final java.util.List<Object> values = fresh.ref().map(Node::getValue).toJavaList();
        sameElements(values, tree.values(order).toJavaList(), order + " values");
        sameElements(values, tree.iterator(order).toJavaList(), order + " iterator");
    }

    /** Tree-level views must match a model built from the reference pre-order values. */
    private static void sameTreeViews(Node<Object> tree) {
        final java.util.List<Object> pre = RecursiveTraversal.traverse(tree, Order.PRE_ORDER)
                .map(Node::getValue).toJavaList();
        final StringBuilder model = new StringBuilder();
        pre.forEach(value -> model.append(", ").append(value));
        assertEquals("Tree(" + model.substring(2) + ")", tree.toString());
        assertSame(pre.get(0), tree.head());
        assertSame(pre.get(pre.size() - 1), tree.last());
        assertEquals(pre.size(), tree.length());
        sameElements(pre, tree.iterator().toJavaList(), "iterator()");
        sameElements(pre, tree.values().toJavaList(), "values()");
    }

    private static void sameNodes(Pair pair, int size) {
        final Iterator<Node<Object>> ref = pair.ref().iterator();
        final Iterator<Node<Object>> act = pair.act().iterator();
        int index = 0;
        while (ref.hasNext()) {
            assertTrue(act.hasNext(), "act ended early");
            assertSame(ref.next(), act.next(), "node by node");
            index++;
        }
        assertFalse(act.hasNext(), "act longer than ref");
        assertEquals(size, index, "length");
    }

    private static void sameFailures(Pair pair, int size) {
        sameFailure(() -> pair.ref().drop(size).head(), () -> pair.act().drop(size).head());
        sameFailure(() -> pair.ref().get(size), () -> pair.act().get(size));
        sameFailure(() -> pair.ref().get(-1), () -> pair.act().get(-1));
        sameFailure(() -> pair.ref().drop(size).tail(), () -> pair.act().drop(size).tail());
    }

    private static void sameFailure(Runnable ref, Runnable act) {
        final RuntimeException expected = failure(ref);
        final RuntimeException actual = failure(act);
        assertEquals(expected.getClass(), actual.getClass(), "exception class");
        assertEquals(expected.getMessage(), actual.getMessage(), "exception message");
    }

    private static RuntimeException failure(Runnable task) {
        try {
            task.run();
        } catch (RuntimeException e) {
            return e;
        }
        throw new AssertionError("no exception");
    }

    private static void sameBytes(Pair pair, boolean full) {
        final byte[] ref = serialize(pair.ref());
        final byte[] act = serialize(pair.act());
        assertArrayEquals(ref, act, "serialized bytes");
        sameViews(pair, full);
        final Object refBack = deserialize(ref);
        final Object actBack = deserialize(act);
        assertEquals(refBack.getClass(), actBack.getClass(), "deserialized class");
        assertEquals(refBack.toString(), actBack.toString(), "deserialized toString");
        assertEquals(refBack, actBack, "deserialized equals");
    }

    private static byte[] serialize(Object value) {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(value);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bytes.toByteArray();
    }

    private static Object deserialize(byte[] bytes) {
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            return in.readObject();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (ClassNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    private static void sameElements(java.util.List<?> expected, java.util.List<?> actual,
            String what) {
        assertEquals(expected.size(), actual.size(), what + " size");
        for (int i = 0; i < expected.size(); i++) {
            assertSame(expected.get(i), actual.get(i), what);
        }
    }

    /** toString when asked (Node.toString walks each subtree), and the identity shape always. */
    private static void sameViews(Pair pair, boolean text) {
        if (text) {
            assertEquals(pair.ref().toString(), pair.act().toString(), "toString");
        }
        assertEquals(shape(pair.ref()), shape(pair.act()), "shape");
    }

    /** Cons.toString with identityHashCode for each element: the evaluated prefix, cheaply. */
    private static String shape(Stream<Node<Object>> stream) {
        final StringBuilder out = new StringBuilder("Stream(");
        Stream<Node<Object>> at = stream;
        while (!at.isEmpty()) {
            final Stream.Cons<Node<Object>> cons = (Stream.Cons<Node<Object>>) at;
            out.append(System.identityHashCode(cons.head));
            if (!cons.tail.isEvaluated()) {
                return out.append(", ?)").toString();
            }
            at = at.tail();
            out.append(at.isEmpty() ? "" : ", ");
        }
        return out.append(')').toString();
    }

    private static Stream<Node<Object>> traverse(Node<Object> tree, Order order) {
        return (Stream<Node<Object>>) tree.traverse(order);
    }

    private static Pair pair(Node<Object> tree, Order order) {
        return new Pair(RecursiveTraversal.traverse(tree, order), traverse(tree, order));
    }

    private static void onStack(long bytes, Runnable task) {
        final AtomicReference<Throwable> failure = new AtomicReference<>();
        final Thread thread = new Thread(null, () -> capture(task, failure), "lanes", bytes);
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
        rethrow(failure.get());
    }

    private static void capture(Runnable task, AtomicReference<Throwable> failure) {
        try {
            task.run();
        } catch (Throwable e) {
            failure.set(e);
        }
    }

    private static void rethrow(Throwable thrown) {
        if (thrown instanceof Error error) {
            throw error;
        }
        if (thrown instanceof RuntimeException runtime) {
            throw runtime;
        }
    }

    /** Seeded operations applied to both streams; every result and view is compared after each. */
    private static final class Script {

        private final Pair root;
        private final int size;
        private final Random rnd;
        private final boolean full;
        private final ArrayDeque<Pair> held = new ArrayDeque<>();
        private Pair last;
        private int lastAt;

        Script(Pair root, int size, Random rnd, boolean full) {
            this.root = root;
            this.size = size;
            this.rnd = rnd;
            this.full = full;
            this.last = root;
        }

        void run(int ops) {
            for (int op = 0; op < ops; op++) {
                step(rnd.nextInt(9));
                sameViews(root, full);
                held.forEach(pair -> sameViews(pair, false));
            }
        }

        private void step(int kind) {
            switch (kind) {
                case 0 -> get();
                case 1 -> holdTail();
                case 2 -> iterate();
                case 3 -> take();
                case 4 -> dropHead();
                case 5 -> assertEquals(root.ref().length(), root.act().length(), "length");
                case 6 -> sameElements(root.ref().toList().toJavaList(),
                        root.act().toList().toJavaList(), "toList");
                case 7 -> appended();
                default -> equality();
            }
        }

        private void get() {
            final int index = rnd.nextInt(size);
            assertSame(root.ref().get(index), root.act().get(index), "get");
        }

        private void holdTail() {
            final int rest = size - lastAt;
            if (rest > 0) {
                final int k = 1 + rnd.nextInt(rnd.nextBoolean() ? Math.min(HELD, rest) : rest);
                Stream<Node<Object>> ref = last.ref();
                Stream<Node<Object>> act = last.act();
                for (int i = 0; i < k; i++) {
                    ref = ref.tail();
                    act = act.tail();
                }
                last = new Pair(ref, act);
                lastAt += k;
                held.addLast(last);
                trim();
            }
        }

        private void trim() {
            if (held.size() > HELD) {
                held.removeFirst();
            }
        }

        private void iterate() {
            final Iterator<Node<Object>> ref = root.ref().iterator();
            final Iterator<Node<Object>> act = root.act().iterator();
            final int steps = rnd.nextInt(size + 1);
            for (int i = 0; i < steps; i++) {
                assertEquals(ref.hasNext(), act.hasNext(), "hasNext");
                assertSame(ref.next(), act.next(), "next");
            }
            assertEquals(ref.hasNext(), act.hasNext(), "hasNext after");
        }

        private void take() {
            final int k = rnd.nextInt(size + 2);
            sameElements(root.ref().take(k).toJavaList(), root.act().take(k).toJavaList(),
                    "take");
        }

        private void dropHead() {
            final int k = rnd.nextInt(size + 2);
            final Option<Node<Object>> ref = root.ref().drop(k).headOption();
            final Option<Node<Object>> act = root.act().drop(k).headOption();
            assertEquals(ref.isDefined(), act.isDefined(), "headOption defined");
            assertSame(ref.getOrElse(ABSENT), act.getOrElse(ABSENT), "headOption");
        }

        private void appended() {
            final Node<Object> extra = TreeTraversalOracle.leaf("extra");
            final Stream<Node<Object>> ref = root.ref().append(extra);
            final Stream<Node<Object>> act = root.act().append(extra);
            assertEquals(ref.getClass(), act.getClass(), "append class");
            sameViews(new Pair(ref, act), full);
            final Stream<Node<Object>> refAll = root.ref().appendAll(Stream.of(extra));
            final Stream<Node<Object>> actAll = root.act().appendAll(Stream.of(extra));
            assertEquals(refAll.getClass(), actAll.getClass(), "appendAll class");
            sameViews(new Pair(refAll, actAll), full);
        }

        private void equality() {
            assertTrue(root.ref().equals(root.act()), "ref equals act");
            assertTrue(root.act().equals(root.ref()), "act equals ref");
            assertEquals(root.ref().hashCode(), root.act().hashCode(), "hashCode");
        }
    }
}
