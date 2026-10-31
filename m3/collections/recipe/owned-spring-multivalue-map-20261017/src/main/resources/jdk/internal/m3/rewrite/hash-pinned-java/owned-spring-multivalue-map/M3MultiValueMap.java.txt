/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// SPDX-License-Identifier: Apache-2.0
// Modified 2026 by Hitesh Soliwal and contributors: owned M3 directory/list storage,
// independent scalar view and owned serialization envelope.
// LinkedMultiValueMap protocol portions: Copyright 2002-2020 the original author or authors.
package com.m3.collections.spring;

import com.m3.collections.M3ArrayList;
import com.m3.collections.M3LinkedHashMap;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import org.springframework.util.MultiValueMap;

/**
 * An independent Spring MultiValueMap body with owned M3 array/index storage.
 *
 * <p>Lists supplied through put/copy/clone remain caller-owned aliases. Lists created by add,
 * set and deepCopy are mutable M3ArrayList owners. Ordinary Map methods/views,
 * putIfAbsent and forEach delegate to the directory; compute, merge and replaceAll retain
 * Map default dispatch, as in the pinned Spring adapter protocol.</p>
 *
 * <p>Expected size is validated as a construction hint; the sealed directory allocates lazily.
 * The serialized form uses its own UID/envelope, preserving key/list object-graph aliases
 * rather than Spring implementation/wire identity. Single-threaded or externally synchronized.</p>
 */
public final class M3MultiValueMap<K, V> implements MultiValueMap<K, V>, Serializable, Cloneable {
    @Serial private static final long serialVersionUID = 1L;
    private transient M3LinkedHashMap<K, List<V>> directory;

    public M3MultiValueMap() {
        directory = new M3LinkedHashMap<>();
    }

    public M3MultiValueMap(final int expectedSize) {
        if (expectedSize < 0) {
            throw new IllegalArgumentException("negative expectedSize");
        }
        directory = new M3LinkedHashMap<>();
    }

    /** Shallow copy in source entry order; every supplied list object is retained. */
    public M3MultiValueMap(final Map<K, List<V>> source) {
        this();
        for (final Map.Entry<K, List<V>> entry : source.entrySet()) {
            directory.put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public V getFirst(final K key) {
        final List<V> values = directory.get(key);
        return values != null && !values.isEmpty() ? values.get(0) : null;
    }

    @Override
    public void add(final K key, final V value) {
        final List<V> values = directory.computeIfAbsent(key, ignored -> new M3ArrayList<>(1));
        values.add(value);
    }

    @Override
    public void addAll(final K key, final List<? extends V> values) {
        final List<V> current = directory.computeIfAbsent(key, ignored -> new M3ArrayList<>(values.size()));
        current.addAll(values);
    }

    @Override
    public void addAll(final MultiValueMap<K, V> values) {
        values.forEach(this::addAll);
    }

    @Override
    public void set(final K key, final V value) {
        final List<V> values = new M3ArrayList<>(1);
        values.add(value);
        directory.put(key, values);
    }

    @Override
    public void setAll(final Map<K, V> values) {
        values.forEach(this::set);
    }

    @Override
    public Map<K, V> toSingleValueMap() {
        final Map<K, V> singles = new M3LinkedHashMap<>();
        directory.forEach((key, values) -> {
            if (values != null && !values.isEmpty()) {
                singles.put(key, values.get(0));
            }
        });
        return singles;
    }

    @Override
    public Map<K, V> asSingleValueMap() {
        return new M3SingleValueMapView<>(this);
    }

    public M3MultiValueMap<K, V> deepCopy() {
        final M3MultiValueMap<K, V> copy = new M3MultiValueMap<>(size());
        forEach((key, values) -> copy.put(key, new M3ArrayList<>(values)));
        return copy;
    }

    @Override
    public M3MultiValueMap<K, V> clone() {
        return new M3MultiValueMap<>(this);
    }

    @Override
    public int size() {
        return directory.size();
    }

    @Override
    public boolean isEmpty() {
        return directory.isEmpty();
    }

    @Override
    public boolean containsKey(final Object key) {
        return directory.containsKey(key);
    }

    @Override
    public boolean containsValue(final Object value) {
        return directory.containsValue(value);
    }

    @Override
    public List<V> get(final Object key) {
        return directory.get(key);
    }

    @Override
    public List<V> put(final K key, final List<V> value) {
        return directory.put(key, value);
    }

    @Override
    public List<V> putIfAbsent(final K key, final List<V> value) {
        return directory.putIfAbsent(key, value);
    }

    @Override
    public List<V> remove(final Object key) {
        return directory.remove(key);
    }

    @Override
    public void putAll(final Map<? extends K, ? extends List<V>> source) {
        directory.putAll(source);
    }

    @Override
    public void clear() {
        directory.clear();
    }

    @Override
    public Set<K> keySet() {
        return directory.keySet();
    }

    @Override
    public Collection<List<V>> values() {
        return directory.values();
    }

    @Override
    public Set<Map.Entry<K, List<V>>> entrySet() {
        return directory.entrySet();
    }

    @Override
    public void forEach(final BiConsumer<? super K, ? super List<V>> action) {
        directory.forEach(action);
    }

    @Override
    public boolean equals(final Object other) {
        return this == other || directory.equals(other);
    }

    @Override
    public int hashCode() {
        return directory.hashCode();
    }

    @Override
    public String toString() {
        return directory.toString();
    }

    @Serial
    private void writeObject(final ObjectOutputStream output) throws IOException {
        output.defaultWriteObject();
        output.writeInt(directory.size());
        for (final K key : directory.keySet()) {
            output.writeObject(key);
            output.writeObject(directory.get(key));
        }
    }

    @SuppressWarnings("unchecked")
    @Serial
    private void readObject(final ObjectInputStream input) throws IOException, ClassNotFoundException {
        input.defaultReadObject();
        final int count = input.readInt();
        if (count < 0) {
            throw new InvalidObjectException("negative mapping count");
        }
        directory = new M3LinkedHashMap<>();
        for (int i = 0; i < count; i++) {
            final K key = (K) input.readObject();
            final Object value = input.readObject();
            if (value != null && !(value instanceof List<?>)) {
                throw new InvalidObjectException("mapping value is not a list");
            }
            directory.put(key, (List<V>) value);
        }
    }
}
