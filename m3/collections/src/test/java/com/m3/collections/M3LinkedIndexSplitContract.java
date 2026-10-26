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

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Spliterator;
import java.util.stream.StreamSupport;

/** Real-owner Java21 linked-index split differential against JDK LinkedList. */
public final class M3LinkedIndexSplitContract {
    private static int checks;
    private M3LinkedIndexSplitContract() { }

    private static void eq(Object expected, Object actual, String reason) {
        checks++;
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(reason + ": expected=" + expected + " actual=" + actual);
        }
    }
    private static void ok(boolean test, String reason) {
        checks++;
        if (!test) throw new AssertionError(reason);
    }

    private static <E> List<E> collect(Spliterator<E> root, int n) {
        List<Spliterator<E>> shards = new ArrayList<>();
        shards.add(root);
        for (int i = 0; i < shards.size();) {
            Spliterator<E> first = shards.get(i).trySplit();
            if (first == null) i++;
            else shards.add(i, first);
            ok(shards.size() <= 128, "bounded partitions");
        }
        ArrayList<E> result = new ArrayList<>();
        long total = 0;
        for (Spliterator<E> shard : shards) {
            ok(shard.hasCharacteristics(Spliterator.ORDERED
                    | Spliterator.SIZED | Spliterator.SUBSIZED), "list signals");
            ok(!shard.hasCharacteristics(Spliterator.NONNULL), "nullable owner");
            long length = shard.getExactSizeIfKnown();
            ok(length >= 0, "exact partition length");
            total += length;
            int start = result.size();
            shard.forEachRemaining(result::add);
            eq(length, (long) result.size() - start, "partition size");
            eq(0L, shard.estimateSize(), "partition consumed");
        }
        eq((long) n, total, "no lost/duplicated slots");
        return result;
    }

    private static void corpus() {
        for (int n : new int[] {0, 1, 2, 3, 17, 63, 127, 128, 1024, 4097, 8193}) {
            M3LinkedList<Integer> m3 = new M3LinkedList<>();
            LinkedList<Integer> jdk = new LinkedList<>();
            for (int i = 0; i < n; i++) {
                Integer v = i % 9 == 0 ? null : i % 19;
                m3.addLast(v); jdk.addLast(v);
            }
            m3.verifyInvariants();
            eq(jdk, collect(m3.spliterator(), n), "forward");
            eq(jdk.reversed(), collect(m3.reversed().spliterator(), n), "reversed");
            eq(jdk, StreamSupport.stream(m3.spliterator(), true).toList(), "parallel");
            eq(jdk.reversed(),
                    StreamSupport.stream(m3.reversed().spliterator(), true).toList(),
                    "parallel reverse");
        }
    }

    private static void mutation() {
        M3LinkedList<Integer> m3 = new M3LinkedList<>();
        for (int i = 0; i < 128; i++) m3.addLast(i);
        Spliterator<Integer> late = m3.spliterator();
        m3.addLast(128);
        eq(129L, late.estimateSize(), "late binding");
        m3.addLast(129);
        try {
            late.tryAdvance(v -> { });
            throw new AssertionError("missing modification refusal");
        } catch (ConcurrentModificationException expected) { checks++; }

        Spliterator<Integer> right = m3.reversed().spliterator();
        Spliterator<Integer> left = right.trySplit();
        ok(left != null, "reverse split exists");
        m3.addFirst(-1);
        for (Spliterator<Integer> shard : List.of(right, left)) {
            try {
                shard.tryAdvance(v -> { });
                throw new AssertionError("failed to invalidate both views");
            } catch (ConcurrentModificationException expected) { checks++; }
        }
        try {
            right.tryAdvance(null);
            throw new AssertionError("missing Consumer NPE");
        } catch (NullPointerException expected) { checks++; }
    }

    private static void reuse() {
        Random r = new Random(0x71717L);
        for (int round = 0; round < 80; round++) {
            M3LinkedList<Integer> m3 = new M3LinkedList<>();
            LinkedList<Integer> jdk = new LinkedList<>();
            for (int step = 0; step < 900; step++) {
                if (jdk.isEmpty() || r.nextInt(6) != 0) {
                    Integer value = r.nextInt(7) == 0 ? null : r.nextInt(25);
                    m3.addLast(value);
                    jdk.addLast(value);
                } else {
                    eq(jdk.removeFirst(), m3.removeFirst(), "head recycling");
                }
            }
            m3.verifyInvariants();
            eq(jdk, collect(m3.spliterator(), jdk.size()), "free-slot reuses");
        }
    }

    public static void main(String[] args) {
        corpus();
        mutation();
        reuse();
        System.out.println("M3_LINKED_INDEX_SPLIT_PASS checks=" + checks);
    }
}
