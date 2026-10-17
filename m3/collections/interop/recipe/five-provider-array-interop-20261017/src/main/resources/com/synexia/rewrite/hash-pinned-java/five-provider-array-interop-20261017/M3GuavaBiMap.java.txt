/*
 * Copyright (C) 2007 The Guava Authors
 * Copyright 2026 Hitesh Soliwal and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * https://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * SPDX-License-Identifier: Apache-2.0
 *
 * Modified 2026: adapted AbstractBiMap.java from
 * google/guava@f06690fa3e874f65515e8fd338a74d636e2c792f (v33.4.8), Git blob
 * 26a2df528ed8957faa5417195810e9fa9c68d2c4. Existing M3 array-slot maps own both
 * directions. Local iterator projections replace package-private Guava helpers;
 * serialization is excluded. replaceAll validates a candidate before applying it,
 * including null-key conflicts, and preserves the original owners and backed views.
 */
package com.m3.collections.interop;

import com.google.common.collect.BiMap;
import com.google.common.collect.ForwardingMap;
import com.google.common.collect.ForwardingMapEntry;
import com.google.common.collect.ForwardingSet;
import com.m3.collections.M3LinkedHashMap;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Guava's mutable bidirectional map contract over existing M3 array-slot owners.
 *
 * <p>Null keys and values are supported. Values are unique. {@code put} rejects an
 * occupied value, while {@code forcePut} removes its former key. The inverse and
 * all collection views write through the same two owners. Each direction preserves
 * its own key insertion order; values iterate in the associated forward-key order.
 * No stored hash node or per-entry payload object is introduced. Entries and views
 * may allocate on API access. This collection is not thread-safe or serializable.
 *
 * <p>{@code replaceAll} first computes and validates the replacement using M3 maps.
 * If the function throws or produces a duplicate value, existing mappings remain
 * unchanged unless the callback itself mutates them. Such callback mutations remain
 * observable and structural changes are detected by the underlying M3 iterator.
 */
public final class M3GuavaBiMap<K, V> extends ForwardingMap<K, V> implements BiMap<K, V> {
    private final M3LinkedHashMap<K, V> forward;
    private final M3GuavaBiMap<V, K> inverse;
    private Set<K> keys;
    private Set<V> values;
    private Set<Entry<K, V>> entries;

    /** Creates independent forward and inverse M3 owners. */
    public M3GuavaBiMap() {
        forward = new M3LinkedHashMap<>();
        inverse = new M3GuavaBiMap<>(new M3LinkedHashMap<>(), this);
    }

    private M3GuavaBiMap(M3LinkedHashMap<K, V> forward, M3GuavaBiMap<V, K> inverse) {
        this.forward = forward;
        this.inverse = inverse;
    }

    @Override protected Map<K, V> delegate() { return forward; }
    @Override public boolean containsValue(Object value) { return inverse.containsKey(value); }
    @Override public V put(K key, V value) { return putInBothMaps(key, value, false); }
    @Override public V forcePut(K key, V value) { return putInBothMaps(key, value, true); }

    private V putInBothMaps(K key, V value, boolean force) {
        boolean containedKey = containsKey(key);
        if (containedKey && Objects.equals(value, get(key))) return value;
        if (force) inverse.remove(value);
        else if (containsValue(value)) throw new IllegalArgumentException("value already present: " + value);
        V previous = forward.put(key, value);
        updateInverse(key, containedKey, previous, value);
        return previous;
    }

    private void updateInverse(K key, boolean containedKey, V previous, V value) {
        if (containedKey) inverse.forward.remove(previous);
        inverse.forward.put(value, key);
    }

    @Override public V remove(Object key) {
        return containsKey(key) ? removeFromBothMaps(key) : null;
    }

    private V removeFromBothMaps(Object key) {
        V previous = forward.remove(key);
        inverse.forward.remove(previous);
        return previous;
    }

