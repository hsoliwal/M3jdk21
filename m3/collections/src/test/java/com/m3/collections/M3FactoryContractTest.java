// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified 2026 by Hitesh Soliwal and Contributors to the Synexia Project: Synexia-to-M3 package/type adaptation.
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class M3FactoryContractTest {
    private enum Color { RED, GREEN, BLUE }
    private record Ready(int priority) implements Delayed {
        @Override public long getDelay(TimeUnit unit) { return -1; }
        @Override public int compareTo(Delayed other) { return Integer.compare(priority, ((Ready) other).priority); }
    }
    @Test void allFactoryFamiliesAreUsable() throws Exception {
        var list = M3Collections.<Integer>arrayList(); list.addAll(List.of(1, 2)); assertEquals(2, list.size());
        var linked = M3Collections.<Integer>linkedList(); linked.add(3); assertEquals(3, linked.pop());
        var hash = M3Collections.<String, Integer>hashMap(); hash.put("a", 1); assertEquals(1, hash.get("a"));
        assertTrue(M3Collections.linkedHashMap().isEmpty()); assertTrue(M3Collections.linkedHashMap(true).isEmpty());
        assertTrue(M3Collections.treeMap().isEmpty()); assertTrue(M3Collections.treeMap(Comparator.<Integer>reverseOrder()).isEmpty());
        assertTrue(M3Collections.hashSet().add("a")); assertTrue(M3Collections.linkedHashSet().add("a"));
        assertTrue(M3Collections.<Integer>treeSet().add(1)); assertTrue(M3Collections.treeSet(Comparator.<Integer>reverseOrder()).add(1));
        var deque = M3Collections.<Integer>arrayDeque(); deque.addFirst(7); assertEquals(7, deque.removeLast());
        var heap = M3Collections.<Integer>priorityQueue(); heap.addAll(List.of(3, 1, 2)); assertEquals(1, heap.remove());
        var reverseHeap = M3Collections.priorityQueue(Comparator.<Integer>reverseOrder()); reverseHeap.addAll(List.of(1, 2)); assertEquals(2, reverseHeap.remove());
        var identity = M3Collections.<String, Integer>identityMap(); identity.put(new String("a"), 1); identity.put(new String("a"), 2); assertEquals(2, identity.size());
        var weak = M3Collections.weakMap(); weak.put(null, "v"); assertEquals("v", weak.get(null));
        var enums = M3Collections.<Color, Integer>enumMap(Color.class); enums.put(Color.BLUE, 1); assertEquals(1, enums.size());
        var enumSet = M3Collections.enumSet(Color.class); enumSet.add(Color.RED); assertTrue(enumSet.contains(Color.RED));
        var bits = M3Collections.bitSet(); bits.set(1234); assertEquals(1, bits.cardinality());
        var vector = M3Collections.vector(); vector.add("v"); assertEquals("v", vector.get(0));
        var stack = M3Collections.stack(); stack.push("v"); assertEquals("v", stack.pop());
        var cow = M3Collections.copyOnWriteList(); cow.add("v"); var iterator = cow.iterator(); cow.clear(); assertEquals("v", iterator.next());
        assertTrue(M3Collections.copyOnWriteSet().add("v"));
        assertTrue(M3Collections.lockedMap().isEmpty()); assertTrue(M3Collections.lockedHashtableMap().isEmpty());
        assertTrue(M3Collections.lockedTreeMap().isEmpty()); assertTrue(M3Collections.lockedTreeMap(Comparator.<Integer>reverseOrder()).isEmpty());
        assertTrue(M3Collections.lockedHashSet().add(1)); assertTrue(M3Collections.<Integer>lockedTreeSet().add(1));
        assertTrue(M3Collections.lockedTreeSet(Comparator.<Integer>reverseOrder()).add(1));
        assertTrue(M3Collections.lockedQueue().offer(1)); assertTrue(M3Collections.lockedDeque().offerFirst(1));
        assertTrue(M3Collections.blockingQueue(3).offer(1)); assertTrue(M3Collections.blockingDeque(3).offer(1));
        assertTrue(M3Collections.arrayBlockingQueue(1, true).offer(1));
        var blockingHeap = M3Collections.priorityBlockingQueue(Comparator.<Integer>naturalOrder()); blockingHeap.put(3); assertEquals(3, blockingHeap.take());
        var delayed = M3Collections.<Ready>delayQueue(); delayed.put(new Ready(1)); assertEquals(new Ready(1), delayed.take());
        assertTrue(M3Collections.transferQueue().offer(1)); assertFalse(M3Collections.synchronousQueue(true).offer(1));
    }
    @Test void immutableAndJdkUtilityWrappers() {
        var list = M3Collections.immutableList(List.of(1, 2)); assertThrows(UnsupportedOperationException.class, () -> list.add(3));
        var set = M3Collections.immutableSet(List.of(1, 1, 2)); assertEquals(2, set.size()); assertThrows(UnsupportedOperationException.class, () -> set.remove(1));
        var map = M3Collections.immutableMap(Map.of("a", 1)); assertThrows(UnsupportedOperationException.class, () -> map.put("a", 2));
        assertThrows(UnsupportedOperationException.class, () -> map.entrySet().iterator().next().setValue(3));
        assertThrows(NullPointerException.class, () -> M3Collections.immutableSet(Collections.singleton(null)));
        assertThrows(NullPointerException.class, () -> M3Collections.immutableMap(Collections.singletonMap("a", null)));
        var packed = new M3TreeMap<Integer, Integer>(); packed.put(1, 1);
        var checked = Collections.checkedNavigableMap(packed, Integer.class, Integer.class); assertEquals(1, checked.get(1));
        var synchronizedMap = Collections.synchronizedNavigableMap(packed); synchronizedMap.put(2, 2); assertEquals(2, packed.size());
        var unmodifiable = Collections.unmodifiableNavigableMap(packed); assertThrows(UnsupportedOperationException.class, unmodifiable::pollFirstEntry);
    }
}
