// Modified 2026 by Hitesh Soliwal and Contributors to the Synexia Project: Synexia-to-M3 package/type adaptation.
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified 2026 by Hitesh Soliwal and Contributors to the Synexia Project: late-binding sorted-set spliterator.
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.AbstractSet;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.SortedSet;
import java.util.Spliterator;

/** Key-only AVL storage; backed key views reject insertion as required by Map. */
public final class M3TreeSet<E> extends AbstractSet<E> implements NavigableSet<E> {
    private final M3TreeMap<E, ?> map;
    private final boolean insertable;
    /** Natural ordering. */
    public M3TreeSet() { this(null); }
    /** Comparator ordering. */
    public M3TreeSet(Comparator<? super E> comparator) { this(new M3TreeMap<E, Object>(comparator, true), true); }
    M3TreeSet(M3TreeMap<E, ?> map, boolean insertable) { this.map = map; this.insertable = insertable; }
    @Override public int size() { return map.size(); }
    @Override public boolean isEmpty() { return map.isEmpty(); }
    @Override public boolean contains(Object value) { return map.containsKey(value); }
    @Override public boolean add(E value) {
        if (!insertable) { throw new UnsupportedOperationException("map key view"); }
        if (map.containsKey(value)) { return false; } map.put(value, null); return true;
    }
    @Override public boolean remove(Object value) {
        int s = map.findSlot(value); if (s < 0) { return false; } map.eraseAt(s); return true;
    }
    @Override public void clear() { map.clear(); }
    @Override public Spliterator<E> spliterator() { return M3SlotSpliterators.sorted(this, comparator()); }
    @Override public Iterator<E> iterator() { return map.slots(0); }
    @Override public Iterator<E> descendingIterator() { return map.descendingMap().slots(0); }
    @Override public Comparator<? super E> comparator() { return map.comparator(); }
    @Override public E first() { return map.firstKey(); }
    @Override public E last() { return map.lastKey(); }
    @Override public E lower(E value) { return map.lowerKey(value); }
    @Override public E floor(E value) { return map.floorKey(value); }
    @Override public E ceiling(E value) { return map.ceilingKey(value); }
    @Override public E higher(E value) { return map.higherKey(value); }
    @Override public E pollFirst() { return map.pollKey(false); }
    @Override public E pollLast() { return map.pollKey(true); }
    @Override public M3TreeSet<E> descendingSet() { return new M3TreeSet<>(map.descendingMap(), insertable); }
    @Override public M3TreeSet<E> subSet(E from, boolean fi, E to, boolean ti) { return new M3TreeSet<>(map.subMap(from, fi, to, ti), insertable); }
    @Override public M3TreeSet<E> headSet(E to, boolean inclusive) { return new M3TreeSet<>(map.headMap(to, inclusive), insertable); }
    @Override public M3TreeSet<E> tailSet(E from, boolean inclusive) { return new M3TreeSet<>(map.tailMap(from, inclusive), insertable); }
    @Override public SortedSet<E> subSet(E from, E to) { return subSet(from, true, to, false); }
    @Override public SortedSet<E> headSet(E to) { return headSet(to, false); }
    @Override public SortedSet<E> tailSet(E from) { return tailSet(from, true); }
    /** Validate underlying AVL storage. */
    public void verifyInvariants() { map.verifyInvariants(); }
}