    @Override public void putAll(Map<? extends K, ? extends V> map) {
        for (Entry<? extends K, ? extends V> entry : map.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        Objects.requireNonNull(function, "function");
        M3GuavaBiMap<K, V> replacement = new M3GuavaBiMap<>();
        forward.forEach((key, value) -> replacement.put(key, function.apply(key, value)));
        clear();
        putAll(replacement);
    }

    @Override public void clear() { forward.clear(); inverse.forward.clear(); }
    @Override public M3GuavaBiMap<V, K> inverse() { return inverse; }

    @Override public Set<K> keySet() {
        if (keys == null) keys = new KeySet();
        return keys;
    }

    private final class KeySet extends ForwardingSet<K> {
        @Override protected Set<K> delegate() { return forward.keySet(); }
        @Override public void clear() { M3GuavaBiMap.this.clear(); }
        @Override public boolean remove(Object key) {
            if (!contains(key)) return false;
            removeFromBothMaps(key);
            return true;
        }
        @Override public boolean removeAll(Collection<?> collection) { return standardRemoveAll(collection); }
        @Override public boolean retainAll(Collection<?> collection) { return standardRetainAll(collection); }
        @Override public Iterator<K> iterator() { return project(entrySet().iterator(), Entry::getKey); }
    }

    @Override public Set<V> values() {
        if (values == null) values = new ValueSet();
        return values;
    }

    private final class ValueSet extends ForwardingSet<V> {
        @Override protected Set<V> delegate() { return inverse.keySet(); }
        @Override public Iterator<V> iterator() { return project(entrySet().iterator(), Entry::getValue); }
        @Override public Object[] toArray() { return standardToArray(); }
        @Override public <T> T[] toArray(T[] array) { return standardToArray(array); }
        @Override public String toString() { return standardToString(); }
    }

    @Override public Set<Entry<K, V>> entrySet() {
        if (entries == null) entries = new EntrySet();
        return entries;
    }

    private final class BiMapEntry extends ForwardingMapEntry<K, V> {
        private final Entry<K, V> entry;
        BiMapEntry(Entry<K, V> entry) { this.entry = entry; }
        @Override protected Entry<K, V> delegate() { return entry; }

        @Override public V setValue(V value) {
            if (!entrySet().contains(this)) throw new IllegalStateException("entry no longer in map");
            if (Objects.equals(value, getValue())) return value;
            if (containsValue(value)) throw new IllegalArgumentException("value already present: " + value);
            V previous = entry.setValue(value);
            if (!Objects.equals(value, get(getKey()))) throw new IllegalStateException("entry no longer in map");
            updateInverse(getKey(), true, previous, value);
            return previous;
        }
    }

    private Iterator<Entry<K, V>> entryIterator() {
        Iterator<Entry<K, V>> iterator = forward.entrySet().iterator();
        return new Iterator<>() {
            private Entry<K, V> last;
            @Override public boolean hasNext() { return iterator.hasNext(); }
            @Override public Entry<K, V> next() { last = iterator.next(); return new BiMapEntry(last); }
            @Override public void remove() {
                if (last == null) throw new IllegalStateException("no next since last remove");
                V value = last.getValue();
                iterator.remove();
                inverse.forward.remove(value);
                last = null;
            }
        };
    }

    private final class EntrySet extends ForwardingSet<Entry<K, V>> {
        @Override protected Set<Entry<K, V>> delegate() { return forward.entrySet(); }
        @Override public void clear() { M3GuavaBiMap.this.clear(); }
        @Override public Iterator<Entry<K, V>> iterator() { return entryIterator(); }

        @Override public boolean contains(Object object) {
            if (!(object instanceof Entry<?, ?> entry)) return false;
            return forward.entrySet().contains(new AbstractMap.SimpleImmutableEntry<>(entry));
        }

        @Override public boolean remove(Object object) {
            if (!contains(object)) return false;
            Entry<?, ?> entry = (Entry<?, ?>) object;
            // Read both fields before invalidating a caller-supplied live entry.
            Object key = entry.getKey();
            Object value = entry.getValue();
            inverse.forward.remove(value);
            forward.remove(key);
            return true;
        }

        @Override public Object[] toArray() { return standardToArray(); }
        @Override public <T> T[] toArray(T[] array) { return standardToArray(array); }
        @Override public boolean containsAll(Collection<?> collection) { return standardContainsAll(collection); }
        @Override public boolean removeAll(Collection<?> collection) { return standardRemoveAll(collection); }
        @Override public boolean retainAll(Collection<?> collection) { return standardRetainAll(collection); }
    }

    private static <A, B> Iterator<B> project(Iterator<A> iterator, Function<? super A, ? extends B> projection) {
        return new Iterator<>() {
            @Override public boolean hasNext() { return iterator.hasNext(); }
            @Override public B next() { return projection.apply(iterator.next()); }
            @Override public void remove() { iterator.remove(); }
        };
    }

    /** Checks both slot owners and the exact forward/inverse membership relation. */
    public void verifyInvariants() {
        forward.verifyInvariants();
        inverse.forward.verifyInvariants();
        if (forward.size() != inverse.forward.size()) throw new AssertionError("inverse size");
        forward.forEach((key, value) -> {
            if (!inverse.forward.containsKey(value) || !Objects.equals(key, inverse.forward.get(value))) {
                throw new AssertionError("inverse membership");
            }
        });
    }
}
