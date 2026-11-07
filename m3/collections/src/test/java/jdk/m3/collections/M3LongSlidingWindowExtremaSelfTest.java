/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Random;

/** Independent deque oracle for every existing public method and lazy-extrema proofs. */
public final class M3LongSlidingWindowExtremaSelfTest {
    private static long checks;
    private static long steps;
    private M3LongSlidingWindowExtremaSelfTest() { }

    private static void check(boolean ok, String message) {
        checks++;
        if (!ok) throw new AssertionError(message);
    }

    private static void emptyThrows(LongSlidingWindow window) {
        try { window.min(); throw new AssertionError("min empty"); }
        catch (NoSuchElementException expected) { checks++; }
        try { window.max(); throw new AssertionError("max empty"); }
        catch (NoSuchElementException expected) { checks++; }
        try { window.oldest(); throw new AssertionError("oldest empty"); }
        catch (NoSuchElementException expected) { checks++; }
        try { window.newest(); throw new AssertionError("newest empty"); }
        catch (NoSuchElementException expected) { checks++; }
    }

    private static void verify(LongSlidingWindow window, ArrayDeque<Long> oracle, int capacity) {
        check(window.size() == oracle.size(), "size");
        check(window.isEmpty() == oracle.isEmpty(), "empty");
        check(window.isFull() == (oracle.size() == capacity), "full");
        check(window.capacity() == capacity, "capacity");
        check(window.payloadBytes() == capacity * Long.BYTES, "payloadBytes contract");
        long sum = 0L;
        long expectedMin = Long.MAX_VALUE;
        long expectedMax = Long.MIN_VALUE;
        int index = 0;
        long[] expected = new long[oracle.size()];
        for (long value : oracle) {
            sum += value;  // Java two's-complement wrap is part of existing sum behavior.
            expectedMin = Math.min(expectedMin, value);
            expectedMax = Math.max(expectedMax, value);
            expected[index] = value;
            check(window.getFromOldest(index++) == value, "indexed read");
        }
        long[] visited = new long[oracle.size()];
        int[] written = {0};
        window.forEach(v -> visited[written[0]++] = v);
        check(Arrays.equals(expected, visited), "forEach order");
        check(window.sum() == sum, "wrapped sum");
        check(Double.compare(window.average(), oracle.isEmpty() ? Double.NaN
                : sum / (double) oracle.size()) == 0, "average");
        if (oracle.isEmpty()) {
            emptyThrows(window);
        } else {
            check(window.oldest() == oracle.getFirst(), "oldest");
            check(window.newest() == oracle.getLast(), "newest");
            check(window.min() == expectedMin, "min");
            check(window.max() == expectedMax, "max");
            for (int i = 0; i < 3; i++) {
                check(window.min() == expectedMin, "cached/repeated min");
                check(window.max() == expectedMax, "cached/repeated max");
            }
        }
        steps++;
    }

    private static void randomized(int capacity, int seed) {
        LongSlidingWindow window = new LongSlidingWindow(capacity);
        ArrayDeque<Long> oracle = new ArrayDeque<>();
        Random random = new Random(0x6d335f4c4c445750L ^ seed ^ capacity);
        for (int step = 0; step < 30000; step++) {
            int choice = random.nextInt(100);
            if (choice < 4) {
                window.clear();
                oracle.clear();
            } else {
                long value = switch (choice % 11) {
                    case 0 -> Long.MIN_VALUE;
                    case 1 -> Long.MAX_VALUE;
                    case 2 -> 0L;
                    case 3 -> -1L;
                    case 4 -> 1L;
                    case 5 -> 42L;
                    case 6 -> -42L;
                    case 7 -> step % 3;
                    default -> random.nextLong();
                };
                long defaultValue = Long.MIN_VALUE + step;
                long expected = oracle.size() == capacity ? oracle.removeFirst() : defaultValue;
                oracle.addLast(value);
                check(window.add(value, defaultValue) == expected, "evicted/default value");
            }
            verify(window, oracle, capacity);
        }
    }

    /** Test-only reflection inspects optional cache state; baseline has no cache. */
    private static void cacheState() throws ReflectiveOperationException {
        Field minKnown;
        Field maxKnown;
        try {
            minKnown = LongSlidingWindow.class.getDeclaredField("minKnown");
            maxKnown = LongSlidingWindow.class.getDeclaredField("maxKnown");
        } catch (NoSuchFieldException originalSource) {
            return;
        }
        minKnown.setAccessible(true);
        maxKnown.setAccessible(true);
        LongSlidingWindow window = new LongSlidingWindow(3);
        window.add(1L, -99L);
        window.add(9L, -99L);
        window.add(3L, -99L);
        check(!minKnown.getBoolean(window) && !maxKnown.getBoolean(window),
                "cache must not be eagerly enabled");
        check(window.min() == 1L, "first min");
        check(minKnown.getBoolean(window) && maxKnown.getBoolean(window),
                "one initial scan must prepare both extrema");
        window.add(7L, -99L); // Expire unique min, retain max.
        check(!minKnown.getBoolean(window) && maxKnown.getBoolean(window),
                "invalidate unique minimum, preserve proven maximum");
        check(window.min() == 3L && window.max() == 9L, "refresh min");
        window.add(4L, -99L); // Expire unique max.
        check(minKnown.getBoolean(window) && !maxKnown.getBoolean(window),
                "invalidate unique maximum, preserve proven minimum");
        check(window.max() == 7L && window.min() == 3L, "refresh max");
        window.clear();
        check(!minKnown.getBoolean(window) && !maxKnown.getBoolean(window),
                "clear must invalidate both caches");
        window.add(5L, -99L);
        check(window.min() == 5L && window.max() == 5L, "single-value refresh");
        window.add(5L, -99L);
        window.add(5L, -99L);
        window.add(5L, -99L); // expired min/max equal incoming, remain certified.
        check(minKnown.getBoolean(window) && maxKnown.getBoolean(window),
                "same-value replacement remains certified");
    }

    public static void main(String[] args) throws Exception {
        try { new LongSlidingWindow(0); throw new AssertionError("zero capacity accepted"); }
        catch (IllegalArgumentException expected) { checks++; }
        for (int capacity : new int[] {1, 2, 3, 4, 7, 16, 31, 64}) randomized(capacity, 1);
        cacheState();
        System.out.println("PASS window oracle: steps=" + steps + " checks=" + checks);
    }
}
