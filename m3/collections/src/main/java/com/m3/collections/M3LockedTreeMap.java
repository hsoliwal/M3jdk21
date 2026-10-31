// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified 2026 by Hitesh Soliwal and Contributors to the Synexia Project: Synexia-to-M3 package/type adaptation.
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.Comparator;
import java.util.NavigableSet;
import java.util.concurrent.ConcurrentNavigableMap;

/** ConcurrentNavigableMap over the array AVL tree; all backed views share one lock. */
public final class M3LockedTreeMap<K, V> extends M3LockedMap<K, V> implements ConcurrentNavigableMap<K, V> {
    private final M3TreeMap<K, V> tree;
    /** Natural ordering, no null keys or values. */
    public M3LockedTreeMap() { this(null); }
    /** Comparator ordering; concurrency policy still rejects null keys. */
    public M3LockedTreeMap(Comparator<? super K> comparator) { this(new M3TreeMap<>(comparator), new Object()); }
    private M3LockedTreeMap(M3TreeMap<K, V> tree, Object mutex) { super(tree, mutex); this.tree = tree; }
    @Override public Comparator<? super K> comparator() { return tree.comparator(); }
    @Override public K firstKey() { synchronized (mutex) { return tree.firstKey(); } }
    @Override public K lastKey() { synchronized (mutex) { return tree.lastKey(); } }
    @Override public Entry<K, V> firstEntry() { synchronized (mutex) { return tree.firstEntry(); } }
    @Override public Entry<K, V> lastEntry() { synchronized (mutex) { return tree.lastEntry(); } }
    @Override public Entry<K, V> pollFirstEntry() { synchronized (mutex) { return tree.pollFirstEntry(); } }
    @Override public Entry<K, V> pollLastEntry() { synchronized (mutex) { return tree.pollLastEntry(); } }
    @Override public K lowerKey(K k) { synchronized (mutex) { return tree.lowerKey(nonnull(k)); } }
    @Override public K floorKey(K k) { synchronized (mutex) { return tree.floorKey(nonnull(k)); } }
    @Override public K ceilingKey(K k) { synchronized (mutex) { return tree.ceilingKey(nonnull(k)); } }
    @Override public K higherKey(K k) { synchronized (mutex) { return tree.higherKey(nonnull(k)); } }
    @Override public Entry<K, V> lowerEntry(K k) { synchronized (mutex) { return tree.lowerEntry(nonnull(k)); } }
    @Override public Entry<K, V> floorEntry(K k) { synchronized (mutex) { return tree.floorEntry(nonnull(k)); } }
    @Override public Entry<K, V> ceilingEntry(K k) { synchronized (mutex) { return tree.ceilingEntry(nonnull(k)); } }
    @Override public Entry<K, V> higherEntry(K k) { synchronized (mutex) { return tree.higherEntry(nonnull(k)); } }
    @Override public M3LockedTreeMap<K, V> descendingMap() { synchronized (mutex) { return new M3LockedTreeMap<>(tree.descendingMap(), mutex); } }
    @Override public M3LockedTreeMap<K, V> subMap(K from, boolean fi, K to, boolean ti) {
        synchronized (mutex) { return new M3LockedTreeMap<>(tree.subMap(nonnull(from), fi, nonnull(to), ti), mutex); }
    }
    @Override public M3LockedTreeMap<K, V> headMap(K to, boolean inclusive) {
        synchronized (mutex) { return new M3LockedTreeMap<>(tree.headMap(nonnull(to), inclusive), mutex); }
    }
    @Override public M3LockedTreeMap<K, V> tailMap(K from, boolean inclusive) {
        synchronized (mutex) { return new M3LockedTreeMap<>(tree.tailMap(nonnull(from), inclusive), mutex); }
    }
    @Override public M3LockedTreeMap<K, V> subMap(K from, K to) { return subMap(from, true, to, false); }
    @Override public M3LockedTreeMap<K, V> headMap(K to) { return headMap(to, false); }
    @Override public M3LockedTreeMap<K, V> tailMap(K from) { return tailMap(from, true); }
    @Override public NavigableSet<K> keySet() { return navigableKeySet(); }
    @Override public NavigableSet<K> navigableKeySet() { return new M3LockedTreeSet<>(this, false); }
    @Override public NavigableSet<K> descendingKeySet() { return descendingMap().navigableKeySet(); }
    K pollKey(boolean last) { synchronized (mutex) { return tree.pollKey(last); } }
}
