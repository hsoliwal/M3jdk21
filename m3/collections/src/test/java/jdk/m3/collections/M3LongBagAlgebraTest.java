/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * Differential proof of the LongBag algebra restored from PR #1084 (fold-close audit leaf L0030) against a
 * {@code Map<Long, Long>} count model, written to the master bag's conventions (add returns the previous count,
 * remove returns the number removed).
 */
final class M3LongBagAlgebraTest {
    private static Map<Long, Long> model(LongBag bag) {
        Map<Long, Long> m = new HashMap<>();
        bag.forEachCount((k, v) -> assertNull(m.put(k, v), "duplicate key"));
        return m;
    }

    private static LongBag random(Random r, int n) {
        LongBag bag = new LongBag();
        for (int i = 0; i < n; i++) bag.add(r.nextInt(23) - 11, 1 + r.nextInt(5));
        return bag;
    }

    @Test void algebraMatchesCountModel() {
        Random r = new Random(0x1084L);
        for (int trial = 0; trial < 200; trial++) {
            LongBag a = random(r, r.nextInt(40)), b = random(r, r.nextInt(40));
            Map<Long, Long> ma = model(a), mb = model(b);
            Map<Long, Long> union = new HashMap<>(ma), inter = new HashMap<>(), sum = new HashMap<>(ma), diff = new HashMap<>();
            mb.forEach((k, v) -> union.merge(k, v, Math::max));
            ma.forEach((k, v) -> { long x = Math.min(v, mb.getOrDefault(k, 0L)); if (x != 0) inter.put(k, x); });
            mb.forEach((k, v) -> sum.merge(k, v, Long::sum));
            ma.forEach((k, v) -> { long x = Math.max(0L, v - mb.getOrDefault(k, 0L)); if (x != 0) diff.put(k, x); });
            assertEquals(union, model(a.union(b)));
            assertEquals(inter, model(a.intersection(b)));
            assertEquals(sum, model(a.sum(b)));
            assertEquals(diff, model(a.difference(b)));
            assertEquals(ma, model(a.copy()));
            boolean contains = mb.entrySet().stream().allMatch(e -> ma.getOrDefault(e.getKey(), 0L) >= e.getValue());
            assertEquals(contains, a.containsOccurrences(b));
            assertEquals(ma, model(a), "algebra must not mutate its inputs");
            assertEquals(mb, model(b), "algebra must not mutate its inputs");
            assertEquals(a.sum(b).totalCount(), a.totalCount() + b.totalCount());
        }
    }

    @Test void lanesRoundTripAndValidate() {
        LongBag bag = LongBag.fromCounts(new long[] {5, -3, 5, 0, Long.MIN_VALUE}, new long[] {2, 4, 3, 0, 1});
        assertEquals(5, bag.count(5));
        assertEquals(4, bag.count(-3));
        assertFalse(bag.contains(0));
        assertEquals(10, bag.totalCount());
        assertArrayEquals(new long[] {5, 0, 1, 4}, bag.countsFor(new long[] {5, 7, Long.MIN_VALUE, -3}));
        assertThrows(IllegalArgumentException.class, () -> LongBag.fromCounts(new long[] {1}, new long[] {1, 2}));
        assertThrows(IllegalArgumentException.class, () -> LongBag.fromCounts(new long[] {1}, new long[] {-1}));
    }

    @Test void removeAllCompareAndSetAndTrim() {
        LongBag bag = new LongBag(64);
        bag.add(9, 4);
        bag.add(2, 1);
        assertEquals(4, bag.removeAll(9));
        assertEquals(0, bag.removeAll(9));
        assertFalse(bag.setCount(2, 5, 7));
        assertTrue(bag.setCount(2, 1, 7));
        assertEquals(7, bag.count(2));
        assertThrows(IllegalArgumentException.class, () -> bag.setCount(2, -1, 1));
        for (long k = 100; k < 600; k++) bag.add(k, 1);
        for (long k = 100; k < 600; k++) bag.remove(k, 1);
        bag.trimToSize();
        assertEquals(Map.of(2L, 7L), model(bag));
        assertEquals(7, bag.totalCount());
    }

    @Test void sumOverflowLeavesInputsUnchanged() {
        LongBag a = new LongBag(), b = new LongBag();
        a.add(1, Long.MAX_VALUE);
        b.add(2, 1);
        assertThrows(ArithmeticException.class, () -> a.sum(b));
        assertEquals(Map.of(1L, Long.MAX_VALUE), model(a));
        assertEquals(Map.of(2L, 1L), model(b));
    }
}
