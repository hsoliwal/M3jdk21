// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Standalone Java 21 behavioral proof; not a substitute for the product JUnit/SDK suite. */
public final class M3LongCircularFifoQueueTest {
    private static long checks;

    private static void yes(boolean okay, String message) {
        checks++;
        if (!okay) throw new AssertionError(message);
    }

    private static void expect(Class<? extends Throwable> type, Runnable action) {
        checks++;
        try { action.run(); }
        catch (Throwable ex) {
            if (type.isInstance(ex)) return;
            throw new AssertionError("expected " + type + ", got " + ex, ex);
        }
        throw new AssertionError("expected " + type);
    }

    private static long[] sample(Random random, int count) {
        long[] values = new long[count];
        for (int i = 0; i < count; i++) {
            values[i] = switch (random.nextInt(8)) {
                case 0 -> 0L;
                case 1 -> Long.MIN_VALUE;
                case 2 -> Long.MAX_VALUE;
                case 3 -> -1L;
                default -> random.nextLong();
            };
        }
        return values;
    }

    private static void offer(ArrayDeque<Long> oracle, int cap, long value) {
        if (oracle.size() == cap) oracle.removeFirst();
        oracle.addLast(value);
    }

    private static void checkState(M3LongCircularFifoQueue queue, ArrayDeque<Long> oracle, int cap) {
        yes(queue.capacity() == cap, "capacity");
        yes(queue.size() == oracle.size(), "size");
        yes(queue.remainingCapacity() == cap - oracle.size(), "remaining capacity");
        yes(queue.isAtFullCapacity() == (oracle.size() == cap), "full");
        long[] actual = queue.toArray();
        long[] expected = oracle.stream().mapToLong(Long::longValue).toArray();
        yes(Arrays.equals(actual, expected), "order: " + Arrays.toString(actual) + " vs " + Arrays.toString(expected));
        M3LongIterator cursor = queue.iterator();
        int i = 0;
        while (cursor.hasNext()) {
            yes(cursor.nextLong() == expected[i++], "iterator value");
        }
        yes(i == expected.length, "iterator length");
        expect(NoSuchElementException.class, cursor::nextLong);
        for (i = 0; i < expected.length; i++) yes(queue.get(i) == expected[i], "index " + i);
        if (oracle.isEmpty()) {
            expect(NoSuchElementException.class, queue::peek);
            expect(NoSuchElementException.class, queue::poll);
        } else {
            yes(queue.peek() == oracle.peekFirst(), "peek");
        }
    }

    @Test
    public void randomizedDifferential() {
        expect(IllegalArgumentException.class, () -> new M3LongCircularFifoQueue(0));
        expect(IllegalArgumentException.class, () -> new M3LongCircularFifoQueue(-1));
        yes(new M3LongCircularFifoQueue().capacity() == 32, "default cap");
        final Random random = new Random(0x6d3354a2L);
        long transitions = 0;
        for (int cap : new int[] {1, 2, 3, 4, 7, 8, 16, 17, 31, 32, 97}) {
            M3LongCircularFifoQueue queue = new M3LongCircularFifoQueue(cap);
            ArrayDeque<Long> oracle = new ArrayDeque<>();
            for (int step = 0; step < 9000; step++) {
                switch (random.nextInt(10)) {
                    case 0, 1, 2, 3 -> {
                        long val = sample(random, 1)[0];
                        yes(queue.offer(val), "offer accepted");
                        offer(oracle, cap, val);
                    }
                    case 4 -> {
                        if (oracle.isEmpty()) {
                            expect(NoSuchElementException.class, queue::poll);
                        } else {
                            yes(queue.poll() == oracle.removeFirst(), "poll");
                        }
                    }
                    case 5, 6 -> {
                        int count = random.nextInt(cap * 3 + 1);
                        long[] input = sample(random, count + 5);
                        int offset = random.nextInt(6);
                        yes(queue.offerBatch(input, offset, count) == count, "batch result");
                        for (int k = offset; k < offset + count; k++) offer(oracle, cap, input[k]);
                    }
                    case 7 -> {
                        queue.clear();
                        oracle.clear();
                    }
                    case 8 -> {
                        long probe = sample(random, 1)[0];
                        yes(queue.contains(probe) == oracle.contains(probe), "contains");
                    }
                    case 9 -> {
                        M3LongIterator cursor = queue.iterator();
                        long[] snapshot = queue.toArray();
                        expect(IndexOutOfBoundsException.class, () -> queue.get(-1));
                        expect(IndexOutOfBoundsException.class, () -> queue.get(queue.size()));
                        expect(IndexOutOfBoundsException.class, () -> queue.offerBatch(new long[2], 1, 2));
                        yes(Arrays.equals(snapshot, queue.toArray()), "preflight no mutation");
                        yes(cursor.hasNext() == !oracle.isEmpty(), "failed preflight iterator retained");
                        yes(queue.offerBatch(new long[]{44}, 1, 0) == 0, "no-op batch");
                        yes(cursor.hasNext() == !oracle.isEmpty(), "no-op iterator retained");
                        queue.offer(5L);
                        offer(oracle, cap, 5L);
                        expect(ConcurrentModificationException.class, cursor::hasNext);
                    }
                    default -> throw new AssertionError();
                }
                checkState(queue, oracle, cap);
                transitions++;
            }
        }
        yes(transitions == 99_000L && checks > 3_000_000L, "complete seeded oracle");
    }
}
