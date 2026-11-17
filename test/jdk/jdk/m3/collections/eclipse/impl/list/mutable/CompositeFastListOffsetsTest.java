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
package jdk.m3.collections.eclipse.impl.list.mutable;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ObjectStreamClass;
import java.io.ObjectStreamField;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import jdk.m3.collections.eclipse.api.list.MutableList;
import jdk.m3.collections.eclipse.impl.list.mutable.OffsetsCallbacks.ProbeSet;
import jdk.m3.collections.eclipse.impl.list.mutable.OffsetsTwin.NamedOp;
import org.junit.jupiter.api.Test;

/**
 * CompositeFastList finds the lane of an index through a transient Fenwick offset tree while
 * every lane is private: driven in lockstep with a renamed verbatim copy of the pre-patch class
 * (LegacyCompositeFastList), every result, exception message, callback trace, lane layout with
 * backing-array tail and get(i) must agree, and the tree must mirror the live lane sizes.
 */
//FLLDCIM: LEETCODIFY — lockstep proof of the CompositeFastList Fenwick lane offsets
class CompositeFastListOffsetsTest {

    private static final System.Logger LOG =
            System.getLogger(CompositeFastListOffsetsTest.class.getName());
    private static final int SEEDS = Integer.getInteger("offsets.seeds", 2000);
    private static final int STEPS = Integer.getInteger("offsets.steps", 400);
    private static final long GUARD = 1000;
    private static final int[][] TABLES = {
        {0, 3, 0, 0, 5, 1, 0, 2, 4, 0, 7},
        {0, 3, 0, 0, 5, 1, 0, 2, 4, 0, 7, 0, 0},
    };

    @Test
    void lockstepMatchesLegacyWalk() throws Throwable {
        OffsetsState.Coverage cov = new OffsetsState.Coverage();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread driver = new Thread(() -> runSeeds(cov, failure), "offsets-lockstep");
        driver.start();
        driver.join();
        if (failure.get() != null) {
            throw failure.get();
        }
        String counts = describe(cov);
        LOG.log(System.Logger.Level.INFO, "CompositeFastList offsets coverage {0}", counts);
        assertTrue(cov.served * 10 >= cov.indexed * 3, "tree served under 30%: " + counts);
        assertTrue(cov.boundary >= GUARD && cov.emptyBefore >= GUARD, counts);
        assertTrue(cov.stalePast >= GUARD && cov.reentrantRetain >= GUARD, counts);
    }

    private static String describe(OffsetsState.Coverage cov) {
        return "indexed=" + cov.indexed + " served=" + cov.served + " boundary=" + cov.boundary
                + " emptyBefore=" + cov.emptyBefore + " stalePast=" + cov.stalePast
                + " stalePastTree=" + cov.stalePastTree + " reentrantRetain=" + cov.reentrantRetain
                + " ops=" + cov.ops + " meanSize=" + cov.sizeSum / Math.max(cov.ops, 1);
    }

    @Test
    void serialFormKeepsListsAndSize() {
        ObjectStreamClass form = ObjectStreamClass.lookup(CompositeFastList.class);
        List<String> names = Arrays.stream(form.getFields()).map(ObjectStreamField::getName)
                .sorted().toList();
        assertEquals(List.of("lists", "size"), names);
        assertEquals(2L, form.getSerialVersionUID());
        MutableList<Object> live = privateComposite(12, 3);
        live.get(0);
        assertTrue(OffsetsState.tree(live).isPresent(), "tree live before serialization");
        MutableList<Object> copy = OffsetsSessions.roundTrip(live);
        OffsetsState.assertDeserialized(copy);
        assertEquals(OffsetsState.elements(live), OffsetsState.elements(copy));
        assertEquals(live.get(20), copy.get(20));
    }

    @Test
    void concurrentReadersSeeSnapshot() throws InterruptedException {
        MutableList<Object> shared = privateComposite(32, 5);
        OffsetsState.dropTree(shared);
        Object[] snapshot = OffsetsState.elements(shared).toArray();
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger mismatches = new AtomicInteger();
        List<Thread> readers = IntStream.range(0, 8).mapToObj(k -> new Thread(
                () -> readAll(shared, snapshot, start, mismatches))).toList();
        readers.forEach(Thread::start);
        start.countDown();
        for (Thread reader : readers) {
            reader.join();
        }
        assertEquals(0, mismatches.get(), "concurrent get mismatches");
        assertArrayEquals(OffsetsState.fenwick(OffsetsState.lanes(shared)),
                OffsetsState.tree(shared).orElseThrow(), "tree after concurrent build");
    }

