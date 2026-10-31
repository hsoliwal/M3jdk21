/*
 * Copyright (C) 2007 The Guava Authors
 * Copyright 2026 Hitesh Soliwal and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Modified 2026: adapt Guava AbstractBiMap's dual-map/inverse/view behavioral
 * atoms to existing M3LinkedHashMap owners; stage replaceAll; no serialization.
 */
package com.m3.collections.interop;

import com.google.common.collect.BiMap;
import com.m3.collections.M3LinkedHashMap;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * Mutable Guava BiMap contract over two existing M3 stable-slot map indexes.
 * Null keys and values are supported. Keys and values must have stable,
 * nonthrowing, non-reentrant hashCode/equals. No identity-key substitution,
 * Serializable concrete-class compatibility or concurrent access is promised.
 *
 * <p>No per-mapping Entry or node is retained by this class. Key/value iteration
 * traverses the existing key iterator; Entry projections allocate only at the
 * entrySet boundary. Both inverse directions share one revision counter.
 * Each direction follows its own key-insertion order; values follow that
 * direction's key order. There is no cross-direction encounter-order promise.
 *
 * <p>Duplicate put/entry updates leave the map unchanged. putAll is sequential
 * and may partly succeed. replaceAll stages both indexes, supports permutations,
 * and publishes only after all callbacks/uniqueness checks succeed. This is a
 * stronger failure guarantee than some donor implementations, not their exact
 * exceptional-state behavior. Callback-owned mutations are not rolled back.
 * Compound put/remove operations are not generally OOME/hostile-callback atomic.
 * Use external locking, including iteration, for shared access.
 */
public final class M3BiMap<K, V> extends AbstractMap<K, V> implements BiMap<K, V> {
    private M3LinkedHashMap<K, V> forward;
    private final M3BiMap<V, K> inverse;
    private final Revision revision;
    private Set<K> keys;
    private Set<V> values;
    private Set<Entry<K, V>> entries;

    private static final class Revision { long value; }

    public M3BiMap() {
        forward = new M3LinkedHashMap<>();
        revision = new Revision();
        inverse = new M3BiMap<>(new M3LinkedHashMap<>(), this, revision);
    }

    private M3BiMap(M3LinkedHashMap<K, V> forward, M3BiMap<V, K> inverse, Revision revision) {
        this.forward = forward;
        this.inverse = inverse;
        this.revision = revision;
    }

    public static <K, V> M3BiMap<K, V> create() { return new M3BiMap<>(); }
    public static <K, V> M3BiMap<K, V> create(Map<? extends K, ? extends V> source) {
        M3BiMap<K, V> result = create();
        result.putAll(Objects.requireNonNull(source, "source"));
        return result;
    }
    @Override public int size() { return forward.size(); }
    @Override public boolean containsKey(Object key) { return forward.containsKey(key); }
    @Override public boolean containsValue(Object value) { return inverse.forward.containsKey(value); }
    @Override public V get(Object key) { return forward.get(key); }
    @Override public M3BiMap<V, K> inverse() { return inverse; }
    @Override public V put(K key, V value) { return put(key, value, false); }
    @Override public V forcePut(K key, V value) { return put(key, value, true); }

    private V put(K key, V value, boolean force) {
        boolean present = forward.containsKey(key);
        V previous = forward.get(key);
        if (present && Objects.equals(previous, value)) return previous;
        boolean conflict = inverse.forward.containsKey(value);
        if (conflict && !force) throw new IllegalArgumentException("value already present");
        // Retain the existing key representative when an equal key is supplied.
        K resident = present ? inverse.forward.get(previous) : key;
        if (conflict) forward.remove(inverse.forward.get(value));
        forward.put(resident, value);
        if (present) inverse.forward.remove(previous);
        inverse.forward.put(value, resident);
        revision.value++;
        return previous;
    }

    @Override public V remove(Object key) {
        if (!forward.containsKey(key)) return null;
        V previous = forward.remove(key);
        inverse.forward.remove(previous);
        revision.value++;
        return previous;
    }
    @Override public void clear() {
        if (isEmpty()) return;
        forward.clear(); inverse.forward.clear(); revision.value++;
    }
    @Override public void putAll(Map<? extends K, ? extends V> source) {
        Objects.requireNonNull(source, "source");
        for (Entry<? extends K, ? extends V> entry : source.entrySet()) put(entry.getKey(), entry.getValue());
    }
    @Override public void forEach(BiConsumer<? super K, ? super V> action) {
        Objects.requireNonNull(action, "action");
        long expected = revision.value;
        forward.forEach((key, value) -> { action.accept(key, value); check(expected); });
    }

