/* SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors */
package com.m3.collections.interop;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.SortedSetMultimap;
import com.m3.collections.M3LinkedHashMap;
import com.m3.collections.M3LinkedHashSet;
import com.m3.collections.M3LinkedList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.springframework.util.MultiValueMap;

/**
 * Stream result-owner closure for the supported M3/JDK/Guava/Eclipse/Spring contracts.
 * All accumulation and merge targets are independently created M3-backed owners.
 *
 * <p>Collectors are deliberately neither CONCURRENT nor UNORDERED: parallel streams
 * get isolated partial owners and ordered merging. This does not make the result
 * collections thread-safe. No intermediary list/map of all input elements is built
 * by these collectors, but stream tasks, views and library internals may allocate.
 *
 * <p>Lists and multimaps retain null payloads supported by their owner. Map collectors
 * accept null keys, reject null incoming values, and either reject duplicate keys
 * or use the explicitly supplied merge function (a null merged result removes a key).
 * Mapper and merge callback effects remain observable; callers must satisfy stream
 * non-interference and associativity requirements for deterministic parallel results.
 */
public final class M3LibraryCollectors {
    private M3LibraryCollectors() { }

    public static <E> Collector<E, ?, List<E>> toList() {
        return Collector.of(M3LinkedList<E>::new, List<E>::add,
                (left, right) -> { left.addAll(right); return left; });
    }

    public static <E> Collector<E, ?, Set<E>> toSet() {
        return Collector.of(M3LinkedHashSet<E>::new, Set<E>::add,
                (left, right) -> { left.addAll(right); return left; });
    }

    public static <T, K, V> Collector<T, ?, Map<K, V>> toMap(
            Function<? super T, ? extends K> keys, Function<? super T, ? extends V> values) {
        return uniqueMap(keys, values, M3LinkedHashMap<K, V>::new);
    }

    public static <T, K, V> Collector<T, ?, Map<K, V>> toMap(
            Function<? super T, ? extends K> keys, Function<? super T, ? extends V> values,
            BinaryOperator<V> merger) {
        Objects.requireNonNull(keys, "keys"); Objects.requireNonNull(values, "values");
        Objects.requireNonNull(merger, "merger");
        Supplier<Map<K, V>> owner = M3LinkedHashMap::new;
        return Collector.of(owner,
                (map, item) -> map.merge(keys.apply(item), Objects.requireNonNull(values.apply(item)), merger),
                (left, right) -> { right.forEach((k, v) -> left.merge(k, v, merger)); return left; });
    }

    private static <T, K, V, M extends Map<K, V>> Collector<T, ?, M> uniqueMap(
            Function<? super T, ? extends K> keys, Function<? super T, ? extends V> values,
            Supplier<M> owner) {
        Objects.requireNonNull(keys, "keys"); Objects.requireNonNull(values, "values");
        return Collector.of(owner,
                (map, item) -> putUnique(map, keys.apply(item), values.apply(item)),
                (left, right) -> { right.forEach((k, v) -> putUnique(left, k, v)); return left; });
    }

    private static <K, V> void putUnique(Map<K, V> map, K key, V value) {
        Objects.requireNonNull(value, "value");
        if (map.containsKey(key)) throw new IllegalStateException("duplicate key: " + key);
        map.put(key, value);
    }

    public static <T, K, V> Collector<T, ?, ListMultimap<K, V>> toGuavaListMultimap(
            Function<? super T, ? extends K> keys, Function<? super T, ? extends V> values) {
        return Multimaps.toMultimap(Objects.requireNonNull(keys, "keys"),
                Objects.requireNonNull(values, "values"), M3LibraryCollections::guavaListMultimap);
    }

    public static <T, K, V> Collector<T, ?, SetMultimap<K, V>> toGuavaSetMultimap(
            Function<? super T, ? extends K> keys, Function<? super T, ? extends V> values) {
        return Multimaps.toMultimap(Objects.requireNonNull(keys, "keys"),
                Objects.requireNonNull(values, "values"), M3LibraryCollections::guavaSetMultimap);
    }

    public static <T, K, V> Collector<T, ?, SortedSetMultimap<K, V>> toGuavaSortedSetMultimap(
            Function<? super T, ? extends K> keys, Function<? super T, ? extends V> values,
            Comparator<? super V> order) {
        Objects.requireNonNull(order, "order");
        return Multimaps.toMultimap(Objects.requireNonNull(keys, "keys"),
                Objects.requireNonNull(values, "values"), () -> M3LibraryCollections.guavaSortedSetMultimap(order));
    }

    public static <E> Collector<E, ?, MutableList<E>> toEclipseList() {
        return Collector.of(M3LibraryCollections::<E>eclipseList, MutableList<E>::add,
                (left, right) -> { left.addAll(right); return left; });
    }

    public static <E> Collector<E, ?, MutableSet<E>> toEclipseSet() {
        return Collector.of(M3LibraryCollections::<E>eclipseSet, MutableSet<E>::add,
                (left, right) -> { left.addAll(right); return left; });
    }

    public static <T, K, V> Collector<T, ?, MutableMap<K, V>> toEclipseMap(
            Function<? super T, ? extends K> keys, Function<? super T, ? extends V> values) {
        return uniqueMap(keys, values, M3LibraryCollections::<K, V>eclipseMap);
    }

    public static <T, K, V> Collector<T, ?, MultiValueMap<K, V>> toSpringMultiValueMap(
            Function<? super T, ? extends K> keys, Function<? super T, ? extends V> values) {
        Objects.requireNonNull(keys, "keys"); Objects.requireNonNull(values, "values");
        Supplier<MultiValueMap<K, V>> owner = M3SpringMultiValueMap::new;
        return Collector.of(owner,
                (map, item) -> map.add(keys.apply(item), values.apply(item)),
                (left, right) -> { left.addAll(right); return left; });
    }
}