    @Test
    void boundaryTableMatchesLegacy() {
        OffsetsState.Coverage cov = new OffsetsState.Coverage();
        for (int[] table : TABLES) {
            int total = Arrays.stream(table).sum();
            for (int kind = 0; kind < 4; kind++) {
                for (int i = -1; i <= total + 1; i++) {
                    OffsetsTwin twin = tableTwin(table, cov);
                    assertTrue(OffsetsState.tree(twin.patched).isPresent(), "table tree");
                    twin.apply(indexedOp(kind, i), -1, 0);
                }
            }
        }
        assertTrue(cov.boundary > 0 && cov.emptyBefore > 0, "table boundaries probed");
    }

    @Test
    void staleSizeTakesLegacyWalk() {
        OffsetsState.Coverage cov = new OffsetsState.Coverage();
        int[] table = new int[12];
        Arrays.fill(table, 3);
        for (int throwAt = 1; throwAt <= 36; throwAt++) {
            OffsetsTwin twin = tableTwin(table, cov);
            ProbeSet keep = new ProbeSet(List.of(100, 101, 102));
            twin.apply(OffsetsOps.named("retainAll", (t, s) -> t.list(s).retainAll(keep)),
                    throwAt, 0);
            int size = twin.patched.size();
            for (int i = Math.max(size - 6, 0); i <= size; i++) {
                twin.apply(indexedOp(2, i), -1, 0);
                twin.apply(indexedOp(3, i), -1, 0);
            }
        }
        assertTrue(cov.stalePastTree > 0, "stale size met a live tree");
    }

    /** Runs every seed on a shallow stack: exception stack traces stay cheap. */
    private static void runSeeds(OffsetsState.Coverage cov, AtomicReference<Throwable> failure) {
        try {
            for (int seed = 0; seed < SEEDS; seed++) {
                new OffsetsTwin(seed, cov).run(STEPS);
            }
        } catch (Throwable thrown) {
            failure.set(thrown);
        }
    }

    private static void readAll(MutableList<Object> shared, Object[] snapshot,
            CountDownLatch start, AtomicInteger mismatches) {
        try {
            start.await();
        } catch (InterruptedException stop) {
            Thread.currentThread().interrupt();
            return;
        }
        for (int round = 0; round < 50; round++) {
            for (int i = 0; i < snapshot.length; i++) {
                mismatches.addAndGet(shared.get(i) == snapshot[i] ? 0 : 1);
            }
        }
    }

    /** lanes private lanes of 1..width distinct Integers each. */
    private static MutableList<Object> privateComposite(int lanes, int width) {
        MutableList<Object> composite = new CompositeFastList<>();
        int next = 0;
        for (int p = 0; p < lanes; p++) {
            List<Object> lane = new ArrayList<>();
            for (int k = 0; k <= p % width; k++) {
                lane.add(next++);
            }
            composite.addAll(lane);
        }
        return composite;
    }

    /** Twins whose private lanes have the table's sizes; empty lanes are emptied markers. */
    private static OffsetsTwin tableTwin(int[] table, OffsetsState.Coverage cov) {
        OffsetsTwin twin = new OffsetsTwin(1, cov);
        int next = 100;
        for (int width : table) {
            List<Object> lane = new ArrayList<>();
            for (int k = 0; k < width; k++) {
                lane.add(next++);
            }
            twin.apply(width == 0 ? OffsetsOps.emptyLane(twin)
                    : OffsetsOps.named("table", (t, s) -> t.list(s).addAll(lane)), -1, 0);
        }
        return twin;
    }

    /** 0 get, 1 set, 2 remove(int), 3 add(int, E) at index i. */
    private static NamedOp indexedOp(int kind, int i) {
        return switch (kind) {
            case 0 -> new NamedOp("get", (t, s) -> t.list(s).get(i), i, OffsetsTwin.INDEXED);
            case 1 -> new NamedOp("set", (t, s) -> t.list(s).set(i, 55), i, OffsetsTwin.INDEXED);
            case 2 -> new NamedOp("removeAt", (t, s) -> t.list(s).remove(i), i,
                    OffsetsTwin.INDEXED);
            default -> new NamedOp("addAt", (t, s) -> {
                t.list(s).add(i, 66);
                return "ok";
            }, i, OffsetsTwin.INSERT);
        };
    }
}
