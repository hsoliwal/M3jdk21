/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.m3.collections;

import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import jdk.m3.collections.ObjectIntHashMap;

/**
 * Immutable encounter-ordered object map over private key/value lanes.
 *
 * <p>The existing object-to-int owner indexes keys to immutable ordinals.
 * {@link #copyOf(Map)} rejects null keys/values and duplicate equal keys, even
 * for a source map with identity semantics. Its null queries find no mapping.
 * {@link #copyOfNullableLanes(Object[], Object[], int)} instead snapshots
 * encounter-ordered lanes that may contain null keys and values. Facades
 * preserve the source encounter order and reject every mutator; entries are
 * fresh immutable projections created only when an entry is requested.
 *
 * <p>This implements the selected Map.copyOf/ImmutableMap operation contracts
 * of the existing internal M3 owner. It is not an ImmutableSortedMap, a Guava
 * interface implementation, or a replacement public JDK map. The JEP269 and
 * Guava provider lineage and original descriptor are retained as history.
 */
public final class FrozenMap<K, V> {
    static final long STATE_FROZEN = 1L << 0;
    private static final Object[] EMPTY = new Object[0];

    private final Object[] keyLane;
    private final Object[] valueLane;
    private final ObjectIntHashMap<Object> ordinals;
    private final long[] stateMaskWords;
    private final int size;
    private final boolean nullable;

    public FrozenMap() { this(EMPTY, EMPTY, new ObjectIntHashMap<>(0)); }

    private FrozenMap(Object[] keys, Object[] values, ObjectIntHashMap<Object> index) {
        this(keys, values, index, false);
    }

    private FrozenMap(Object[] keys, Object[] values, ObjectIntHashMap<Object> index,
            boolean nullable) {
        keyLane = keys;
        valueLane = values;
        ordinals = index;
        size = keys.length;
        this.nullable = nullable;
        int tail = size & 63;
        stateMaskWords = new long[(size >>> 6) + (tail == 0 ? 0 : 1)];
        Arrays.fill(stateMaskWords, -1L);
        if (tail != 0) stateMaskWords[stateMaskWords.length - 1] = (1L << tail) - 1;
    }

    /** Retained compatibility descriptor; lookup lanes are owned by ObjectIntHashMap. */
    public static String scaffoldDescriptor() {
        return "M3FrozenMap|FROZEN_MAP|OBJECT|FROZEN|Object[] keyLane;Object[] valueLane;int[] hashIndexLane";
    }

    public static <K, V> FrozenMap<K, V> copyOf(Map<? extends K, ? extends V> source) {
        Objects.requireNonNull(source, "source");
        // Exact JDK map classes have a trusted cardinality without arbitrary
        // user callbacks. Subclasses and other Map implementations instead
        // take the callback-neutral, grow-on-demand path.
        Class<?> type = source.getClass();
        int expected = type == HashMap.class || type == LinkedHashMap.class
                ? source.size() : 0;
        Object[] keys = expected == 0 ? EMPTY : new Object[expected];
        Object[] values = expected == 0 ? EMPTY : new Object[expected];
        int count = 0;
        // Keep the original hash-index growth schedule and all key callbacks;
        // capacity optimization applies to the two payload lanes only.
        ObjectIntHashMap<Object> index = new ObjectIntHashMap<>(0);
        for (Map.Entry<? extends K, ? extends V> entry : source.entrySet()) {
            Objects.requireNonNull(entry, "entry");
            K key = Objects.requireNonNull(entry.getKey(), "key");
            V value = Objects.requireNonNull(entry.getValue(), "value");
            if (index.getOrDefault(key, -1) >= 0) {
                throw new IllegalArgumentException("duplicate equal key");
            }
            index.put(key, count);
            if (count == keys.length) {
                long capacity = count == 0 ? 8L
                        : (long) count + (count >>> 1) + 1;
                if (capacity > Integer.MAX_VALUE - 8L) {
                    throw new OutOfMemoryError("FrozenMap lane capacity exhausted");
                }
                // Stage both private lanes before publishing either one.
                Object[] grownKeys = Arrays.copyOf(keys, (int) capacity);
                Object[] grownValues = Arrays.copyOf(values, (int) capacity);
                keys = grownKeys;
                values = grownValues;
            }
            keys[count] = key;
            values[count] = value;
            count++;
        }
        return new FrozenMap<>(count == keys.length ? keys : Arrays.copyOf(keys, count),
                count == values.length ? values : Arrays.copyOf(values, count), index);
    }

