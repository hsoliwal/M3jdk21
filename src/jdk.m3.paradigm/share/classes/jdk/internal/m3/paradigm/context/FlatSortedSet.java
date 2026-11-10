/* ******************************************************************************
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
package jdk.internal.m3.paradigm.context;

import java.util.AbstractSet;
import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.SortedSet;

/**
 * Sorted array set with live range views and no dummy-value column.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/cop/collections/flat/FlatSortedSet.java}
 * blob {@code c96e85b2cf0769fdbe4ddda66c89a4535bbd2d53} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class FlatSortedSet<E> extends AbstractSet<E> implements SortedSet<E> {
    private final SortedMap<E, Boolean> map;
    public FlatSortedSet(StorageType<E> type) { this(type, null); }
    public FlatSortedSet(StorageType<E> type, Comparator<? super E> comparator) {
        map = new FlatSortedMap<>(type, StorageType.PRESENCE, comparator);
    }
    private FlatSortedSet(SortedMap<E, Boolean> map) { this.map = map; }
    @Override public int size() { return map.size(); }
    @Override public Comparator<? super E> comparator() { return map.comparator(); }
    @Override public E first() { return map.firstKey(); }
    @Override public E last() { return map.lastKey(); }
    @Override public boolean contains(Object value) { return map.containsKey(value); }
    @Override public boolean add(E value) { return map.put(value, Boolean.TRUE) == null; }
    @Override public boolean remove(Object value) { return map.keySet().remove(value); }
    @Override public void clear() { map.clear(); }
    @Override public Iterator<E> iterator() { return map.keySet().iterator(); }
    @Override public SortedSet<E> subSet(E from, E to) { return new FlatSortedSet<>(map.subMap(from, to)); }
    @Override public SortedSet<E> headSet(E to) { return new FlatSortedSet<>(map.headMap(to)); }
    @Override public SortedSet<E> tailSet(E from) { return new FlatSortedSet<>(map.tailMap(from)); }
}
