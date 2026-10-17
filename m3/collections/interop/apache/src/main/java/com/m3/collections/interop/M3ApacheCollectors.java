/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections.interop;

import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collector;
import org.apache.commons.collections4.ListValuedMap;
import org.apache.commons.collections4.MultiSet;
import org.apache.commons.collections4.SetValuedMap;

/**
 * Ordered reduction to independent M3-backed Apache owners. Collectors claim neither
 * CONCURRENT nor UNORDERED; parallel reduction does not make results thread-safe.
 * Callbacks must be non-interfering. Partial owners merge left to right. Null keys
 * and values retain the selected provider family's ordinary null support.
 * No BidiMap collector is offered: eviction loses history, so a state-only parallel
 * combiner cannot generally reproduce sequential put semantics.
 */
public final class M3ApacheCollectors {
    private M3ApacheCollectors() { }
    public static <T,K,V> Collector<T,?,ListValuedMap<K,V>> toListValuedMap(
            Function<? super T,? extends K> keys, Function<? super T,? extends V> values) {
        Objects.requireNonNull(keys, "keys"); Objects.requireNonNull(values, "values");
        return Collector.of(M3ApacheCollections::<K,V>listValuedMap,
                (map, item) -> map.put(keys.apply(item), values.apply(item)),
                (left, right) -> { left.putAll(right); return left; });
    }
    public static <T,K,V> Collector<T,?,SetValuedMap<K,V>> toSetValuedMap(
            Function<? super T,? extends K> keys, Function<? super T,? extends V> values) {
        Objects.requireNonNull(keys, "keys"); Objects.requireNonNull(values, "values");
        return Collector.of(M3ApacheCollections::<K,V>setValuedMap,
                (map, item) -> map.put(keys.apply(item), values.apply(item)),
                (left, right) -> { left.putAll(right); return left; });
    }
    /** Combines distinct counts without expanding every repeated occurrence. */
    public static <E> Collector<E,?,MultiSet<E>> toMultiSet() {
        return Collector.of(M3ApacheMultiSet<E>::new, M3ApacheMultiSet::add,
                (left, right) -> { right.forEachEntry(left::add); return left; },
                owner -> owner);
    }
}
