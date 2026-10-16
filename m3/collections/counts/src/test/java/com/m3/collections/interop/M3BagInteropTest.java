/* SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors */
package com.m3.collections.interop;

import static org.junit.jupiter.api.Assertions.*;
import com.google.common.collect.*;
import com.m3.collections.M3LongBag;
import java.util.*;
import java.util.stream.*;
import org.eclipse.collections.impl.bag.mutable.primitive.LongHashBag;
import org.junit.jupiter.api.Test;

final class M3BagInteropTest {
    static void same(Multiset<Long> expected, Multiset<Long> actual) {
        assertEquals(expected, actual); assertEquals(actual, expected); assertEquals(expected.hashCode(), actual.hashCode());
        assertEquals(expected.size(), actual.size()); assertEquals(expected.elementSet(), actual.elementSet());
        assertEquals(expected.entrySet(), actual.entrySet());
    }
    @Test void mutationTracesAgainstActualGuava() {
        for (int seed = 0; seed < 16; seed++) {
            Multiset<Long> expected = HashMultiset.create(), actual = M3LibraryBags.guavaLongMultiset(); Random r = new Random(seed);
            for (int step = 0; step < 2500; step++) {
                Long key = (long) r.nextInt(64); int n = r.nextInt(20);
                switch (r.nextInt(6)) {
                    case 0 -> assertEquals(expected.add(key, n), actual.add(key, n));
                    case 1 -> assertEquals(expected.remove(key, n), actual.remove(key, n));
                    case 2 -> assertEquals(expected.setCount(key, n), actual.setCount(key, n));
                    case 3 -> assertEquals(expected.setCount(key, n, n + 1), actual.setCount(key, n, n + 1));
                    case 4 -> assertEquals(expected.elementSet().remove(key), actual.elementSet().remove(key));
                    default -> assertEquals(expected.entrySet().remove(Multisets.immutableEntry(key, n)), actual.entrySet().remove(Multisets.immutableEntry(key, n)));
                }
                if ((step & 63) == 0) same(expected, actual);
            }
            same(expected, actual);
        }
    }
    @Test void backedViewsIteratorRemovalAndImmutableEntrySnapshots() {
        M3GuavaLongMultiset bag = M3GuavaLongMultiset.create(List.of(1L, 1L, 2L));
        Set<Long> keys = bag.elementSet(); Set<Multiset.Entry<Long>> entries = bag.entrySet(); bag.add(3L, 4);
        assertTrue(keys.contains(3L)); assertTrue(entries.contains(Multisets.immutableEntry(3L, 4)));
        var it = bag.iterator(); assertThrows(IllegalStateException.class, it::remove);
        while (it.hasNext()) { it.next(); it.remove(); assertThrows(IllegalStateException.class, it::remove); }
        assertTrue(keys.isEmpty()); assertTrue(entries.isEmpty()); assertThrows(NoSuchElementException.class, it::next);
        bag.add(5L, 3); var entry = entries.iterator().next(); bag.add(5L); assertEquals(3, entry.getCount());
        assertFalse(entries.remove(entry)); assertEquals(4, bag.count(5L));
        var distinct = keys.iterator(); assertEquals(5L, distinct.next()); distinct.remove(); assertTrue(bag.isEmpty());
        bag.add(7L, 2); var entryIt = entries.iterator(); entryIt.next(); entryIt.remove(); assertTrue(bag.isEmpty());
    }
    @Test void bulkMembershipSelfOperationsAndRemoveIf() {
        for (Multiset<Long> bag : List.of(HashMultiset.<Long>create(), M3GuavaLongMultiset.create())) {
            bag.addAll(List.of(1L, 1L, 2L, 3L)); bag.addAll(bag); assertEquals(4, bag.count(1L));
            assertTrue(bag.removeAll(List.of(1L))); assertEquals(0, bag.count(1L));
            assertTrue(bag.retainAll(List.of(2L))); assertEquals(2, bag.count(2L));
            assertTrue(bag.removeIf(x -> x == 2)); assertTrue(bag.isEmpty());
            bag.add(5L, 8); bag.removeAll(bag); assertTrue(bag.isEmpty());
        }
    }
    @Test void perKeyIntegerLimitSizeSaturationAndInvalidInputs() {
        for (Multiset<Long> bag : List.of(HashMultiset.<Long>create(), M3GuavaLongMultiset.create())) {
            assertEquals(0, bag.add(1L, Integer.MAX_VALUE)); assertEquals(0, bag.add(2L, Integer.MAX_VALUE));
            assertEquals(Integer.MAX_VALUE, bag.size());
            assertThrows(IllegalArgumentException.class, () -> bag.add(1L, 1));
            assertEquals(Integer.MAX_VALUE, bag.count(1L));
            assertThrows(IllegalArgumentException.class, () -> bag.remove("wrong", -1));
            assertThrows(IllegalArgumentException.class, () -> bag.setCount(1L, -1, 0));
            assertEquals(0, bag.count("wrong")); assertEquals(0, bag.count(null));
            assertEquals(Integer.MAX_VALUE, bag.remove(1L, Integer.MAX_VALUE));
        }
        var bag = M3GuavaLongMultiset.create(); assertThrows(NullPointerException.class, () -> bag.add(null));
        assertThrows(NullPointerException.class, () -> bag.setCount(null, 0));
    }
    @Test void failFastAndCopiesCannotMutateOwner() {
        var bag = M3GuavaLongMultiset.create(List.of(1L)); var iterator = bag.iterator(); bag.add(1L);
        assertThrows(ConcurrentModificationException.class, iterator::hasNext);
        M3LongBag copy = bag.toLongBag(); copy.setCount(1, Long.MAX_VALUE); assertEquals(2, bag.count(1L));
        assertThrows(ConcurrentModificationException.class, () -> bag.forEachEntry((key, n) -> bag.add(key)));
    }
    @Test void actualGuavaAlgebraViewsRemainLive() {
        Multiset<Long> a = M3GuavaLongMultiset.create(List.of(1L, 1L, 2L)), b = M3GuavaLongMultiset.create(List.of(1L, 3L));
        var intersection = Multisets.intersection(a, b); var union = Multisets.union(a, b);
        assertEquals(1, intersection.count(1L)); b.add(1L, 2); assertEquals(2, intersection.count(1L));
        assertEquals(3, union.count(1L)); assertEquals(5, Multisets.sum(a, b).count(1L));
        assertEquals(0, Multisets.difference(a, b).count(1L));
    }
    @Test void parallelCollectorAndRealEclipsePrimitiveImport() {
        List<Long> values = LongStream.range(0, 20000).map(i -> i % 113).boxed().toList();
        var actual = values.parallelStream().collect(M3LibraryBags.toGuavaLongMultiset(Long::longValue));
        same(HashMultiset.create(values), actual);
        assertFalse(M3LibraryBags.<Long>toGuavaLongMultiset(Long::longValue).characteristics().contains(Collector.Characteristics.CONCURRENT));
        var eclipse = new LongHashBag(); values.forEach(eclipse::add);
        M3LongBag imported = M3LibraryBags.fromEclipse(eclipse), jdk = M3LibraryBags.fromJdk(values), guava = M3LibraryBags.fromGuava(actual);
        assertEquals(20000, imported.size()); for (long key = 0; key < 113; key++) {
            assertEquals(eclipse.occurrencesOf(key), imported.count(key)); assertEquals(imported.count(key), jdk.count(key));
            assertEquals(imported.count(key), guava.count(key));
        }
        eclipse.clear(); assertEquals(20000, imported.size());
    }
}
