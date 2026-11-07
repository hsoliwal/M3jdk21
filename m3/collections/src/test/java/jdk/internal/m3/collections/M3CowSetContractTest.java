/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/** Existing public M3 owner, JDK provider oracle, no Java SE type replacement. */
//FLLDCIM: RELOCATE - donor blob fbce36f189cd at ca1aafc4d94f; recipe m3/collections/qualification/census-g2-20261106/relocate.py v4
final class M3CowSetContractTest {
    @Test
    void publicContractAndSnapshotIterators() {
        M3CowSet<Integer> set = new M3CowSet<>();
        assertEquals("M3CowSet|COW_SET|OBJECT|FROZEN_EPOCH|Object[] valueLane",
                M3CowSet.scaffoldDescriptor());
        assertTrue(set.isEmpty());
        assertTrue(set.add(1));
        assertTrue(set.contains(1));
        assertFalse(set.add(1));
        assertTrue(set.add(null));
        assertTrue(set.add(Integer.MIN_VALUE));
        Iterator<Integer> previous = set.iterator();
        assertTrue(set.remove(null));
        assertFalse(set.remove(null));
        set.clear();
        assertEquals(java.util.Arrays.asList(1, null, Integer.MIN_VALUE), collect(previous));
        assertEquals(0, set.size());
        assertThrows(UnsupportedOperationException.class, () -> set.iterator().remove());
        assertThrows(java.util.NoSuchElementException.class, () -> set.iterator().next());
    }

    @Test
    void java21CopyOnWriteArraySetDifferential() {
        Random random = new Random(0xC0FFEE12L);
        for (int trial = 0; trial < 128; trial++) {
            M3CowSet<Integer> subject = new M3CowSet<>();
            CopyOnWriteArraySet<Integer> jdk = new CopyOnWriteArraySet<>();
            for (int step = 0; step < 600; step++) {
                Integer value = random.nextInt(18) == 0 ? null : random.nextInt(49) - 24;
                switch (random.nextInt(7)) {
                    case 0, 1 -> assertEquals(jdk.add(value), subject.add(value));
                    case 2, 3 -> assertEquals(jdk.remove(value), subject.remove(value));
                    case 4 -> assertEquals(jdk.contains(value), subject.contains(value));
                    case 5 -> {
                        Iterator<Integer> old = subject.iterator();
                        Iterator<Integer> expected = jdk.iterator();
                        subject.clear();
                        jdk.clear();
                        assertEquals(collect(expected), collect(old));
                    }
                    default -> assertEquals(jdk.isEmpty(), subject.isEmpty());
                }
                assertEquals(jdk.size(), subject.size());
                assertEquals(collect(jdk.iterator()), collect(subject.iterator()));
            }
        }
    }

    @Test
    void concurrentWritersPublishCompleteEpochs() throws Exception {
        M3CowSet<Integer> set = new M3CowSet<>();
        CountDownLatch start = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        List<Thread> threads = new ArrayList<>();
        for (int worker = 0; worker < 4; worker++) {
            final int offset = worker * 250;
            Thread thread = new Thread(() -> {
                try {
                    start.await();
                    for (int i = 0; i < 250; i++) {
                        set.add(offset + i);
                        for (Integer value : collect(set.iterator())) {
                            if (value == null) throw new AssertionError("unexpected null");
                        }
                    }
                } catch (Throwable error) {
                    failure.compareAndSet(null, error);
                }
            });
            threads.add(thread);
            thread.start();
        }
        start.countDown();
        for (Thread thread : threads) {
            thread.join(20_000L);
            assertFalse(thread.isAlive(), "writer stalled");
        }
        if (failure.get() != null) throw new AssertionError(failure.get());
        assertEquals(1_000, set.size());
        for (int value = 0; value < 1_000; value++) assertTrue(set.contains(value));
    }

    private static List<Integer> collect(Iterator<Integer> input) {
        List<Integer> values = new ArrayList<>();
        input.forEachRemaining(values::add);
        return values;
    }

}
