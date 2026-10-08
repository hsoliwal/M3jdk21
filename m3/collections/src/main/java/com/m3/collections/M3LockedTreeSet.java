// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified 2026 by Hitesh Soliwal and Contributors to the Synexia Project: Synexia-to-M3 package/type adaptation.
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.AbstractSet;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.Spliterator;

/** Lock-based concurrent sorted set; bounded/descending views retain the same lock. */
public final class M3LockedTreeSet<E> extends AbstractSet<E> implements NavigableSet<E> {
    private final M3LockedTreeMap<E, ?> map;
    private final boolean insertable;
    /** Natural ordering. */
    public M3LockedTreeSet() { this(null); }
    /** Comparator ordering, with nulls rejected. */
    public M3LockedTreeSet(Comparator<? super E> comparator) { this(new M3LockedTreeMap<E, Boolean>(comparator), true); }
    M3LockedTreeSet(M3LockedTreeMap<E, ?> map, boolean insertable) { this.map = map; this.insertable = insertable; }
    @Override public int size() { return map.size(); }
    @Override public boolean isEmpty() { return map.isEmpty(); }
    @Override public boolean contains(Object value) { return map.containsKey(value); }
    @Override @SuppressWarnings("unchecked") public boolean add(E value) {
        if (!insertable) { throw new UnsupportedOperationException("map key view"); }
        return ((M3LockedTreeMap<E, Boolean>) map).putIfAbsent(value, Boolean.TRUE) == null;
    }
    @Override public boolean remove(Object value) { return map.remove(value) != null; }
    @Override public void clear() { map.clear(); }
    @Override public Spliterator<E> spliterator() { return M3SlotSpliterators.sorted(iterator(), size(), comparator()); }
    @Override public Iterator<E> iterator() { return map.iterator(0); }
    @Override public Iterator<E> descendingIterator() { return descendingSet().iterator(); }
    @Override public Comparator<? super E> comparator() { return map.comparator(); }
    @Override public E first() { return map.firstKey(); }
    @Override public E last() { return map.lastKey(); }
    @Override public E lower(E value) { return map.lowerKey(value); }
    @Override public E floor(E value) { return map.floorKey(value); }
    @Override public E ceiling(E value) { return map.ceilingKey(value); }
    @Override public E higher(E value) { return map.higherKey(value); }
    @Override public E pollFirst() { return map.pollKey(false); }
    @Override public E pollLast() { return map.pollKey(true); }
    @Override public M3LockedTreeSet<E> descendingSet() { return new M3LockedTreeSet<>(map.descendingMap(), insertable); }
    @Override public M3LockedTreeSet<E> subSet(E from, boolean fi, E to, boolean ti) { return new M3LockedTreeSet<>(map.subMap(from, fi, to, ti), insertable); }
    @Override public M3LockedTreeSet<E> headSet(E to, boolean inclusive) { return new M3LockedTreeSet<>(map.headMap(to, inclusive), insertable); }
    @Override public M3LockedTreeSet<E> tailSet(E from, boolean inclusive) { return new M3LockedTreeSet<>(map.tailMap(from, inclusive), insertable); }
    @Override public M3LockedTreeSet<E> subSet(E from, E to) { return subSet(from, true, to, false); }
    @Override public M3LockedTreeSet<E> headSet(E to) { return headSet(to, false); }
    @Override public M3LockedTreeSet<E> tailSet(E from) { return tailSet(from, true); }
}
