/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.m3.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/** Real Java 21 CopyOnWriteArrayList behavior oracle, without boxing in the M3 owner. */
//FLLDCIM: RELOCATE - donor blob 4b65d351cd8a at ca1aafc4d94f; recipe m3/collections/qualification/census-g2-20261106/relocate.py v4
final class CowLongListContractTest {
    private static long[] contents(CopyOnWriteArrayList<Long> source) {
        long[] values = new long[source.size()];
        for (int i = 0; i < values.length; i++) values[i] = source.get(i);
        return values;
    }

    @Test void seededOperationsPreserveJdkOrderAndOccurrenceBehavior() {
        for (int seed = 0; seed < 32; seed++) {
            Random random = new Random(seed);
            CopyOnWriteArrayList<Long> oracle = new CopyOnWriteArrayList<>();
            CowLongList owner = new CowLongList();
            for (int step = 0; step < 256; step++) {
                long value = switch (step % 13) {
                    case 0 -> Long.MIN_VALUE;
                    case 1 -> Long.MAX_VALUE;
                    default -> random.nextInt(47) - 23L;
                };
                int op = random.nextInt(9);
                if (oracle.size() > 100) op = 8;
                switch (op) {
                    case 0 -> assertEquals(oracle.add(value), owner.add(value));
                    case 1 -> {
                        int index = random.nextInt(oracle.size() + 1);
                        oracle.add(index, value);
                        owner.add(index, value);
                    }
                    case 2 -> {
                        if (!oracle.isEmpty()) {
                            int index = random.nextInt(oracle.size());
                            assertEquals(oracle.set(index, value).longValue(), owner.set(index, value));
                        }
                    }
                    case 3 -> {
                        if (!oracle.isEmpty()) {
                            int index = random.nextInt(oracle.size());
                            assertEquals(oracle.remove(index).longValue(), owner.removeAt(index));
                        }
                    }
                    case 4 -> assertEquals(oracle.remove(Long.valueOf(value)), owner.remove(value));
                    case 5 -> assertEquals(oracle.addIfAbsent(value), owner.addIfAbsent(value));
                    case 6 -> assertEquals(oracle.indexOf(value), owner.indexOf(value));
                    case 7 -> {
                        long[] snapshot = owner.snapshot();
                        var iterator = owner.iterator();
                        oracle.add(991L);
                        owner.add(991L);
                        int seen = 0;
                        while (iterator.hasNext()) assertEquals(snapshot[seen++], iterator.nextLong());
                        assertEquals(snapshot.length, seen);
                    }
                    default -> { oracle.clear(); owner.clear(); }
                }
                assertEquals(oracle.size(), owner.size());
                assertArrayEquals(contents(oracle), owner.snapshot());
            }
        }
    }

    @Test void detachedExportsStableIteratorsAndBounds() {
        CowLongList list = new CowLongList();
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(0));
        assertThrows(IndexOutOfBoundsException.class, () -> list.add(-1, 4));
        list.add(3);
        list.add(4);
        long[] snapshot = list.snapshot();
        snapshot[0] = 88;
        assertEquals(3L, list.get(0));
        var iterator = list.iterator();
        list.clear();
        assertEquals(3L, iterator.nextLong());
        assertEquals(4L, iterator.nextLong());
        assertFalse(iterator.hasNext());
        assertThrows(java.util.NoSuchElementException.class, iterator::nextLong);
        assertThrows(UnsupportedOperationException.class, iterator::remove);
        assertEquals(0, list.size());
    }

    @Test void concurrentAddIfAbsentIsAtomic() throws Exception {
        CowLongList list = new CowLongList();
        ExecutorService executor = Executors.newFixedThreadPool(4);
        CountDownLatch start = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        try {
            for (int worker = 0; worker < 4; worker++) {
                executor.submit(() -> {
                    try {
                        start.await();
                        for (int i = 0; i < 150; i++) list.addIfAbsent(i);
                    } catch (Throwable problem) {
                        failure.compareAndSet(null, problem);
                    }
                });
            }
            start.countDown();
        } finally {
            executor.shutdown();
        }
        assertTrue(executor.awaitTermination(20, TimeUnit.SECONDS));
        if (failure.get() != null) throw new AssertionError(failure.get());
        assertEquals(150, list.size());
        for (int i = 0; i < 150; i++) assertTrue(list.indexOf(i) >= 0);
    }

    @Test void noRetainedEntryNodes() {
        assertTrue(Arrays.stream(CowLongList.class.getDeclaredFields())
                .filter(field -> !java.lang.reflect.Modifier.isStatic(field.getModifiers()))
                .allMatch(field -> field.getType() == long[].class || field.getType() == int.class));
    }
}
