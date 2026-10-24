/*******************************************************************************
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
/*
 * Generated from canonical Synexia M3 source-sealed recipe.
 */
package com.m3.collections;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

/**
 * M3-owned equality-keyed, one-to-one relation using two existing indexed
 * {@link M3LinkedHashMap} owners, rather than per-mapping entry nodes.
 * Forward and inverse are live views sharing those same two indexed maps.
 * Null keys/values are permitted by this M3-specific contract. No Guava or
 * Apache class is implemented here. Not concurrent; resource failures do not
 * promise multi-map transactionality. No JNI is needed for a scalar update.
 */
public final class M3IndexedBiMap<K, V> extends AbstractMap<K, V> {
    private final M3LinkedHashMap<K, V> forward;
    private final M3LinkedHashMap<V, K> reverse;
    private final Epoch epoch;
    private M3IndexedBiMap<V, K> inverse;
    private transient Set<Entry<K, V>> entries;

    private static final class Epoch { long revision; }

    public M3IndexedBiMap() {
        this(new M3LinkedHashMap<>(), new M3LinkedHashMap<>(), new Epoch());
        inverse = new M3IndexedBiMap<>(reverse, forward, epoch);
        inverse.inverse = this;
    }

    private M3IndexedBiMap(M3LinkedHashMap<K, V> forward,
            M3LinkedHashMap<V, K> reverse, Epoch epoch) {
        this.forward = forward;
        this.reverse = reverse;
        this.epoch = epoch;
    }

    public long revision() { return epoch.revision; }
    public M3IndexedBiMap<V, K> inverse() { return inverse; }
    @Override public int size() { return forward.size(); }
    @Override public boolean containsKey(Object key) { return forward.containsKey(key); }
    @Override public boolean containsValue(Object value) { return reverse.containsKey(value); }
    @Override public V get(Object key) { return forward.get(key); }

    @Override public V put(K key, V value) {
        boolean present = forward.containsKey(key);
        V old = present ? forward.get(key) : null;
        if (reverse.containsKey(value)) {
            K owner = reverse.get(value);
            if (!present || !Objects.equals(owner, key)) {
                throw new IllegalArgumentException("value already belongs to another M3 key");
            }
        }
        if (present && old == value) return old;
        if (present) reverse.remove(old);
        forward.put(key, value);
        reverse.put(value, key);
        epoch.revision++;
        return old;
    }

    /** Replace any prior owner of value, maintaining uniqueness in both indexes. */
    public V forcePut(K key, V value) {
        if (reverse.containsKey(value)) {
            K owner = reverse.get(value);
            if (!forward.containsKey(key) || !Objects.equals(owner, key)) {
                remove(owner);
            }
        }
        return put(key, value);
    }

    @Override public V remove(Object key) {
        if (!forward.containsKey(key)) return null;
        V old = forward.remove(key);
        reverse.remove(old);
        epoch.revision++;
        return old;
    }

    @Override public void clear() {
        if (forward.isEmpty()) return;
        forward.clear();
        reverse.clear();
        epoch.revision++;
    }

    @Override public Set<Entry<K, V>> entrySet() {
        if (entries == null) entries = new Entries();
        return entries;
    }

    private final class Entries extends AbstractSet<Entry<K, V>> {
        @Override public int size() { return forward.size(); }
        @Override public void clear() { M3IndexedBiMap.this.clear(); }
        @Override public Iterator<Entry<K, V>> iterator() {
            Iterator<K> keys = forward.keySet().iterator();
            return new Iterator<>() {
                private K last;
                private boolean removable;
                @Override public boolean hasNext() { return keys.hasNext(); }
                @Override public Entry<K, V> next() {
                    if (!hasNext()) throw new NoSuchElementException();
                    K key = keys.next();
                    last = key;
                    removable = true;
                    return new Entry<>() {
                        @Override public K getKey() { return key; }
                        @Override public V getValue() { return forward.get(key); }
                        @Override public V setValue(V value) {
                            if (!forward.containsKey(key)) throw new IllegalStateException("removed mapping");
                            return M3IndexedBiMap.this.put(key, value);
                        }
                        @Override public boolean equals(Object other) {
                            return other instanceof Map.Entry<?, ?> e
                                    && Objects.equals(key, e.getKey())
                                    && Objects.equals(getValue(), e.getValue());
                        }
                        @Override public int hashCode() {
                            return Objects.hashCode(key) ^ Objects.hashCode(getValue());
                        }
                    };
                }
                @Override public void remove() {
                    if (!removable) throw new IllegalStateException("next required");
                    // The stored value might have changed through Entry.setValue.
                    V current = forward.get(last);
                    keys.remove();
                    reverse.remove(current);
                    epoch.revision++;
                    removable = false;
                }
            };
        }
    }
}
