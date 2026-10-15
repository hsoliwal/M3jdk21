/*
 * Portions of the method structure are adapted from Spring Framework
 * MultiValueMapAdapter.java at v6.2.0 (Git blob 4c7c2f1d133cccef1eab060524a71cd3de4b45a9).
 * Copyright 2002-2024 the original author or authors.
 * Copyright 2026 Hitesh Soliwal and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     https://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * SPDX-License-Identifier: Apache-2.0
 * Modified 2026: keep Spring's map-of-lists semantics, replacing only internally
 * allocated map/list owners with existing M3 owners. No donor source is relicensed.
 */
package com.m3.collections.interop;

import com.m3.collections.M3LinkedHashMap;
import com.m3.collections.M3LinkedList;
import java.io.Serial;
import java.util.List;
import java.util.Map;
import org.springframework.util.MultiValueMapAdapter;

/**
 * Spring's MultiValueMap contract with M3-owned directories, generated value lists,
 * and single-value copies. Empty and null-list mappings remain distinct from absence.
 *
 * <p>Lists supplied to put/putAll remain caller-owned and aliased, as in the donor:
 * copying or silently replacing those lists would break the Map contract. Mutations
 * through a supplied immutable list therefore still fail. The superclass supplies
 * the views and all remaining operations; this is not a second view implementation.
 *
 * <p>Not thread-safe, not a concrete LinkedMultiValueMap replacement, and no compatible
 * serialized form is promised. The inherited Serializable marker does not make the
 * non-serializable M3 backing owner serializable. No payload interning is performed.
 */
public final class M3SpringMultiValueMap<K, V> extends MultiValueMapAdapter<K, V> {
    @Serial private static final long serialVersionUID = 1L;
    // Same declared-map serialization boundary as the donor; compatibility is not claimed.
    @SuppressWarnings("serial") private final Map<K, List<V>> storage;

    public M3SpringMultiValueMap() { this(new M3LinkedHashMap<>()); }

    private M3SpringMultiValueMap(Map<K, List<V>> storage) {
        super(storage);
        this.storage = storage;
    }

    private static <V> List<V> valueList(int expectedSize) {
        if (expectedSize < 0) throw new IllegalArgumentException("negative expected size");
        return new M3LinkedList<>();
    }

    @Override public void add(K key, V value) {
        List<V> values = storage.computeIfAbsent(key, k -> valueList(1));
        values.add(value);
    }

    @Override public void addAll(K key, List<? extends V> values) {
        // Keep size() evaluation inside the absent/null-mapping branch, as in Spring.
        List<V> current = storage.computeIfAbsent(key, k -> valueList(values.size()));
        current.addAll(values);
    }

    @Override public void set(K key, V value) {
        List<V> values = valueList(1);
        values.add(value);
        storage.put(key, values);
    }

    @Override public Map<K, V> toSingleValueMap() {
        Map<K, V> single = new M3LinkedHashMap<>();
        storage.forEach((key, values) -> {
            if (values != null && !values.isEmpty()) single.put(key, values.get(0));
        });
        return single;
    }
}
