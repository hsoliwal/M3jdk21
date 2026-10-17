/* SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors */
package com.m3.collections.interop;

import com.google.common.collect.BiMap;
import com.google.common.collect.Multiset;
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
import org.apache.commons.collections4.Bag;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.ListValuedMap;
import org.apache.commons.collections4.MultiSet;
import org.apache.commons.collections4.SetValuedMap;
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.map.MutableOrderedMap;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.springframework.util.MultiValueMap;

/**
 * Stream result-owner closure for the supported M3/JDK/Guava/Eclipse/Spring/Apache contracts.
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

    /** Counts stream elements, retaining nulls and refusing per-element count overflow. */
    public static <E> Collector<E, ?, Multiset<E>> toGuavaMultiset() {
        return Collector.of(M3LibraryCollections::<E>guavaMultiset, Multiset<E>::add,
                (left, right) -> { left.addAll(right); return left; });
    }

    /** Counts stream elements using Apache MultiSet saturation and old-count semantics. */
    public static <E> Collector<E, ?, MultiSet<E>> toApacheMultiSet() {
        return Collector.of(M3LibraryCollections::<E>apacheMultiSet, MultiSet<E>::add,
                (left, right) -> {
                    for (MultiSet.Entry<E> entry : right.entrySet()) {
                        left.add(entry.getElement(), entry.getCount());
                    }
                    return left;
                });
    }

    /** Legacy Bag collector; duplicate add returns do not affect stream accumulation. */
    @SuppressWarnings("deprecation")
    public static <E> Collector<E, ?, Bag<E>> toApacheBag() {
        return Collector.of(M3LibraryCollections::<E>apacheBag, Bag<E>::add,
                (left, right) -> {
                    for (E element : right.uniqueSet()) left.add(element, right.getCount(element));
                    return left;
                });
    }

    /** Counts stream elements into an M3-owned Eclipse MutableBag. */
    public static <E> Collector<E, ?, MutableBag<E>> toEclipseBag() {
        return Collector.of(M3LibraryCollections::<E>eclipseBag, MutableBag<E>::add,
                (left, right) -> { right.forEachWithOccurrences(left::addOccurrences); return left; });
    }

    /** Retains value order and duplicate pairs under Apache ListValuedMap semantics. */
    public static <T, K, V> Collector<T, ?, ListValuedMap<K, V>> toApacheListValuedMap(
            Function<? super T, ? extends K> keys, Function<? super T, ? extends V> values) {
        Objects.requireNonNull(keys, "keys"); Objects.requireNonNull(values, "values");
        Supplier<ListValuedMap<K, V>> owner = M3LibraryCollections::apacheListValuedMap;
        return Collector.of(owner, (map, item) -> map.put(keys.apply(item), values.apply(item)),
                (left, right) -> { left.putAll(right); return left; });
    }

    /** Suppresses duplicate pairs under Apache SetValuedMap semantics. */
    public static <T, K, V> Collector<T, ?, SetValuedMap<K, V>> toApacheSetValuedMap(
            Function<? super T, ? extends K> keys, Function<? super T, ? extends V> values) {
        Objects.requireNonNull(keys, "keys"); Objects.requireNonNull(values, "values");
        Supplier<SetValuedMap<K, V>> owner = M3LibraryCollections::apacheSetValuedMap;
        return Collector.of(owner, (map, item) -> map.put(keys.apply(item), values.apply(item)),
                (left, right) -> { left.putAll(right); return left; });
    }

    /** Ordered Eclipse map collector; duplicate keys are refused. */
    public static <T, K, V> Collector<T, ?, MutableOrderedMap<K, V>> toEclipseOrderedMap(
            Function<? super T, ? extends K> keys, Function<? super T, ? extends V> values) {
        return uniqueMap(keys, values, M3LibraryCollections::<K, V>eclipseOrderedMap);
    }

    /** Guava BiMap collector; duplicate keys or values are refused in every partition. */
    public static <T, K, V> Collector<T, ?, BiMap<K, V>> toGuavaBiMap(
            Function<? super T, ? extends K> keys, Function<? super T, ? extends V> values) {
        return uniqueBidi(keys, values, M3LibraryCollections::<K, V>guavaBiMap);
    }

    /**
     * Apache BidiMap result collector. Duplicate keys and values are refused during
     * collection, so parallel merging cannot silently discard input mappings.
     * Subsequent result.put calls retain Apache's ordinary displacement semantics.
     */
    public static <T, K, V> Collector<T, ?, BidiMap<K, V>> toApacheBidiMap(
            Function<? super T, ? extends K> keys, Function<? super T, ? extends V> values) {
        return uniqueBidi(keys, values, M3LibraryCollections::<K, V>apacheBidiMap);
    }

    private static <T, K, V, M extends Map<K, V>> Collector<T, ?, M> uniqueBidi(
            Function<? super T, ? extends K> keys, Function<? super T, ? extends V> values,
            Supplier<M> owner) {
        Objects.requireNonNull(keys, "keys"); Objects.requireNonNull(values, "values");
        return Collector.of(owner,
                (map, item) -> putUniqueBidi(map, keys.apply(item), values.apply(item)),
                (left, right) -> { right.forEach((k, v) -> putUniqueBidi(left, k, v)); return left; });
    }

    private static <K, V> void putUniqueBidi(Map<K, V> map, K key, V value) {
        Objects.requireNonNull(value, "value");
        if (map.containsValue(value)) throw new IllegalStateException("duplicate value: " + value);
        putUnique(map, key, value);
    }
}
