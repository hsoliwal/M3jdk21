/*******************************************************************************
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
package com.m3.collections;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Spliterator;
import java.util.stream.StreamSupport;

/** Executable Java 21 oracle: primitive, live, parent-owned nested paths. */
public final class M3LongCompositePathContract {
    private static long checks;
    private M3LongCompositePathContract() { }

    private static void require(boolean ok, String why) {
        checks++;
        if (!ok) throw new AssertionError(why);
    }
    private static void equal(long[] value, long... expected) {
        checks++;
        if (!Arrays.equals(value, expected)) {
            throw new AssertionError(Arrays.toString(value) + " != " + Arrays.toString(expected));
        }
    }
    private static void deep() {
        M3LongCollection nested = M3LongCompositeViews.concat();
        final int depth = 16_384;
        for (int i = 0; i < depth; i++) {
            nested = M3LongCompositeViews.concat(
                    nested, M3LongArrayList.copyOf(new long[] {i}));
        }
        require(nested.size() == depth && !nested.isEmpty(), "deep composed size");
        require(nested.contains(0) && nested.contains(depth / 2)
                && nested.contains(depth - 1) && !nested.contains(-1), "deep membership");
        long[] output = nested.toArray();
        require(output.length == depth, "deep export size");
        for (int i = 0; i < depth; i++) require(output[i] == i, "deep occurrence order");
        M3LongIterator cursor = nested.iterator();
        for (int i = 0; i < depth; i++) {
            require(cursor.hasNext() && cursor.hasNext(), "idempotent deep hasNext");
            require(cursor.nextLong() == i, "deep next");
        }
        require(!cursor.hasNext(), "deep exhaustion");
        try { cursor.nextLong(); throw new AssertionError("exhausted next permitted"); }
        catch (NoSuchElementException expected) { checks++; }

        M3LongCollection empty = M3LongCompositeViews.concat();
        for (int i = 0; i < depth; i++) empty = M3LongCompositeViews.concat(empty);
        require(empty.size() == 0 && empty.isEmpty(), "nested empty subpaths");
        require(!empty.iterator().hasNext() && empty.toArray().length == 0, "empty cursor/export");
    }
    private static void liveLateBinding() {
        M3LongArrayList a = M3LongArrayList.copyOf(new long[] {1, 3});
        M3LongArrayList b = M3LongArrayList.copyOf(new long[] {4});
        M3LongArrayList c = M3LongArrayList.copyOf(new long[] {4, 8});
        M3LongCollection path = M3LongCompositeViews.concat(
                M3LongCompositeViews.concat(a, M3LongCompositeViews.concat(b)),
                M3LongCompositeViews.concat(M3LongCompositeViews.concat(), c));
        Spliterator.OfLong unbound = path.longSpliterator();
        require(unbound.trySplit() == null, "no copied split buffer");
        require((unbound.characteristics() & Spliterator.ORDERED) != 0, "ordered");
        a.add(0);
        c.add(9);
        equal(StreamSupport.longStream(unbound, false).toArray(), 1, 3, 0, 4, 4, 8, 9);
        require(path.contains(4) && !path.contains(99) && path.size() == 7, "live views");
        M3LongIterator late = path.iterator();
        b.add(5); // leaf iterator cannot have been opened by path.iterator()
        equal(path.toArray(), 1, 3, 0, 4, 5, 4, 8, 9);
        long[] received = new long[8];
        for (int i = 0; i < received.length; i++) {
            require(late.hasNext(), "late hasNext");
            received[i] = late.nextLong();
        }
        equal(received, 1, 3, 0, 4, 5, 4, 8, 9);
        try { path.clear(); throw new AssertionError("read-only clear admitted"); }
        catch (UnsupportedOperationException expected) { checks++; }
    }
    private static void random() {
        Random r = new Random(0x4d33504154483431L);
        for (int trial = 0; trial < 360; trial++) {
            M3LongCollection path = M3LongCompositeViews.concat();
            long[] expected = new long[0];
            int n = r.nextInt(40);
            for (int i = 0; i < n; i++) {
                long[] leaf = new long[r.nextInt(4)];
                for (int j = 0; j < leaf.length; j++) leaf[j] = r.nextInt(11) - 5;
                M3LongCollection piece = M3LongArrayList.copyOf(leaf);
                path = (i & 1) == 0 ? M3LongCompositeViews.concat(path, piece)
                        : M3LongCompositeViews.concat(M3LongCompositeViews.concat(path),
                                M3LongCompositeViews.concat(piece));
                long[] merged = Arrays.copyOf(expected, expected.length + leaf.length);
                System.arraycopy(leaf, 0, merged, expected.length, leaf.length);
                expected = merged;
            }
            equal(path.toArray(), expected);
            require(path.size() == expected.length, "random size");
            require(path.isEmpty() == (expected.length == 0), "random emptiness");
        }
    }
    public static void main(String[] args) {
        require(args.length == 0, "unexpected arguments");
        deep();
        liveLateBinding();
        random();
        System.out.println("PASS M3LongCompositePathContract checks=" + checks);
    }
}