    /** Stage replacements, including cyclic permutations; do not publish partial indexes. */
    @Override public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        Objects.requireNonNull(function, "function");
        long expected = revision.value;
        M3LinkedHashMap<K, V> next = new M3LinkedHashMap<>();
        M3LinkedHashMap<V, K> reverse = new M3LinkedHashMap<>();
        forward.forEach((key, old) -> {
            V value = function.apply(key, old);
            check(expected);
            // containsKey, not putIfAbsent's return: the existing owner may be null.
            if (reverse.containsKey(value)) throw new IllegalArgumentException("duplicate replacement value");
            next.put(key, value); reverse.put(value, key);
        });
        check(expected);
        if (isEmpty()) return;
        forward = next; inverse.forward = reverse; revision.value++;
    }

    @Override public Set<K> keySet() {
        if (keys == null) keys = new AbstractSet<>() {
            @Override public int size() { return M3BiMap.this.size(); }
            @Override public boolean contains(Object key) { return containsKey(key); }
            @Override public boolean remove(Object key) {
                if (!containsKey(key)) return false;
                M3BiMap.this.remove(key); return true;
            }
            @Override public void clear() { M3BiMap.this.clear(); }
            @Override public Iterator<K> iterator() {
                return new ViewIterator<>() { @Override K project(K key) { return key; } };
            }
        };
        return keys;
    }
    @Override public Set<V> values() {
        if (values == null) values = new AbstractSet<>() {
            @Override public int size() { return M3BiMap.this.size(); }
            @Override public boolean contains(Object value) { return containsValue(value); }
            @Override public boolean remove(Object value) {
                if (!inverse.containsKey(value)) return false;
                inverse.remove(value); return true;
            }
            @Override public void clear() { M3BiMap.this.clear(); }
            @Override public Iterator<V> iterator() {
                return new ViewIterator<>() { @Override V project(K key) { return forward.get(key); } };
            }
        };
        return values;
    }
    @Override public Set<Entry<K, V>> entrySet() {
        if (entries == null) entries = new AbstractSet<>() {
            @Override public int size() { return M3BiMap.this.size(); }
            @Override public void clear() { M3BiMap.this.clear(); }
            @Override public boolean contains(Object object) {
                return object instanceof Entry<?, ?> e && containsKey(e.getKey())
                        && Objects.equals(get(e.getKey()), e.getValue());
            }
            @Override public boolean remove(Object object) {
                if (!contains(object)) return false;
                M3BiMap.this.remove(((Entry<?, ?>) object).getKey()); return true;
            }
            @Override public Iterator<Entry<K, V>> iterator() {
                return new ViewIterator<>() {
                    @Override Entry<K, V> project(K key) {
                        return new SimpleEntry<>(key, forward.get(key)) {
                            private static final long serialVersionUID = 1L;
                            @Override public V setValue(V value) {
                                check(expected);
                                if (!containsKey(key) || !Objects.equals(getValue(), get(key))) {
                                    throw new IllegalStateException("entry no longer in map");
                                }
                                V previous = M3BiMap.this.put(key, value);
                                expected = revision.value;
                                super.setValue(value);
                                return previous;
                            }
                        };
                    }
                };
            }
        };
        return entries;
    }

    private abstract class ViewIterator<T> implements Iterator<T> {
        final Iterator<K> delegate = forward.keySet().iterator();
        long expected = revision.value;
        K last;
        boolean removable;
        abstract T project(K key);
        @Override public boolean hasNext() { check(expected); return delegate.hasNext(); }
        @Override public T next() {
            check(expected); last = delegate.next(); removable = true; return project(last);
        }
        @Override public void remove() {
            check(expected);
            if (!removable) throw new IllegalStateException();
            V value = forward.get(last);
            delegate.remove(); inverse.forward.remove(value);
            revision.value++; expected = revision.value; removable = false;
        }
    }
    private void check(long expected) {
        if (revision.value != expected) throw new ConcurrentModificationException();
    }
}
