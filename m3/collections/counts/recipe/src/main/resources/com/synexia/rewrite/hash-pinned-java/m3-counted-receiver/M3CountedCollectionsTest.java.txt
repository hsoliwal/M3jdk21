/* SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors */
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;

final class M3CountedCollectionsTest {
    static Map<Long, Long> snapshot(M3LongBag bag) {
        Map<Long, Long> result = new HashMap<>(); bag.forEachCount(result::put); return result;
    }
    static long total(Map<Long, Long> values) { return values.values().stream().mapToLong(Long::longValue).sum(); }
    @Test void randomMutableCountsAgainstJdk() {
        for (int seed = 0; seed < 32; seed++) {
            Random random = new Random(seed); M3LongBag bag = new M3LongBag(); Map<Long, Long> oracle = new HashMap<>();
            for (int step = 0; step < 4000; step++) {
                long key = random.nextInt(129) - 64, n = random.nextInt(100), old = oracle.getOrDefault(key, 0L);
                switch (random.nextInt(6)) {
                    case 0 -> { n++; assertEquals(old + n, bag.add(key, n)); oracle.put(key, old + n); }
                    case 1 -> { n++; long next = Math.max(0, old - n); assertEquals(next, bag.remove(key, n)); if (next == 0) oracle.remove(key); else oracle.put(key, next); }
                    case 2 -> { assertEquals(old, bag.setCount(key, n)); if (n == 0) oracle.remove(key); else oracle.put(key, n); }
                    case 3 -> { assertEquals(old, bag.removeAll(key)); oracle.remove(key); }
                    case 4 -> { assertFalse(bag.setCount(key, old + 1, n)); assertTrue(bag.setCount(key, old, old)); }
                    default -> { assertEquals(old, bag.count(key)); if ((step & 63) == 0) bag.trimToSize(); }
                }
                assertEquals(total(oracle), bag.size()); assertEquals(oracle.size(), bag.distinctSize());
                if ((step & 127) == 0) assertEquals(oracle, snapshot(bag));
            }
            assertEquals(oracle, snapshot(bag));
        }
    }
    @Test void extremesAndOverflowLeaveInputsUnchanged() {
        M3LongBag bag = new M3LongBag(); bag.add(Long.MIN_VALUE, Long.MAX_VALUE);
        assertThrows(ArithmeticException.class, () -> bag.add(Long.MAX_VALUE));
        assertThrows(ArithmeticException.class, () -> bag.add(Long.MIN_VALUE));
        assertThrows(ArithmeticException.class, () -> bag.setCount(0, 1));
        assertEquals(Map.of(Long.MIN_VALUE, Long.MAX_VALUE), snapshot(bag));
        assertThrows(IllegalArgumentException.class, () -> bag.setCount(1, 0, -1));
        assertThrows(IllegalArgumentException.class, () -> bag.add(1, 0));
        assertThrows(IllegalArgumentException.class, () -> bag.remove(1, 0));
        assertThrows(IllegalArgumentException.class, () -> bag.setCount(1, -1, 0));
        assertEquals(Long.MAX_VALUE, bag.removeAll(Long.MIN_VALUE)); assertTrue(bag.isEmpty());
    }
    @Test void snapshotAlgebraMatchesIndependentCounts() {
        for (int seed = 0; seed < 128; seed++) {
            Random r = new Random(seed); M3LongBag left = new M3LongBag(512), right = new M3LongBag();
            for (long key = -40; key < 40; key++) { left.setCount(key, r.nextInt(100)); right.setCount(key, r.nextInt(100)); }
            Map<Long, Long> a = snapshot(left), b = snapshot(right);
            M3LongBag[] results = {left.sum(right), left.union(right), left.intersection(right), left.difference(right)};
            for (int op = 0; op < 4; op++) {
                Map<Long, Long> expected = new HashMap<>();
                for (long key = -40; key < 40; key++) {
                    long x = a.getOrDefault(key, 0L), y = b.getOrDefault(key, 0L);
                    long n = switch (op) { case 0 -> x + y; case 1 -> Math.max(x, y); case 2 -> Math.min(x, y); default -> Math.max(0, x - y); };
                    if (n != 0) expected.put(key, n);
                }
                assertEquals(expected, snapshot(results[op])); results[op].clear();
            }
            assertEquals(a, snapshot(left)); assertEquals(b, snapshot(right));
            assertTrue(left.containsOccurrences(left.copy())); assertTrue(left.difference(left).isEmpty());
            assertEquals(snapshot(left.intersection(right)), snapshot(right.intersection(left)));
        }
    }
    @Test void algebraOverflowDoesNotMutateOperands() {
        M3LongBag a = new M3LongBag(), b = new M3LongBag(); a.add(1, Long.MAX_VALUE); b.add(2);
        assertThrows(ArithmeticException.class, () -> a.sum(b)); assertThrows(ArithmeticException.class, () -> a.union(b));
        assertEquals(Long.MAX_VALUE, a.size()); assertEquals(1, b.size());
        assertTrue(a.intersection(b).isEmpty()); assertFalse(b.containsOccurrences(a));
    }
    @Test void laneImportExportAndIndependentCopies() {
        M3LongBag bag = M3LongBag.fromCounts(new long[]{0, Long.MIN_VALUE, 0, Long.MAX_VALUE}, new long[]{2, 3, 4, 0});
        assertArrayEquals(new long[]{6, 3, 0}, bag.countsFor(new long[]{0, Long.MIN_VALUE, 55}));
        long[] keys = bag.distinctValues(); Arrays.fill(keys, 55); assertEquals(6, bag.count(0));
        M3LongBag copy = bag.copy(); copy.clear(); bag.trimToSize(); assertEquals(9, bag.size());
        assertThrows(IllegalArgumentException.class, () -> M3LongBag.fromCounts(new long[]{1}, new long[0]));
        assertThrows(IllegalArgumentException.class, () -> M3LongBag.fromCounts(new long[]{1}, new long[]{-1}));
        assertThrows(ArithmeticException.class, () -> M3LongBag.fromCounts(new long[]{1, 1}, new long[]{Long.MAX_VALUE, 1}));
    }
    @Test void longOwnersPreserveOrderAndMutationContracts() {
        M3LongLongHashMap map = new M3LongLongHashMap(); Map<Long, Long> m = new HashMap<>();
        M3LongHashSet set = new M3LongHashSet(); Set<Long> s = new HashSet<>();
        M3LongLinkedHashSet linked = new M3LongLinkedHashSet(); Set<Long> l = new LinkedHashSet<>();
        Random r = new Random(119); for (int i = 0; i < 40000; i++) {
            long key = r.nextInt(512), value = r.nextLong();
            if (r.nextBoolean()) {
                assertEquals(!m.containsKey(key), map.put(key, value)); m.put(key, value);
                assertEquals(s.add(key), set.add(key)); assertEquals(l.add(key), linked.add(key));
            } else {
                assertEquals(m.remove(key) != null, map.remove(key)); assertEquals(s.remove(key), set.remove(key));
                assertEquals(l.remove(key), linked.remove(key));
            }
            if ((i & 127) == 0) {
                Map<Long, Long> actual = new HashMap<>(); map.forEach(actual::put); assertEquals(m, actual);
                List<Long> order = new ArrayList<>(); linked.forEachLong(order::add); assertEquals(new ArrayList<>(l), order);
                assertEquals(s.size(), set.size()); assertEquals(m.size(), map.size());
            }
        }
    }
}
