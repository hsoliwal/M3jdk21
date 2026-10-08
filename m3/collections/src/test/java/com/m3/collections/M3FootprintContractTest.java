// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified 2026 by Hitesh Soliwal and Contributors to the Synexia Project: Synexia-to-M3 package/type adaptation.
/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.ref.Reference;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

/** Boundary regressions also run on the sealed preimage implementations. */
final class M3FootprintContractTest {
    private static final class Key {
        private int hashes;
        private int throwOn = -1;
        @Override public int hashCode() {
            if (++hashes == throwOn) { throw new IllegalArgumentException("hash callback"); }
            return 7;
        }
    }

    @Test void emptyHashLookupsStillEvaluateCallbacksAndFirstInsertRecovers() {
        for (Map<Key, Integer> map : List.of(new M3LinkedHashMap<Key, Integer>(), new M3WeakHashMap<Key, Integer>())) {
            Key key = new Key();
            assertNull(map.get(key)); assertEquals(1, key.hashes);
            assertFalse(map.containsKey(key)); assertEquals(2, key.hashes);
            assertNull(map.remove(key)); assertEquals(3, key.hashes);
            key.throwOn = 4;
            assertThrows(IllegalArgumentException.class, () -> map.get(key));
            key.throwOn = 5;
            assertThrows(IllegalArgumentException.class, () -> map.put(key, 1));
            assertTrue(map.isEmpty());
            key.throwOn = -1; key.hashes = 0;
            assertNull(map.put(key, 2));
            assertEquals(map instanceof M3WeakHashMap<?, ?> ? 2 : 1, key.hashes);
            assertEquals(2, map.get(key)); assertEquals(2, map.remove(key));
            map.clear(); assertNull(map.put(key, 3)); assertEquals(3, map.get(key));
            Reference.reachabilityFence(key);
        }
    }

    @Test void weakSecondHashFailureLeavesAnUsableEmptyOwner() {
        var map = new M3WeakHashMap<Key, Integer>(); Key key = new Key(); key.throwOn = 2;
        assertThrows(IllegalArgumentException.class, () -> map.put(key, 1));
        assertEquals(2, key.hashes); assertTrue(map.isEmpty());
        key.throwOn = -1; map.put(key, 2); assertEquals(2, map.get(key));
        map.clear(); map.expungeStaleKeys(); assertTrue(map.isEmpty());
        Reference.reachabilityFence(key);
    }

    @Test void treeEmptyChecksStillInvokeComparatorAndRejectNull() {
        int[] calls = {0}; boolean[] reject = {false};
        var map = new M3TreeMap<Integer, Integer>((a, b) -> {
            calls[0]++; if (reject[0]) { throw new IllegalArgumentException("comparator"); }
            return Integer.compare(a, b);
        });
        assertNull(map.get(1)); assertEquals(1, calls[0]);
        reject[0] = true;
        assertThrows(IllegalArgumentException.class, () -> map.put(1, 10));
        assertTrue(map.isEmpty()); reject[0] = false;
        map.put(1, 10); assertEquals(10, map.get(1)); map.verifyInvariants();
        var natural = new M3TreeMap<Integer, Integer>();
        assertThrows(NullPointerException.class, () -> natural.get(null));
        assertThrows(NullPointerException.class, () -> natural.put(null, 1));
        assertTrue(natural.isEmpty());
    }

    @Test void mapViewsCreatedWhileEmptyRemainBackedAndFailFast() {
        var hash = new M3LinkedHashMap<Integer, Integer>(); var reverse = hash.reversed();
        var hashKeys = hash.keySet(); var hashIterator = hashKeys.iterator();
        reverse.putFirst(1, 10); reverse.putFirst(2, 20);
        assertEquals(List.of(1, 2), new ArrayList<>(hashKeys));
        assertThrows(ConcurrentModificationException.class, hashIterator::next);
        reverse.clear(); assertTrue(hash.isEmpty()); hash.verifyInvariants();
        reverse.putLast(3, 30); assertEquals(30, hash.get(3)); hash.verifyInvariants();

        var tree = new M3TreeMap<Integer, Integer>();
        var range = tree.subMap(0, true, 10, false).descendingMap();
        var treeKeys = range.navigableKeySet(); var treeIterator = treeKeys.iterator();
        range.put(4, 40); range.put(2, 20);
        assertEquals(List.of(4, 2), new ArrayList<>(treeKeys));
        assertThrows(ConcurrentModificationException.class, treeIterator::next);
        range.clear(); assertTrue(tree.isEmpty()); range.put(5, 50); tree.verifyInvariants();
        assertEquals(50, tree.get(5));
    }

    @Test void listAndSetViewsWorkAcrossFirstInsertionClearAndReuse() {
        var list = new M3LinkedList<Integer>(); var reverse = list.reversed();
        var iterator = list.listIterator(); assertFalse(iterator.hasNext());
        assertNull(reverse.poll()); assertThrows(NoSuchElementException.class, reverse::getFirst);
        reverse.add(null); reverse.add(2);
        assertEquals(java.util.Arrays.asList(2, null), list);
        assertThrows(ConcurrentModificationException.class, iterator::next);
        reverse.clear(); iterator = reverse.listIterator(); iterator.add(9);
        assertEquals(List.of(9), list); list.verifyInvariants();

        var hashSet = new M3LinkedHashSet<Integer>(); var reverseSet = hashSet.reversed();
        var treeSet = new M3TreeSet<Integer>(); var tail = treeSet.tailSet(2, true);
        assertTrue(reverseSet.add(null)); assertFalse(hashSet.add(null));
        assertTrue(tail.add(3)); assertFalse(treeSet.add(3));
        hashSet.clear(); treeSet.clear(); reverseSet.add(1); tail.add(4);
        assertEquals(List.of(1), new ArrayList<>(hashSet));
        assertEquals(List.of(4), new ArrayList<>(treeSet));
        hashSet.verifyInvariants(); treeSet.verifyInvariants();
    }

    @Test void separateEmptyOwnersNeverShareWritableSlots() {
        var first = new M3LinkedHashMap<Integer, Integer>(); var second = new M3LinkedHashMap<Integer, Integer>();
        var firstTree = new M3TreeMap<Integer, Integer>(); var secondTree = new M3TreeMap<Integer, Integer>();
        var firstList = new M3LinkedList<Integer>(); var secondList = new M3LinkedList<Integer>();
        var firstWeak = new M3WeakHashMap<Integer, Integer>(); var secondWeak = new M3WeakHashMap<Integer, Integer>();
        for (int i = 0; i < 64; i++) {
            first.put(i, i); firstTree.put(i, i); firstList.add(i); firstWeak.put(i, i);
        }
        assertTrue(second.isEmpty()); assertTrue(secondTree.isEmpty());
        assertTrue(secondList.isEmpty()); assertTrue(secondWeak.isEmpty());
        second.put(-1, -1); secondTree.put(-1, -1); secondList.add(-1); secondWeak.put(-1, -1);
        assertFalse(first.containsKey(-1)); assertFalse(firstTree.containsKey(-1));
        assertFalse(firstList.contains(-1)); assertFalse(firstWeak.containsKey(-1));
    }

    @Test void representableAvlHeightFitsSignedByteEvenDuringInsertion() {
        long previous = 0, current = 1; int height = 1;
        while (current <= Integer.MAX_VALUE - 8L) {
            long next = 1 + previous + current; previous = current; current = next; height++;
        }
        assertEquals(45, height); assertEquals(1_836_311_902L, previous);
        assertEquals(2_971_215_072L, current); assertTrue(height < Byte.MAX_VALUE);
    }
}
