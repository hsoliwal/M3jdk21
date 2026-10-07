// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified 2026 by Hitesh Soliwal and Contributors to the Synexia Project: Synexia-to-M3 package/type adaptation.
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

class M3SequenceContractTest {
    @Test void linkedListAndDequeDifferential() {
        var a = new M3LinkedList<Integer>(); var b = new LinkedList<Integer>(); Random random = new Random(91);
        for (int step = 0; step < 30000; step++) {
            Integer value = random.nextInt(10) == 0 ? null : random.nextInt(100);
            int op = random.nextInt(12), index = b.isEmpty() ? 0 : random.nextInt(b.size());
            switch (op) {
                case 0 -> { a.addFirst(value); b.addFirst(value); }
                case 1 -> { a.addLast(value); b.addLast(value); }
                case 2 -> assertEquals(b.pollFirst(), a.pollFirst());
                case 3 -> assertEquals(b.pollLast(), a.pollLast());
                case 4 -> assertEquals(b.removeFirstOccurrence(value), a.removeFirstOccurrence(value));
                case 5 -> assertEquals(b.removeLastOccurrence(value), a.removeLastOccurrence(value));
                case 6 -> { a.add(index, value); b.add(index, value); }
                case 7 -> { if (!b.isEmpty()) { assertEquals(b.set(index, value), a.set(index, value)); } }
                case 8 -> { if (!b.isEmpty()) { assertEquals(b.remove(index), a.remove(index)); } }
                case 9 -> { a.reversed().addFirst(value); b.reversed().addFirst(value); }
                case 10 -> { a.reversed().add(index, value); b.reversed().add(index, value); }
                default -> {
                    if (b.size() > 30) { a.subList(5, 20).clear(); b.subList(5, 20).clear(); }
                }
            }
            assertEquals(b, a); assertEquals(b.reversed(), a.reversed()); a.verifyInvariants();
        }
    }
    @Test void listIteratorAlternatingMutations() {
        var a = new M3LinkedList<Integer>(); var b = new LinkedList<Integer>();
        for (int i = 0; i < 30; i++) { a.add(i); b.add(i); }
        ListIterator<Integer> ai = a.listIterator(15), bi = b.listIterator(15); Random r = new Random(419);
        for (int step = 0; step < 10000; step++) {
            int action = r.nextInt(5);
            switch (action) {
                case 0 -> { if (bi.hasNext()) { assertEquals(bi.next(), ai.next()); } }
                case 1 -> { if (bi.hasPrevious()) { assertEquals(bi.previous(), ai.previous()); } }
                case 2 -> { ai.add(step); bi.add(step); }
                case 3 -> {
                    try { bi.remove(); } catch (IllegalStateException e) { assertThrows(IllegalStateException.class, ai::remove); continue; }
                    ai.remove();
                }
                default -> {
                    try { bi.set(step); } catch (IllegalStateException e) { int v = step; assertThrows(IllegalStateException.class, () -> ai.set(v)); continue; }
                    ai.set(step);
                }
            }
            assertEquals(bi.nextIndex(), ai.nextIndex()); assertEquals(b, a); a.verifyInvariants();
        }
    }
    @Test void sublistAndReverseShareStructuralVersion() {
        var list = new M3LinkedList<Integer>(); list.addAll(List.of(1, 2, 3, 4));
        var sub = list.subList(1, 3); var iterator = list.iterator(); list.reversed().addFirst(9);
        assertThrows(ConcurrentModificationException.class, iterator::next);
        assertThrows(ConcurrentModificationException.class, sub::size);
        assertTrue(list.reversed().reversed() == list);
        list.clear(); list.add(null); assertEquals(1, list.size()); assertEquals(null, list.removeFirst());
        assertThrows(java.util.NoSuchElementException.class, list::removeFirst);
    }
    @Test void orderedSetSequenceOperations() {
        var a = new M3LinkedHashSet<Integer>(); var b = new LinkedHashSet<Integer>(); Random r = new Random(434);
        for (int step = 0; step < 15000; step++) {
            Integer k = step % 91 == 0 ? null : r.nextInt(200);
            var av = step % 2 == 0 ? a : a.reversed(); var bv = step % 2 == 0 ? b : b.reversed();
            switch (r.nextInt(6)) {
                case 0 -> assertEquals(bv.add(k), av.add(k));
                case 1 -> { av.addFirst(k); bv.addFirst(k); }
                case 2 -> { av.addLast(k); bv.addLast(k); }
                case 3 -> assertEquals(bv.remove(k), av.remove(k));
                case 4 -> { if (!bv.isEmpty()) { assertEquals(bv.removeFirst(), av.removeFirst()); } }
                default -> { if (!bv.isEmpty()) { assertEquals(bv.removeLast(), av.removeLast()); } }
            }
            assertEquals(new ArrayList<>(bv), new ArrayList<>(av)); assertEquals(b, a); a.verifyInvariants();
        }
    }
    @Test void streamsRetainEncounterOrderAndSortedComparator() {
        var set = new M3LinkedHashSet<Integer>(); var list = new M3LinkedList<Integer>();
        for (int i = 999; i >= 0; i--) { set.add(i); list.add(i); }
        assertEquals(new ArrayList<>(set), set.parallelStream().toList());
        assertEquals(new ArrayList<>(list), list.parallelStream().toList());
        assertTrue(set.spliterator().hasCharacteristics(java.util.Spliterator.ORDERED));
        var tree = new M3TreeSet<Integer>(Comparator.reverseOrder()); tree.addAll(set);
        assertTrue(tree.spliterator().hasCharacteristics(java.util.Spliterator.SORTED));
        assertEquals(Comparator.reverseOrder(), tree.spliterator().getComparator());
        assertEquals(new ArrayList<>(tree), tree.parallelStream().toList());
    }
    @Test void treeSetViewsAndPolls() {
        Comparator<Integer> cmp = Comparator.nullsLast(Comparator.naturalOrder());
        var a = new M3TreeSet<>(cmp); var b = new TreeSet<>(cmp);
        for (Integer i : new Integer[] {1, 2, 3, 4, 5, null}) { assertEquals(b.add(i), a.add(i)); }
        assertEquals(new ArrayList<>(b), new ArrayList<>(a));
        a.subSet(2, false, 5, true).descendingSet().remove(4); b.subSet(2, false, 5, true).descendingSet().remove(4);
        assertEquals(b, a); a.headSet(3, true).add(0); b.headSet(3, true).add(0);
        assertThrows(IllegalArgumentException.class, () -> a.headSet(3, true).add(4));
        while (!b.isEmpty()) { assertEquals(b.pollLast(), a.pollLast()); a.verifyInvariants(); }
        assertFalse(a.add(8) && a.add(8)); assertEquals(1, a.size());
        var map = new M3TreeMap<Integer, Integer>(); map.put(1, 1);
        assertThrows(UnsupportedOperationException.class, () -> map.navigableKeySet().add(2));
    }
}