    /**
     * Snapshot the first {@code size} entries of parallel key/value lanes.
     * Null keys and values are preserved; duplicate Java-equal keys, including
     * a second null key, are rejected. Encounter order is the lane order.
     * Both bounds are checked and both lanes are copied before any key hash or
     * equality callback. Later edits to the supplied arrays cannot change the
     * result. Key objects retain the ordinary stable equality/hash contract.
     *
     * <p>This additive nullable factory preserves the strict null rejection and
     * callback schedule of {@link #copyOf(Map)}. Storage remains two private
     * Object[] lanes and the existing primitive ordinal directory; entries are
     * immutable projections only when requested.</p>
     *
     * @param keys keys in encounter order
     * @param values aligned mapped values
     * @param size number of entries to snapshot
     * @param <K> key type
     * @param <V> mapped value type
     * @return an immutable nullable map owner
     * @throws NullPointerException if either lane is null
     * @throws IndexOutOfBoundsException if size is negative or exceeds either lane
     * @throws IllegalArgumentException if two keys are Java-equal
     */
    public static <K, V> FrozenMap<K, V> copyOfNullableLanes(
            Object[] keys, Object[] values, int size) {
        Objects.requireNonNull(keys, "keys");
        Objects.requireNonNull(values, "values");
        Objects.checkFromIndexSize(0, size, keys.length);
        Objects.checkFromIndexSize(0, size, values.length);
        Object[] keySnapshot = Arrays.copyOf(keys, size);
        Object[] valueSnapshot = Arrays.copyOf(values, size);
        ObjectIntHashMap<Object> index = new ObjectIntHashMap<>(0);
        for (int ordinal = 0; ordinal < size; ordinal++) {
            Object key = keySnapshot[ordinal];
            if (index.getOrDefault(key, -1) >= 0) {
                throw new IllegalArgumentException("duplicate equal key");
            }
            index.put(key, ordinal);
        }
        return new FrozenMap<>(keySnapshot, valueSnapshot, index, true);
    }

    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }
    private int ordinal(Object key) {
        return key == null && !nullable ? -1 : ordinals.getOrDefault(key, -1);
    }
    public boolean containsKey(Object key) { return ordinal(key) >= 0; }

    public boolean containsValue(Object value) {
        if (value == null && nullable) {
            for (Object element : valueLane) if (element == null) return true;
        }
        if (value == null) return false;
        for (Object element : valueLane) if (value.equals(element)) return true;
        return false;
    }

    public V get(Object key) { return getOrDefault(key, null); }

    public V getOrDefault(Object key, V defaultValue) {
        int ordinal = ordinal(key);
        return ordinal < 0 ? defaultValue : valueAt(ordinal);
    }

    @SuppressWarnings("unchecked")
    public K keyAt(int ordinal) { return (K) keyLane[Objects.checkIndex(ordinal, size)]; }

    @SuppressWarnings("unchecked")
    public V valueAt(int ordinal) { return (V) valueLane[Objects.checkIndex(ordinal, size)]; }

    public Set<Map.Entry<K, V>> entrySet() {
        return Collections.unmodifiableSet(new AbstractSet<>() {
            @Override public int size() { return size; }
            @Override public boolean contains(Object candidate) {
                if (!(candidate instanceof Map.Entry<?, ?> entry)) return false;
                int ordinal = ordinal(entry.getKey());
                return ordinal >= 0 && Objects.equals(valueLane[ordinal], entry.getValue());
            }
            @Override public Iterator<Map.Entry<K, V>> iterator() {
                return new Iterator<>() {
                    private int cursor;
                    @Override public boolean hasNext() { return cursor < size; }
                    @Override public Map.Entry<K, V> next() {
                        if (!hasNext()) throw new NoSuchElementException();
                        int ordinal = cursor++;
                        return new AbstractMap.SimpleImmutableEntry<>(keyAt(ordinal), valueAt(ordinal));
                    }
                };
            }
            @Override public Spliterator<Map.Entry<K, V>> spliterator() {
                return Spliterators.spliterator(iterator(), size,
                        Spliterator.ORDERED | Spliterator.DISTINCT | Spliterator.IMMUTABLE | Spliterator.NONNULL);
            }
        });
    }

    public Map<K, V> asMap() {
        return Collections.unmodifiableMap(new AbstractMap<>() {
            @Override public int size() { return size; }
            @Override public V get(Object key) { return FrozenMap.this.get(key); }
            @Override public V getOrDefault(Object key, V defaultValue) { return FrozenMap.this.getOrDefault(key, defaultValue); }
            @Override public boolean containsKey(Object key) { return FrozenMap.this.containsKey(key); }
            @Override public boolean containsValue(Object value) { return FrozenMap.this.containsValue(value); }
            @Override public Set<Map.Entry<K, V>> entrySet() { return FrozenMap.this.entrySet(); }
            @Override public Set<K> keySet() {
                return Collections.unmodifiableSet(new AbstractSet<>() {
                    @Override public int size() { return size; }
                    @Override public boolean contains(Object key) { return FrozenMap.this.containsKey(key); }
                    @Override public Iterator<K> iterator() {
                        return new Iterator<>() {
                            private int cursor;
                            @Override public boolean hasNext() { return cursor < size; }
                            @Override public K next() {
                                if (!hasNext()) throw new NoSuchElementException();
                                return keyAt(cursor++);
                            }
                        };
                    }
                    @Override public Spliterator<K> spliterator() {
                        return Spliterators.spliterator(iterator(), size,
                                Spliterator.ORDERED | Spliterator.DISTINCT | Spliterator.IMMUTABLE
                                        | (nullable ? 0 : Spliterator.NONNULL));
                    }
                });
            }
            @Override public Collection<V> values() {
                return Collections.unmodifiableCollection(new AbstractCollection<>() {
                    @Override public int size() { return size; }
                    @Override public boolean contains(Object value) { return FrozenMap.this.containsValue(value); }
                    @Override public Iterator<V> iterator() {
                        return new Iterator<>() {
                            private int cursor;
                            @Override public boolean hasNext() { return cursor < size; }
                            @Override public V next() {
                                if (!hasNext()) throw new NoSuchElementException();
                                return valueAt(cursor++);
                            }
                        };
                    }
                    @Override public Spliterator<V> spliterator() {
                        return Spliterators.spliterator(iterator(), size,
                                Spliterator.ORDERED | Spliterator.IMMUTABLE
                                        | (nullable ? 0 : Spliterator.NONNULL));
                    }
                });
            }
        });
    }

    public V put(K key, V value) {
        throw new UnsupportedOperationException("immutable map");
    }

    public V remove(Object key) {
        throw new UnsupportedOperationException("immutable map");
    }
}
