/* ******************************************************************************
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
package jdk.internal.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.SequencedCollection;
import java.util.SequencedMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;
/**
 * Thread-safe and immutable collection factory methods.
 *
 * <p>
 * Consolidates collection creation patterns to reduce boilerplate and ensure
 * consistent usage across the codebase.
 * </p>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * import static jdk.internal.m3.collections.CollectionFactories.*;
 *
 * // Thread-safe collections
 * Map<String, Object> map = concurrentMap();
 * List<String> list = copyOnWriteList();
 * Set<String> set = concurrentSet();
 *
 * // Sequenced collection helpers (Java 21)
 * String first = first(list);
 * String last = last(list);
 * List<String> rev = reversed(list);
 * }</pre>
 *
 * @author Hitesh Soliwal
 * @since 1.0.0
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/utils/CollectionFactories.java}
 * blob {@code 32d598a55fc77ad5cbb193e684ea9358256382e8} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common batch b29 (slf4j owners on the PlaceholderLogger facade) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class CollectionFactories {
    private static final PlaceholderLogger log = PlaceholderLogger.of(CollectionFactories.class);


    private CollectionFactories() {
        // Utility class - no instantiation
    }

    // --- THREAD-SAFE COLLECTIONS ---

    /**
     * Creates a new ConcurrentHashMap.
     *
     * @param <K>
     *            key type
     * @param <V>
     *            value type
     * @return new ConcurrentHashMap
     */
    public static <K, V> ConcurrentHashMap<K, V> concurrentMap() {
        return new ConcurrentHashMap<>();
    }

    /**
     * Creates a new ConcurrentHashMap with initial capacity.
     *
     * @param <K>
     *            key type
     * @param <V>
     *            value type
     * @param initialCapacity
     *            initial capacity
     * @return new ConcurrentHashMap
     */
    public static <K, V> ConcurrentHashMap<K, V> concurrentMap(int initialCapacity) {
        return new ConcurrentHashMap<>(initialCapacity);
    }

    /**
     * Creates a new CopyOnWriteArrayList.
     *
     * @param <T>
     *            element type
     * @return new CopyOnWriteArrayList
     */
    public static <T> CopyOnWriteArrayList<T> copyOnWriteList() {
        return new CopyOnWriteArrayList<>();
    }

    /**
     * Creates a new CopyOnWriteArrayList from existing collection.
     *
     * @param <T>
     *            element type
     * @param c
     *            source collection
     * @return new CopyOnWriteArrayList
     */
    public static <T> CopyOnWriteArrayList<T> copyOnWriteList(Collection<? extends T> c) {
        return new CopyOnWriteArrayList<>(c);
    }

    /**
     * Creates a new CopyOnWriteArraySet.
     *
     * @param <T>
     *            element type
     * @return new CopyOnWriteArraySet
     */
    public static <T> CopyOnWriteArraySet<T> copyOnWriteSet() {
        return new CopyOnWriteArraySet<>();
    }

    /**
     * Creates a new concurrent Set backed by ConcurrentHashMap.
     *
     * @param <T>
     *            element type
     * @return new concurrent Set
     */
    public static <T> Set<T> concurrentSet() {
        return ConcurrentHashMap.newKeySet();
    }

    /**
     * Creates a new concurrent Set with initial capacity.
     *
     * @param <T>
     *            element type
     * @param initialCapacity
     *            initial capacity
     * @return new concurrent Set
     */
    public static <T> Set<T> concurrentSet(int initialCapacity) {
        return ConcurrentHashMap.newKeySet(initialCapacity);
    }

    // --- STANDARD MUTABLE COLLECTIONS ---

    /**
     * Creates a new ArrayList.
     *
     * @param <T>
     *            element type
     * @return new ArrayList
     */
    public static <T> ArrayList<T> arrayList() {
        return new ArrayList<>();
    }

    /**
     * Creates a new ArrayList with initial capacity.
     *
     * @param <T>
     *            element type
     * @param initialCapacity
     *            initial capacity
     * @return new ArrayList
     */
    public static <T> ArrayList<T> arrayList(int initialCapacity) {
        return new ArrayList<>(initialCapacity);
    }

    /**
     * Creates a new HashMap.
     *
     * @param <K>
     *            key type
     * @param <V>
     *            value type
     * @return new HashMap
     */
    public static <K, V> HashMap<K, V> hashMap() {
        return new HashMap<>();
    }

    /**
     * Creates a new HashMap with initial capacity.
     *
     * @param <K>
     *            key type
     * @param <V>
     *            value type
     * @param initialCapacity
     *            initial capacity
     * @return new HashMap
     */
    public static <K, V> HashMap<K, V> hashMap(int initialCapacity) {
        return new HashMap<>(initialCapacity);
    }

    /**
     * Creates a new LinkedHashMap (insertion order preserved).
     *
     * @param <K>
     *            key type
     * @param <V>
     *            value type
     * @return new LinkedHashMap
     */
    public static <K, V> LinkedHashMap<K, V> linkedMap() {
        return new LinkedHashMap<>();
    }

    /**
     * Creates a new LinkedHashMap with access order (LRU cache pattern).
     *
     * @param <K>
     *            key type
     * @param <V>
     *            value type
     * @param config
     *            linked map configuration
     * @return new LinkedHashMap
     */
    public static <K, V> LinkedHashMap<K, V> linkedMap(LinkedMapConfig config) {
        return new LinkedHashMap<>(config.initialCapacity(), config.loadFactor(), config.accessOrder());
    }

    /**
     * Creates a new LinkedHashMap with access order (LRU cache pattern).
     *
     * @param <K>
     *            key type
     * @param <V>
     *            value type
     * @param initialCapacity
     *            initial capacity
     * @param loadFactor
     *            load factor
     * @param accessOrder
     *            true for access order, false for insertion order
     * @return new LinkedHashMap
     * @deprecated Use {@link #linkedMap(LinkedMapConfig)} instead
     */
    @Deprecated(since = "1.0.0", forRemoval = true)
    public static <K, V> LinkedHashMap<K, V> linkedMap(int initialCapacity, float loadFactor, boolean accessOrder) {
        return new LinkedHashMap<>(initialCapacity, loadFactor, accessOrder);
    }

    /**
     * Configuration record for creating LinkedHashMap with LRU cache pattern.
     *
     * @param initialCapacity
     *            initial capacity
     * @param loadFactor
     *            load factor
     * @param accessOrder
     *            true for access order (LRU), false for insertion order
     */
    public record LinkedMapConfig(int initialCapacity, float loadFactor, boolean accessOrder) {

        /** Default load factor. */
        private static final float DEFAULT_LOAD_FACTOR = 0.75f;

        /** Default initial capacity. */
        private static final int DEFAULT_INITIAL_CAPACITY = 16;

        /**
         * Creates a config for access-order (LRU) linked map.
         *
         * @param initialCapacity
         *            initial capacity
         * @return LRU config
         */
        public static LinkedMapConfig lru(int initialCapacity) {
            return new LinkedMapConfig(initialCapacity, DEFAULT_LOAD_FACTOR, true);
        }

        /**
         * Creates a config for insertion-order linked map.
         *
         * @param initialCapacity
         *            initial capacity
         * @return insertion-order config
         */
        public static LinkedMapConfig insertionOrder(int initialCapacity) {
            return new LinkedMapConfig(initialCapacity, DEFAULT_LOAD_FACTOR, false);
        }

        /**
         * Creates default config (insertion order).
         *
         * @return default config
         */
        public static LinkedMapConfig defaults() {
            return new LinkedMapConfig(DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR, false);
        }
    }

    /**
     * Creates a new HashSet.
     *
     * @param <T>
     *            element type
     * @return new HashSet
     */
    public static <T> HashSet<T> hashSet() {
        return new HashSet<>();
    }

    /**
     * Creates a new HashSet with initial capacity.
     *
     * @param <T>
     *            element type
     * @param initialCapacity
     *            initial capacity
     * @return new HashSet
     */
    public static <T> HashSet<T> hashSet(int initialCapacity) {
        return new HashSet<>(initialCapacity);
    }

    /**
     * Creates a new LinkedHashSet (insertion order preserved).
     *
     * @param <T>
     *            element type
     * @return new LinkedHashSet
     */
    public static <T> LinkedHashSet<T> linkedSet() {
        return new LinkedHashSet<>();
    }

    // --- IMMUTABLE COLLECTION FACTORIES (Java 21 compatible) ---

    /**
     * Creates an immutable list from varargs.
     *
     * @param <T>
     *            element type
     * @param items
     *            items to include
     * @return immutable list
     */
    //FLLDCIM: MRL — T2-L1 for-each → stream().collect(); behaviour preserved
    @SafeVarargs
    @SuppressWarnings("varargs") // LINT_SUPPRESS (declared, b29): source-retention
    public static <T> List<T> listOf(T... items) {
        if (items == null || items.length == 0) {
            return List.of();
        }
        return Arrays.stream(items).collect(Collectors.toUnmodifiableList());
    }

    /**
     * Creates an immutable set from varargs.
     *
     * @param <T>
     *            element type
     * @param items
     *            items to include
     * @return immutable set
     */
    //FLLDCIM: MRL — T2-L1 for-each → stream().collect(); behaviour preserved
    @SafeVarargs
    @SuppressWarnings("varargs") // LINT_SUPPRESS (declared, b29): source-retention
    public static <T> Set<T> setOf(T... items) {
        if (items == null || items.length == 0) {
            return Set.of();
        }
        return Arrays.stream(items).collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Creates an immutable singleton map.
     *
     * @param <K>
     *            key type
     * @param <V>
     *            value type
     * @param k1
     *            key
     * @param v1
     *            value
     * @return immutable map
     */
    public static <K, V> Map<K, V> mapOf(K k1, V v1) {
        return Map.of(k1, v1);
    }

    /**
     * Creates an immutable map with two entries.
     *
     * @param <K>
     *            key type
     * @param <V>
     *            value type
     * @param k1
     *            first key
     * @param v1
     *            first value
     * @param k2
     *            second key
     * @param v2
     *            second value
     * @return immutable map
     */
    public static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2) {
        return Map.of(k1, v1, k2, v2);
    }

    /**
     * Creates an immutable map with three entries.
     *
     * @param <K>
     *            key type
     * @param <V>
     *            value type
     * @param entries
     *            three map entries
     * @return immutable map
     */
    @SafeVarargs
    @SuppressWarnings("varargs")
    public static <K, V> Map<K, V> mapOfEntries(Map.Entry<K, V>... entries) {
        return Map.ofEntries(entries);
    }

    /**
     * Creates an immutable map with three entries (backward compatibility).
     *
     * @param <K>
     *            key type
     * @param <V>
     *            value type
     * @param k1
     *            first key
     * @param v1
     *            first value
     * @param k2
     *            second key
     * @param v2
     *            second value
     * @param k3
     *            third key
     * @param v3
     *            third value
     * @return immutable map
     * @deprecated Use {@link #mapOfEntries(Map.Entry[])} instead for more than 2
     *             entries
     */
    @Deprecated(since = "1.0.0", forRemoval = true)
    @SuppressWarnings("checkstyle:ParameterNumber") // Retained for backward compatibility
    public static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2, K k3, V v3) {
        return Map.of(k1, v1, k2, v2, k3, v3);
    }

    /**
     * Creates a map entry for use with {@link #mapOfEntries(Map.Entry[])}.
     *
     * @param <K>
     *            key type
     * @param <V>
     *            value type
     * @param key
     *            the key
     * @param value
     *            the value
     * @return map entry
     */
    public static <K, V> Map.Entry<K, V> entry(K key, V value) {
        return Map.entry(key, value);
    }

    // --- SEQUENCED COLLECTION HELPERS (Java 21) ---

    /**
     * Gets the first element of a sequenced collection.
     *
     * @param <T>
     *            element type
     * @param c
     *            the collection
     * @return first element or null if empty
     */
    public static <T> T first(SequencedCollection<T> c) {
        return c == null || c.isEmpty() ? null : c.getFirst();
    }

    /**
     * Gets the last element of a sequenced collection.
     *
     * @param <T>
     *            element type
     * @param c
     *            the collection
     * @return last element or null if empty
     */
    public static <T> T last(SequencedCollection<T> c) {
        return c == null || c.isEmpty() ? null : c.getLast();
    }

    /**
     * Gets a reversed view of the list.
     *
     * @param <T>
     *            element type
     * @param list
     *            the list
     * @return reversed view
     */
    public static <T> List<T> reversed(List<T> list) {
        return list == null ? List.of() : list.reversed();
    }

    /**
     * Gets a reversed view of the set.
     *
     * @param <T>
     *            element type
     * @param set
     *            the set (must be SequencedSet like LinkedHashSet)
     * @return reversed view
     */
    public static <T> Set<T> reversed(LinkedHashSet<T> set) {
        return set == null ? Set.of() : set.reversed();
    }

    /**
     * Gets the first key of a sequenced map.
     *
     * @param <K>
     *            key type
     * @param <V>
     *            value type
     * @param map
     *            the map
     * @return first key or null if empty
     */
    public static <K, V> K firstKey(SequencedMap<K, V> map) {
        return map == null || map.isEmpty() ? null : map.firstEntry().getKey();
    }

    /**
     * Gets the last key of a sequenced map.
     *
     * @param <K>
     *            key type
     * @param <V>
     *            value type
     * @param map
     *            the map
     * @return last key or null if empty
     */
    public static <K, V> K lastKey(SequencedMap<K, V> map) {
        return map == null || map.isEmpty() ? null : map.lastEntry().getKey();
    }

    /**
     * Gets the first value of a sequenced map.
     *
     * @param <K>
     *            key type
     * @param <V>
     *            value type
     * @param map
     *            the map
     * @return first value or null if empty
     */
    public static <K, V> V firstValue(SequencedMap<K, V> map) {
        return map == null || map.isEmpty() ? null : map.firstEntry().getValue();
    }

    /**
     * Gets the last value of a sequenced map.
     *
     * @param <K>
     *            key type
     * @param <V>
     *            value type
     * @param map
     *            the map
     * @return last value or null if empty
     */
    public static <K, V> V lastValue(SequencedMap<K, V> map) {
        return map == null || map.isEmpty() ? null : map.lastEntry().getValue();
    }

    // --- UTILITY METHODS ---

    /**
     * Returns an unmodifiable view of the list.
     *
     * @param <T>
     *            element type
     * @param list
     *            the list
     * @return unmodifiable view
     */
    public static <T> List<T> unmodifiable(List<T> list) {
        return list == null ? List.of() : List.copyOf(list);
    }

    /**
     * Returns an unmodifiable view of the set.
     *
     * @param <T>
     *            element type
     * @param set
     *            the set
     * @return unmodifiable view
     */
    public static <T> Set<T> unmodifiable(Set<T> set) {
        return set == null ? Set.of() : Set.copyOf(set);
    }

    /**
     * Returns an unmodifiable view of the map.
     *
     * @param <K>
     *            key type
     * @param <V>
     *            value type
     * @param map
     *            the map
     * @return unmodifiable view
     */
    public static <K, V> Map<K, V> unmodifiable(Map<K, V> map) {
        return map == null ? Map.of() : Map.copyOf(map);
    }

    /**
     * Creates a mutable copy of a list.
     *
     * @param <T>
     *            element type
     * @param list
     *            the list to copy
     * @return mutable ArrayList copy
     */
    public static <T> ArrayList<T> mutableCopy(Collection<? extends T> list) {
        return list == null ? new ArrayList<>() : new ArrayList<>(list);
    }

    /**
     * Creates a mutable copy of a set.
     *
     * @param <T>
     *            element type
     * @param set
     *            the set to copy
     * @return mutable HashSet copy
     */
    public static <T> HashSet<T> mutableSetCopy(Collection<? extends T> set) {
        return set == null ? new HashSet<>() : new HashSet<>(set);
    }

    /**
     * Creates a mutable copy of a map.
     *
     * @param <K>
     *            key type
     * @param <V>
     *            value type
     * @param map
     *            the map to copy
     * @return mutable HashMap copy
     */
    public static <K, V> HashMap<K, V> mutableCopy(Map<? extends K, ? extends V> map) {
        return map == null ? new HashMap<>() : new HashMap<>(map);
    }
}
