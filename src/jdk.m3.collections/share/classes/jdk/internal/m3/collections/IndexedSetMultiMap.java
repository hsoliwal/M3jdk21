/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package jdk.internal.m3.collections;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import jdk.m3.collections.M3ArrayList;
import jdk.m3.collections.IndexMaskCompactor;
import jdk.m3.collections.IndexedListSnapshot;
import jdk.m3.collections.M3LinkedHashMap;
import jdk.m3.collections.M3LinkedHashSet;

/**
 * Native M3 set-valued relation over existing M3 indexed hash-map and set
 * storage: one M3 set per admitted key, no retained mapping Entry/Node owner.
 * Value duplicates are suppressed per key; total size counts pairs, not keys.
 * No Guava/Eclipse/Apache runtime or adapter is required. Snapshots use
 * IndexedListSnapshot's reusable physical-index filtering/sorting/JNI atoms.
 */
public final class IndexedSetMultiMap<K, V> {
    private final M3LinkedHashMap<K, M3LinkedHashSet<V>> groups = new M3LinkedHashMap<>();
    private long pairCount;
    private long revision;

    public IndexedSetMultiMap() { }
    public long size() { return pairCount; }
    public int keyCount() { return groups.size(); }
    public long revision() { return revision; }
    public boolean containsKey(Object key) { return groups.containsKey(key); }
    public boolean containsEntry(Object key, Object value) {
        Set<V> values = groups.get(key);
        return values != null && values.contains(value);
    }

    public boolean put(K key, V value) {
        M3LinkedHashSet<V> values = groups.get(key);
        boolean created = values == null;
        if (created) values = new M3LinkedHashSet<>();
        boolean added = values.add(value);
        if (!added) return false;
        if (created) groups.put(key, values);
        pairCount++;
        revision++;
        return true;
    }

    public boolean remove(Object key, Object value) {
        M3LinkedHashSet<V> values = groups.get(key);
        if (values == null || !values.remove(value)) return false;
        pairCount--;
        if (values.isEmpty()) groups.remove(key);
        revision++;
        return true;
    }

    /** A detached M3 indexed snapshot; no live vendor value-collection contract. */
    public IndexedListSnapshot<V> snapshot(Object key) {
        Set<V> values = groups.get(key);
        return values == null ? IndexedListSnapshot.<V>copyOf(new M3ArrayList<>())
                : IndexedListSnapshot.copyOf(values);
    }

    /** Filter once per admitted occurrence, then reuse immutable physical indexes. */
    public IndexedListSnapshot<V> snapshotFiltered(
            Object key, Predicate<? super V> predicate,
            IndexMaskCompactor.Backend backend) {
        return snapshot(key).filtered(Objects.requireNonNull(predicate, "predicate"), backend);
    }

    /** Live, M3-owned view; modifications preserve pair-count and key deletion. */
    public Set<V> get(K key) {
        return new AbstractSet<>() {
            @Override public int size() {
                Set<V> set = groups.get(key);
                return set == null ? 0 : set.size();
            }
            @Override public boolean contains(Object value) { return containsEntry(key, value); }
            @Override public boolean add(V value) { return put(key, value); }
            @Override public boolean remove(Object value) { return IndexedSetMultiMap.this.remove(key, value); }
            @Override public void clear() { IndexedSetMultiMap.this.removeAll(key); }
            @Override public Iterator<V> iterator() {
                IndexedListSnapshot<V> snapshot = snapshot(key);
                Iterator<V> selected = snapshot.iterator();
                return new Iterator<>() {
                    private V last;
                    private boolean removable;
                    @Override public boolean hasNext() { return selected.hasNext(); }
                    @Override public V next() {
                        last = selected.next();
                        removable = true;
                        return last;
                    }
                    @Override public void remove() {
                        if (!removable) throw new IllegalStateException();
                        IndexedSetMultiMap.this.remove(key, last);
                        removable = false;
                    }
                };
            }
        };
    }

    public IndexedListSnapshot<V> removeAll(Object key) {
        M3LinkedHashSet<V> removed = groups.remove(key);
        if (removed == null) return IndexedListSnapshot.copyOf(new M3ArrayList<>());
        IndexedListSnapshot<V> snapshot = IndexedListSnapshot.copyOf(removed);
        pairCount -= removed.size();
        revision++;
        return snapshot;
    }

    public void clear() {
        if (groups.isEmpty()) return;
        groups.clear();
        pairCount = 0;
        revision++;
    }
}
