/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections.interop;

import com.m3.collections.M3LinkedHashMap;
import com.m3.collections.M3LinkedHashSet;
import com.m3.collections.M3LinkedList;
import java.util.List;
import java.util.Collection;
import java.util.AbstractMap;
import org.apache.commons.collections4.MapIterator;
import org.apache.commons.collections4.iterators.UnmodifiableMapIterator;
import com.google.common.collect.Collections2;
import java.util.Collections;
import com.google.common.collect.Maps;
import java.util.Map;
import java.util.Set;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.ListValuedMap;
import org.apache.commons.collections4.MultiSet;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.SetValuedMap;
import org.apache.commons.collections4.bidimap.AbstractDualBidiMap;
import org.apache.commons.collections4.bidimap.UnmodifiableBidiMap;
import org.apache.commons.collections4.multimap.AbstractListValuedMap;
import org.apache.commons.collections4.multimap.AbstractMultiValuedMapDecorator;
import org.apache.commons.collections4.multimap.AbstractSetValuedMap;
import org.apache.commons.collections4.multiset.SynchronizedMultiSet;
import org.apache.commons.collections4.multiset.UnmodifiableMultiSet;

/**
 * Optional Apache Commons Collections adapters. Provider abstractions retain their
 * backed-view and mutation algorithms; only their owned stores use existing M3 lanes.
 * Distinct keys, sets and per-key lists retain insertion order. Nulls are accepted.
 * These are interface implementations, not provider concrete-class/serialization
 * substitutes. Ordinary results are not thread-safe. Read-only wrappers remain live.
 * Provider boundary iteration may construct Entry projections; no retained structural
 * Entry store is introduced. No global memory-pressure or OS-interning claim is made.
 */
public final class M3ApacheCollections {
    private M3ApacheCollections() { }
    /** Apache 4.4 can retain an empty key after get(absent).add(invalidIndex, value). */
    public static <K,V> ListValuedMap<K,V> listValuedMap() { return new ListValues<>(); }
    public static <K,V> SetValuedMap<K,V> setValuedMap() { return new SetValues<>(); }
    /**
     * Duplicate values evict their previous key. Apache 4.4 entry.setValue returns
     * the new value (a provider deviation from Map.Entry); this adapter preserves
     * that observed profile, not strict JDK entry old-value-return compatibility.
     */
    public static <K,V> BidiMap<K,V> bidiMap() { return new Bidirectional<>(); }
    public static <E> MultiSet<E> multiSet() { return new M3ApacheMultiSet<>(); }
    /**
     * Live, container-deep read-only view. Unlike the Apache 4.4 wrapper, asMap's
     * nested value collections cannot mutate the source. Elements themselves are
     * not made immutable. This is an explicit defect correction, not preservation
     * of the donor's mutation escape. Serialization is not a supported contract.
     */
    public static <K,V> MultiValuedMap<K,V> unmodifiableMultiValuedMap(MultiValuedMap<K,V> source) {
        if (source instanceof ReadOnlyMultiValues<?,?>) { return source; }
        return new ReadOnlyMultiValues<>(source);
    }
    public static <K,V> BidiMap<K,V> unmodifiableBidiMap(BidiMap<K,V> source) {
        return UnmodifiableBidiMap.unmodifiableBidiMap(source);
    }
    public static <E> MultiSet<E> unmodifiableMultiSet(MultiSet<E> source) {
        return UnmodifiableMultiSet.unmodifiableMultiSet(source);
    }
    /**
     * Uses the provider's shared monitor, including its removal views. All access
     * must go through this wrapper. Hold synchronized(result) for iteration and
     * multi-call compound operations; exposed aliases are not made thread-safe.
     */
    public static <E> MultiSet<E> synchronizedMultiSet(MultiSet<E> source) {
        return SynchronizedMultiSet.synchronizedMultiSet(source);
    }
    private static <V> Collection<V> readOnlyValues(Collection<V> values) {
        if (values instanceof List<V> list) { return Collections.unmodifiableList(list); }
        if (values instanceof Set<V> set) { return Collections.unmodifiableSet(set); }
        return Collections.unmodifiableCollection(values);
    }
    private static final class ReadOnlyMultiValues<K,V> extends AbstractMultiValuedMapDecorator<K,V> {
        private static final long serialVersionUID = 1L;
        private ReadOnlyMultiValues(MultiValuedMap<K,V> source) { super(source); }
        @Override public boolean put(K key, V value) { throw new UnsupportedOperationException(); }
        @Override public boolean putAll(K key, Iterable<? extends V> values) { throw new UnsupportedOperationException(); }
        @Override public boolean putAll(Map<? extends K,? extends V> values) { throw new UnsupportedOperationException(); }
        @Override public boolean putAll(MultiValuedMap<? extends K,? extends V> values) { throw new UnsupportedOperationException(); }
        @Override public Collection<V> remove(Object key) { throw new UnsupportedOperationException(); }
        @Override public boolean removeMapping(Object key, Object value) { throw new UnsupportedOperationException(); }
        @Override public void clear() { throw new UnsupportedOperationException(); }
        @Override public Collection<V> get(K key) { return readOnlyValues(super.get(key)); }
        @Override public Set<K> keySet() { return Collections.unmodifiableSet(super.keySet()); }
        @Override public MultiSet<K> keys() { return UnmodifiableMultiSet.unmodifiableMultiSet(super.keys()); }
        @Override public Collection<V> values() { return Collections.unmodifiableCollection(super.values()); }
        @Override public Collection<Map.Entry<K,V>> entries() {
            return Collections.unmodifiableCollection(Collections2.transform(super.entries(), AbstractMap.SimpleImmutableEntry::new));
        }
        @Override public MapIterator<K,V> mapIterator() { return UnmodifiableMapIterator.unmodifiableMapIterator(super.mapIterator()); }
        @Override public Map<K,Collection<V>> asMap() {
            return Collections.unmodifiableMap(Maps.transformValues(super.asMap(), M3ApacheCollections::readOnlyValues));
        }
    }
    private static final class ListValues<K,V> extends AbstractListValuedMap<K,V> {
        private ListValues() { super(new M3LinkedHashMap<>()); }
        @Override protected List<V> createCollection() { return new M3LinkedList<>(); }
    }
    private static final class SetValues<K,V> extends AbstractSetValuedMap<K,V> {
        private SetValues() { super(new M3LinkedHashMap<>()); }
        @Override protected Set<V> createCollection() { return new M3LinkedHashSet<>(); }
    }
    private static final class Bidirectional<K,V> extends AbstractDualBidiMap<K,V> {
        private Bidirectional() { super(new M3LinkedHashMap<>(), new M3LinkedHashMap<>()); }
        private Bidirectional(Map<K,V> normal, Map<V,K> reverse, BidiMap<V,K> inverse) {
            super(normal, reverse, inverse);
        }
        @Override protected BidiMap<V,K> createBidiMap(Map<V,K> normal, Map<K,V> reverse, BidiMap<K,V> inverse) {
            return new Bidirectional<>(normal, reverse, inverse);
        }
    }
}
