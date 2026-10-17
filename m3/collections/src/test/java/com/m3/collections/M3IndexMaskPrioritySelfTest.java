/* Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.PriorityQueue;
import java.util.Random;

/**
 * Actual M3 priority heap mask differential against independent JDK priority
 * behavior. The test oracle may box; the runtime path never creates entries.
 */
public final class M3IndexMaskPrioritySelfTest {
    private static long assertions;

    private M3IndexMaskPrioritySelfTest() { }

    public static void main(String[] args) {
        for (int seed = 0; seed < 128; seed++) exercise(seed);
        boundaries();
        storage();
        System.out.println("PASS M3IndexMaskPrioritySelfTest checks=" + assertions);
    }

    private static void exercise(int seed) {
        Random random = new Random(0x4d335052494f5249L ^ seed);
        M3LongPriorityQueue actual = new M3LongPriorityQueue(0);
        PriorityQueue<Long> oracle = new PriorityQueue<>();
        for (int step = 0; step < 64; step++) {
            long item = value(random, step);
            check(actual.offer(item), "offer returns true");
            check(oracle.offer(item), "JDK oracle offer");
        }
        for (int round = 0; round < 28; round++) {
            long[] encountered = actual.toArray();
            check(encountered.length == oracle.size(), "initial count");
            byte[] states = new byte[encountered.length];
            int mask = (round + seed) & 255;
            for (int i = 0; i < states.length; i++) {
                states[i] = (byte) random.nextInt(256);
            }
            M3LongIterator cursor = actual.iterator();
            int removed = 0;
            for (int i = 0; i < encountered.length; i++) {
                if ((((states[i] & 255) & mask) == mask)) continue;
                check(oracle.remove(encountered[i]), "JDK remove occurrence");
                removed++;
            }
            check(actual.retainByStateMask(states, mask) == removed, "removed count");
            if (removed > 0) {
                fails(ConcurrentModificationException.class, cursor::hasNext);
            } else {
                check(cursor.hasNext() == (encountered.length != 0),
                        "no-op cursor still valid");
            }
            check(actual.size() == oracle.size(), "size matches JDK");
            long[] sorted = actual.toSortedArray();
            PriorityQueue<Long> detached = new PriorityQueue<>(oracle);
            for (long value : sorted) {
                check(!detached.isEmpty() && detached.poll() == value, "heap sorted oracle");
            }
            check(detached.isEmpty(), "no extra JDK members");
            if (!oracle.isEmpty()) {
                check(actual.firstLong() == oracle.peek(), "minimum preserved");
            }
            // Heapification followed by additions exercises rebuilt heap geometry.
            for (int i = 0; i < 5; i++) {
                long item = value(random, round * 7 + i);
                actual.offer(item);
                oracle.add(item);
            }
        }
        while (!oracle.isEmpty()) {
            check(actual.removeFirstLong() == oracle.remove(),
                    "successive priority poll");
        }
        check(actual.isEmpty(), "fully drained");
    }

    private static void boundaries() {
        M3LongPriorityQueue queue = new M3LongPriorityQueue();
        queue.offer(Long.MAX_VALUE);
        queue.offer(0);
        queue.offer(Long.MIN_VALUE);
        long[] prior = queue.toArray();
        for (int mask : new int[] {-1, 256, Integer.MAX_VALUE}) {
            fails(IllegalArgumentException.class,
                    () -> queue.retainByStateMask(new byte[] {0, 0, 0}, mask));
        }
        fails(IllegalArgumentException.class,
                () -> queue.retainByStateMask(new byte[] {0, 0}, 0));
        fails(NullPointerException.class,
                () -> queue.retainByStateMask(null, 0));
        eq(prior, queue.toArray());
        M3LongIterator cursor = queue.iterator();
        check(queue.retainByStateMask(new byte[] {0, 0, 0}, 0) == 0,
                "zero mask accepts all");
        check(cursor.hasNext(), "noop leaves cursor live");
        check(queue.retainByStateMask(new byte[] {0, 0, 0}, 255) == 3,
                "full removal");
        check(queue.isEmpty(), "queue empty after filter");
        check(queue.offer(0), "offer after empty");
        check(queue.firstLong() == 0, "new zero minimum");
    }

    private static void storage() {
        for (Field field : M3LongPriorityQueue.class.getDeclaredFields()) {
            Class<?> type = field.getType();
            if (type.isArray()) check(type.getComponentType().isPrimitive(),
                    "no retained Object[] heap");
        }
    }

    private static long value(Random random, int step) {
        return switch (step % 13) {
            case 0 -> Long.MIN_VALUE;
            case 1 -> Long.MAX_VALUE;
            case 2 -> 0;
            default -> random.nextInt(63) - 31L;
        };
    }

    private static void eq(long[] expected, long[] actual) {
        assertions++;
        if (!Arrays.equals(expected, actual)) {
            throw new AssertionError("arrays differ: " + Arrays.toString(expected)
                    + " / " + Arrays.toString(actual));
        }
    }

    private static void check(boolean value, String reason) {
        assertions++;
        if (!value) throw new AssertionError(reason);
    }

    private static void fails(Class<? extends Throwable> expected, Runnable action) {
        assertions++;
        try { action.run(); }
        catch (Throwable exception) {
            if (expected.isInstance(exception)) return;
            throw new AssertionError("wrong failure", exception);
        }
        throw new AssertionError("expected " + expected.getSimpleName());
    }
}
