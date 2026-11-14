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

import java.io.Closeable;
import java.io.File;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
/**
 * ChronicleMapLazy - Fallback implementation using ConcurrentHashMap.
 *
 * <p>
 * This is a fallback when Chronicle Map is not available. Uses
 * ConcurrentHashMap for in-memory storage. Does NOT provide off-heap
 * persistence.
 * </p>
 *
 * @param <K>
 *            key type
 * @param <V>
 *            value type
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/lazy/ChronicleMapLazy.java}
 * blob {@code 72103e04f4ebe3dd202939850e2a20234273351f} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common batch b29 (slf4j owners on the PlaceholderLogger facade) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class ChronicleMapLazy<K, V> implements Closeable {
    private static final PlaceholderLogger log = PlaceholderLogger.of(ChronicleMapLazy.class);


    /** Default number of entries. */
    private static final long DEFAULT_ENTRIES = 1000L;

    private final ConcurrentHashMap<K, V> map;
    private final File persistFile;

    private ChronicleMapLazy(long entries, File persistFile) {
        // Use entries as initial capacity hint
        int initialCapacity = (int) Math.min(entries, Integer.MAX_VALUE);
        this.map = new ConcurrentHashMap<>(initialCapacity);
        this.persistFile = persistFile;
    }

    public static <K, V> Builder<K, V> builder() {
        return new Builder<>();
    }

    /** Creates an in-memory map with defaults. */
    public static <K, V> ChronicleMapLazy<K, V> create(Class<K> keyClass, Class<V> valueClass, long entries) {
        return ChronicleMapLazy.<K, V>builder()
                .keyClass(keyClass)
                .valueClass(valueClass)
                .entries(entries)
                .build();
    }

    /** Creates a persisted map (fallback: in-memory only). */
    public static <K, V> ChronicleMapLazy<K, V> createPersisted(
            Class<K> keyClass, Class<V> valueClass, long entries, File file) {
        return ChronicleMapLazy.<K, V>builder()
                .keyClass(keyClass)
                .valueClass(valueClass)
                .entries(entries)
                .persistTo(file)
                .build();
    }

    /**
     * Gets a value by key.
     *
     * @param key
     *            the key
     * @return the value or null
     */
    public V get(K key) {
        return map.get(key);
    }

    /**
     * Gets an optional value by key.
     *
     * @param key
     *            the key
     * @return optional value
     */
    public Optional<V> getOptional(K key) {
        return Optional.ofNullable(map.get(key));
    }

    /**
     * Gets or computes a value.
     *
     * @param key
     *            the key
     * @param compute
     *            compute function
     * @return the value
     */
    public V getOrCompute(K key, Function<K, V> compute) {
        return map.computeIfAbsent(key, compute);
    }

    /**
     * Puts a value.
     *
     * @param key
     *            the key
     * @param value
     *            the value
     * @return previous value
     */
    public V put(K key, V value) {
        return map.put(key, value);
    }

    /**
     * Puts a value if absent.
     *
     * @param key
     *            the key
     * @param value
     *            the value
     * @return existing value or null
     */
    public V putIfAbsent(K key, V value) {
        return map.putIfAbsent(key, value);
    }

    /**
     * Removes a value.
     *
     * @param key
     *            the key
     * @return removed value
     */
    public V remove(K key) {
        return map.remove(key);
    }

    /**
     * Checks if key exists.
     *
     * @param key
     *            the key
     * @return true if exists
     */
    public boolean containsKey(K key) {
        return map.containsKey(key);
    }

    /**
     * Gets the map size.
     *
     * @return size
     */
    public long size() {
        return map.size();
    }

    /**
     * Checks if empty.
     *
     * @return true if empty
     */
    public boolean isEmpty() {
        return map.isEmpty();
    }

    /** Clears the map. */
    public void clear() {
        map.clear();
    }

    /**
     * Iterates over entries.
     *
     * @param action
     *            action to perform
     */
    public void forEach(BiConsumer<K, V> action) {
        map.forEach(action);
    }

    /** Gets underlying map (ConcurrentHashMap in fallback mode). */
    public Map<K, V> underlying() {
        return map;
    }

    /**
     * Returns the map.
     *
     * @return map
     */
    public Map<K, V> asMap() {
        return map;
    }

    /**
     * Gets the persist file.
     *
     * @return optional file
     */
    public Optional<File> getPersistFile() {
        return Optional.ofNullable(persistFile);
    }

    /** Returns estimated heap memory (fallback has no off-heap). */
    public long offHeapMemoryUsed() {
        return 0; // Fallback uses heap, not off-heap
    }

    @Override
    public void close() {
        map.clear();
    }

    /**
     * Builder for ChronicleMapLazy.
     *
     * @param <K>
     *            key type
     * @param <V>
     *            value type
     */
    public static final class Builder<K, V> {

        // INTERNED STRING CONSTANTS
        private static final TokenInterner INTERNER = TokenInterner.getInstance();
        private static final String ERR_KEY_CLASS = INTERNER.intern("Key class must be set");
        private static final String ERR_VALUE_CLASS = INTERNER.intern("Value class must be set");

        private Class<K> keyClass;
        private Class<V> valueClass;
        private long entries = DEFAULT_ENTRIES;
        private int averageKeySize = -1;
        private int averageValueSize = -1;
        private File persistFile = null;
        private String name = null;
        private boolean putReturnsNull = false;
        private boolean removeReturnsNull = false;

        private Builder() {}

        /**
         * Sets the key class.
         *
         * @param keyClass
         *            key class
         * @return this builder
         */
        public Builder<K, V> keyClass(Class<K> keyClass) {
            this.keyClass = keyClass;
            return this;
        }

        /**
         * Sets the value class.
         *
         * @param valueClass
         *            value class
         * @return this builder
         */
        public Builder<K, V> valueClass(Class<V> valueClass) {
            this.valueClass = valueClass;
            return this;
        }

        /**
         * Sets the number of entries.
         *
         * @param entries
         *            number of entries
         * @return this builder
         */
        public Builder<K, V> entries(long entries) {
            this.entries = entries;
            return this;
        }

        /**
         * Sets the average key size.
         *
         * @param size
         *            average key size
         * @return this builder
         */
        public Builder<K, V> averageKeySize(int size) {
            this.averageKeySize = size;
            return this;
        }

        /**
         * Sets the average value size.
         *
         * @param size
         *            average value size
         * @return this builder
         */
        public Builder<K, V> averageValueSize(int size) {
            this.averageValueSize = size;
            return this;
        }

        /**
         * Sets the persist file.
         *
         * @param file
         *            persist file
         * @return this builder
         */
        public Builder<K, V> persistTo(File file) {
            this.persistFile = file;
            return this;
        }

        /**
         * Sets the name.
         *
         * @param name
         *            name
         * @return this builder
         */
        public Builder<K, V> name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets put to return null.
         *
         * @return this builder
         */
        public Builder<K, V> putReturnsNull() {
            this.putReturnsNull = true;
            return this;
        }

        /**
         * Sets remove to return null.
         *
         * @return this builder
         */
        public Builder<K, V> removeReturnsNull() {
            this.removeReturnsNull = true;
            return this;
        }

        /**
         * Builds the map.
         *
         * @return chronicle map lazy
         */
        public ChronicleMapLazy<K, V> build() {
            if (keyClass == null) {
                throw new IllegalStateException(ERR_KEY_CLASS);
            }
            if (valueClass == null) {
                throw new IllegalStateException(ERR_VALUE_CLASS);
            }
            return new ChronicleMapLazy<>(entries, persistFile);
        }
    }

    /** Creates a String-to-String map. */
    public static ChronicleMapLazy<String, String> stringMap(long entries, int averageKeySize, int averageValueSize) {
        return ChronicleMapLazy.<String, String>builder()
                .keyClass(String.class)
                .valueClass(String.class)
                .entries(entries)
                .averageKeySize(averageKeySize)
                .averageValueSize(averageValueSize)
                .build();
    }

    /** Creates a String-to-byte[] map. */
    public static ChronicleMapLazy<String, byte[]> bytesMap(long entries, int averageKeySize, int averageValueSize) {
        return ChronicleMapLazy.<String, byte[]>builder()
                .keyClass(String.class)
                .valueClass(byte[].class)
                .entries(entries)
                .averageKeySize(averageKeySize)
                .averageValueSize(averageValueSize)
                .build();
    }

    /** Creates a Long-to-Long map. */
    public static ChronicleMapLazy<Long, Long> longMap(long entries) {
        return ChronicleMapLazy.<Long, Long>builder()
                .keyClass(Long.class)
                .valueClass(Long.class)
                .entries(entries)
                .build();
    }
}
