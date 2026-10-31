/* SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 Hitesh Soliwal and contributors */
package com.m3.collections.interop;

import com.google.common.collect.BiMap;
import com.google.common.collect.Maps;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collector;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.map.mutable.MapAdapter;

/** Explicit JDK/Guava/Eclipse boundaries over the same M3 bidirectional owner. */
public final class M3BiMaps {
    private M3BiMaps() { }

    /** Detached structure, shared caller-owned key/value references. Nulls are allowed. */
    public static <K, V> M3BiMap<K, V> copyOf(Map<? extends K, ? extends V> source) {
        return M3BiMap.create(source);
    }
    /** Read-only view of an independent M3 copy; inverse and entry mutation are blocked. */
    public static <K, V> BiMap<K, V> unmodifiableCopyOf(Map<? extends K, ? extends V> source) {
        return Maps.unmodifiableBiMap(copyOf(source));
    }
    /** Guava owns locking. All access, including iteration, must follow its wrapper contract. */
    public static <K, V> BiMap<K, V> synchronizedMap() {
        return Maps.synchronizedBiMap(M3BiMap.<K, V>create());
    }
    /** Live Eclipse MutableMap view, NOT an Eclipse MutableBiMap implementation. */
    public static <K, V> MutableMap<K, V> asEclipseMap(BiMap<K, V> owner) {
        return MapAdapter.adapt(Objects.requireNonNull(owner, "owner"));
    }
    /** Detached M3 ownership imported through Eclipse's distinct-pair traversal. */
    public static <K, V> M3BiMap<K, V> fromEclipse(org.eclipse.collections.api.bimap.BiMap<K, V> source) {
        Objects.requireNonNull(source, "source");
        M3BiMap<K, V> result = M3BiMap.create();
        source.forEachKeyValue(result::put);
        return result;
    }
    /**
     * Ordered independent parallel partials; duplicate keys OR values are errors,
     * including repeated equal pairs. Null keys/values are accepted once. Not CONCURRENT.
     */
    public static <T, K, V> Collector<T, ?, M3BiMap<K, V>> toBiMap(
            Function<? super T, ? extends K> key, Function<? super T, ? extends V> value) {
        Objects.requireNonNull(key, "key"); Objects.requireNonNull(value, "value");
        return Collector.of(M3BiMap<K, V>::new,
                (map, item) -> insertUnique(map, key.apply(item), value.apply(item)),
                (left, right) -> { right.forEach((k, v) -> insertUnique(left, k, v)); return left; });
    }
    private static <K, V> void insertUnique(M3BiMap<K, V> map, K key, V value) {
        if (map.containsKey(key)) throw new IllegalArgumentException("duplicate key");
        map.put(key, value);
    }
}
