/* *****************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.internal.m3.collections;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.SortedSet;
import java.util.Spliterator;

/**
 * Navigable set using the map engine's key-only storage: one Object[] lane.
 * No dummy value array or per-element node. All range views remain backed.
 * Standalone set views permit add; a map key-set view never permits add.
 *
 * @param <E> element type
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/DenseArraySortedSet.java}
 * blob {@code ebcdaafaeba0f7ba3dac69d34a5ee95446d23262} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (common collections batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class DenseArraySortedSet<E> extends AbstractSet<E> implements NavigableSet<E> {
    private final DenseArraySortedMap<E, ?> map;
    private final boolean insertionAllowed;

    public DenseArraySortedSet() {
        this((Comparator<? super E>) null);
    }

    public DenseArraySortedSet(Comparator<? super E> comparator) {
        this(DenseArraySortedMap.keyOnly(comparator), true);
    }

    DenseArraySortedSet(DenseArraySortedMap<E, ?> map, boolean insertionAllowed) {
        this.map = map;
        this.insertionAllowed = insertionAllowed;
    }

    @Override public int size() { return map.size(); }
    @Override public boolean contains(Object key) { return map.containsKey(key); }
    @Override public boolean remove(Object key) { return map.removeKey(key); }
    @Override public void clear() { map.clear(); }
    @Override public Comparator<? super E> comparator() { return map.comparator(); }
    @Override public Iterator<E> iterator() { return map.keyIterator(); }
    /** Late-bound, ordered, directly split index ranges; no element-buffer copy. */
    @Override public Spliterator<E> spliterator() { return map.keySpliterator(); }
    @Override public Iterator<E> descendingIterator() { return descendingSet().iterator(); }

    @Override public boolean add(E key) {
        if (!insertionAllowed) { throw new UnsupportedOperationException("map key-set insertion"); }
        return map.addKey(key);
    }

    /** Atomic sort-once/merge-once batch; ordinary inherited addAll is sequential. */
    public boolean addAllAtomic(Collection<? extends E> source) {
        if (!insertionAllowed) { throw new UnsupportedOperationException("map key-set insertion"); }
        return map.addKeysAtomic(source);
    }

    @Override public E lower(E key) { return map.lowerKey(key); }
    @Override public E floor(E key) { return map.floorKey(key); }
    @Override public E ceiling(E key) { return map.ceilingKey(key); }
    @Override public E higher(E key) { return map.higherKey(key); }
    @Override public E first() { return map.firstKey(); }
    @Override public E last() { return map.lastKey(); }

    @Override public E pollFirst() { return map.pollKey(true); }
    @Override public E pollLast() { return map.pollKey(false); }

    @Override public DenseArraySortedSet<E> descendingSet() {
        return new DenseArraySortedSet<>(map.descendingMap(), insertionAllowed);
    }

    @Override public DenseArraySortedSet<E> subSet(E from, boolean fromInclusive,
            E to, boolean toInclusive) {
        return new DenseArraySortedSet<>(map.subMap(from, fromInclusive, to, toInclusive),
                insertionAllowed);
    }

    @Override public DenseArraySortedSet<E> headSet(E to, boolean inclusive) {
        return new DenseArraySortedSet<>(map.headMap(to, inclusive), insertionAllowed);
    }

    @Override public DenseArraySortedSet<E> tailSet(E from, boolean inclusive) {
        return new DenseArraySortedSet<>(map.tailMap(from, inclusive), insertionAllowed);
    }

    @Override public SortedSet<E> subSet(E from, E to) { return subSet(from, true, to, false); }
    @Override public SortedSet<E> headSet(E to) { return headSet(to, false); }
    @Override public SortedSet<E> tailSet(E from) { return tailSet(from, true); }
}
