// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** Exact Java21 provider oracle; no JDK source or LeetCode implementation copied. */
//FLLDCIM: RELOCATE - donor blob a55968fc1547 at ca1aafc4d94f; recipe m3/collections/qualification/census-g2-20261106/relocate.py v4
public final class M3CowSetDifferential {
    private static long checks;

    private static void eq(Object actual, Object expected, String stage) {
        checks++;
        if (!java.util.Objects.equals(actual, expected)) {
            throw new AssertionError(stage + " actual=" + actual + " expected=" + expected);
        }
    }

    private static Object value(Random random) {
        return switch (random.nextInt(18)) {
            case 0 -> null;
            case 1 -> Integer.MIN_VALUE;
            case 2 -> Integer.MAX_VALUE;
            default -> random.nextInt(35) - 17;
        };
    }

    private static List<Object> snap(Iterator<?> iterator) {
        List<Object> result = new ArrayList<>();
        while (iterator.hasNext()) result.add(iterator.next());
        return result;
    }

    private static void randomDifferential() {
        Random seed = new Random(0xC0FFEE12L);
        for (int sample = 0; sample < 128; sample++) {
            M3CowSet<Object> actual = new M3CowSet<>();
            CopyOnWriteArraySet<Object> expected = new CopyOnWriteArraySet<>();
            for (int step = 0; step < 900; step++) {
                Object candidate = value(seed);
                int op = seed.nextInt(11);
                String stage = sample + ":" + step + " operation " + op;
                if (op <= 2) eq(actual.add(candidate), expected.add(candidate), stage + " add");
                else if (op <= 4) eq(actual.remove(candidate), expected.remove(candidate), stage + " remove");
                else if (op == 5) eq(actual.contains(candidate), expected.contains(candidate), stage + " contains");
                else if (op == 6) {
                    Iterator<Object> old = actual.iterator();
                    Iterator<Object> baseline = expected.iterator();
                    actual.add(candidate);
                    expected.add(candidate);
                    eq(snap(old), snap(baseline), stage + " snapshot");
                } else if (op == 7) {
                    Iterator<Object> old = actual.iterator();
                    Iterator<Object> baseline = expected.iterator();
                    actual.clear();
                    expected.clear();
                    eq(snap(old), snap(baseline), stage + " clear snapshot");
                } else if (op == 8) eq(actual.isEmpty(), expected.isEmpty(), stage + " empty");
                else if (op == 9) eq(actual.size(), expected.size(), stage + " size");
                else eq(actual.contains(null), expected.contains(null), stage + " null");
                eq(actual.size(), expected.size(), stage + " size post");
                eq(snap(actual.iterator()), snap(expected.iterator()), stage + " values post");
            }
        }
    }

    private static void boundaries() {
        M3CowSet<Integer> actual = new M3CowSet<>();
        CopyOnWriteArraySet<Integer> expected = new CopyOnWriteArraySet<>();
        eq(M3CowSet.scaffoldDescriptor(),
                "M3CowSet|COW_SET|OBJECT|FROZEN_EPOCH|Object[] valueLane", "descriptor");
        Iterator<Integer> empty = actual.iterator();
        eq(empty.hasNext(), false, "empty iterator");
        try { empty.next(); throw new AssertionError("exhausted iterator"); }
        catch (NoSuchElementException accepted) { checks++; }
        try { empty.remove(); throw new AssertionError("snapshot mutator"); }
        catch (UnsupportedOperationException accepted) { checks++; }

        for (Integer element : Arrays.asList(1, 2, null, Integer.MIN_VALUE,
                Integer.MAX_VALUE, 2)) {
            eq(actual.add(element), expected.add(element), "boundary add");
        }
        Iterator<Integer> historical = actual.iterator();
        Iterator<Integer> baseline = expected.iterator();
        eq(actual.remove(null), expected.remove(null), "remove null");
        eq(actual.remove(2), expected.remove(2), "remove second");
        eq(snap(historical), snap(baseline), "snapshot after deletes");
        eq(snap(actual.iterator()), snap(expected.iterator()), "latest snapshot");
        actual.clear();
        expected.clear();
        eq(actual.size(), expected.size(), "clear");
    }

    private static void concurrentWriters() throws Exception {
        M3CowSet<Integer> actual = new M3CowSet<>();
        CountDownLatch start = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();
        List<Thread> threads = new ArrayList<>();
        for (int worker = 0; worker < 4; worker++) {
            final int workerId = worker;
            threads.add(new Thread(() -> {
                try {
                    start.await();
                    for (int i = 0; i < 700; i++) {
                        actual.add(workerId * 700 + i);
                        if (i % 17 == 0) {
                            Iterator<Integer> snapshot = actual.iterator();
                            while (snapshot.hasNext()) {
                                if (snapshot.next() == null) throw new AssertionError("unexpected null");
                            }
                        }
                    }
                } catch (Throwable problem) { error.compareAndSet(null, problem); }
            }));
        }
        for (Thread thread : threads) thread.start();
        start.countDown();
        for (Thread thread : threads) {
            thread.join(TimeUnit.SECONDS.toMillis(20));
            if (thread.isAlive()) throw new AssertionError("stalled writer");
        }
        if (error.get() != null) throw new AssertionError(error.get());
        eq(actual.size(), 2800, "four writers without lost updates");
        for (int value = 0; value < 2800; value++)
            eq(actual.contains(value), true, "postwriter membership");
        eq(snap(actual.iterator()).size(), 2800, "final snapshot cardinality");
    }

    public static void main(String[] args) throws Exception {
        boundaries();
        randomDifferential();
        concurrentWriters();
        System.out.println("M3CowSetDifferential PASS checks=" + checks);
    }
}
