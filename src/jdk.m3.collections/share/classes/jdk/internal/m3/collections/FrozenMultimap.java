/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.m3.collections;

import java.util.AbstractCollection;
import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.Set;
import jdk.m3.collections.ObjectIntHashMap;

/**
 * Immutable object multimap with encounter-ordered keys and packed value runs.
 *
 * <p>The existing object-to-int owner indexes keys to CSR run ordinals. This
 * owner retains only the key, offset and value lanes, frozen state bits, and
 * that index. Run collections and immutable entries are projected on demand;
 * no collection or entry object is retained for each key or mapping.
 *
 * <p>Copying preserves source-map encounter order and the order and duplicates
 * within each value collection. Empty runs are omitted. Null keys, collections
 * and values are rejected. Queries for null or missing keys find no mappings.
 * All exposed collections are immutable, including empty results. A copy owns
 * its structural arrays; later source collection mutations cannot change it.
 * This is the existing internal M3 API, not a Guava interface implementation.
 */
public final class FrozenMultimap<K, V> {
    static final long STATE_FROZEN = 1L;
    private final Object[] keyLane;
    private final int[] valueOffsetLane;
    private final Object[] valueLane;
    private final long[] stateMaskWords;
    private final ObjectIntHashMap<Object> keyOrdinals;
    private final int size;

    /** Creates a valid immutable empty owner. */
    public FrozenMultimap() {
        this(new Object[0], new int[] {0}, new Object[0], new ObjectIntHashMap<>(0));
    }

    private FrozenMultimap(Object[] keys, int[] offsets, Object[] values,
            ObjectIntHashMap<Object> ordinals) {
        keyLane = keys;
        valueOffsetLane = offsets;
        valueLane = values;
        keyOrdinals = ordinals;
        size = values.length;
        stateMaskWords = new long[(int) (((long) keys.length + 63) >>> 6)];
        Arrays.fill(stateMaskWords, -1L);
        if ((keys.length & 63) != 0) {
            stateMaskWords[stateMaskWords.length - 1] = (1L << (keys.length & 63)) - 1;
        }
    }

    /** Retained compatibility descriptor; it does not describe admission status. */
    public static String scaffoldDescriptor() {
        return "M3FrozenMultimap|FROZEN_MULTIMAP|OBJECT|FROZEN|Object[] keyLane;int[] valueOffsetLane;Object[] valueLane;int[] hashIndexLane";
    }

    /**
     * Copies each input run once. Construction remains private until every
     * collection traversal, key callback, validation and allocation succeeds.
     * Equal nonempty keys from a nonstandard source map are rejected.
     */
    public static <K, V> FrozenMultimap<K, V> copyOf(
            Map<? extends K, ? extends Collection<? extends V>> source) {
        Objects.requireNonNull(source, "source");
        ArrayList<K> keys = new ArrayList<>();
        ArrayList<V> values = new ArrayList<>();
        ObjectIntHashMap<Object> ordinals = new ObjectIntHashMap<>(0);
        int[] offsets = new int[8];
        for (Map.Entry<? extends K, ? extends Collection<? extends V>> entry
                : source.entrySet()) {
            K key = Objects.requireNonNull(entry.getKey(), "key");
            Collection<? extends V> run = Objects.requireNonNull(entry.getValue(), "values");
            int start = values.size();
            for (V value : run) {
                values.add(Objects.requireNonNull(value, "value"));
            }
            if (values.size() == start) {
                continue;
            }
            int ordinal = keys.size();
            if (ordinals.containsKey(key)) {
                throw new IllegalArgumentException("duplicate equal key");
            }
            ordinals.put(key, ordinal);
            keys.add(key);
            if (keys.size() == offsets.length) {
                int capacity = Math.toIntExact(Math.min(Integer.MAX_VALUE - 8L,
                        Math.max((long) keys.size() + 1,
                                (long) offsets.length + (offsets.length >>> 1) + 1)));
                if (capacity <= keys.size()) {
                    throw new OutOfMemoryError("FrozenMultimap offset capacity exhausted");
                }
                offsets = Arrays.copyOf(offsets, capacity);
            }
            offsets[keys.size()] = values.size();
        }
        return new FrozenMultimap<>(keys.toArray(),
                Arrays.copyOf(offsets, keys.size() + 1), values.toArray(), ordinals);
    }

    public int size() { return size; }
    public int keyCount() { return keyLane.length; }

    public boolean containsKey(Object key) {
        return keyOrdinals.containsKey(key);
    }

    public boolean containsEntry(Object key, Object value) {
        int ordinal = keyOrdinals.getOrDefault(key, -1);
        if (ordinal < 0 || value == null) {
            return false;
        }
        for (int index = valueOffsetLane[ordinal]; index < valueOffsetLane[ordinal + 1]; index++) {
            if (Objects.equals(value, valueLane[index])) {
                return true;
            }
        }
        return false;
    }

    public Collection<V> get(K key) {
        int ordinal = keyOrdinals.getOrDefault(key, -1);
        return ordinal < 0 ? Collections.unmodifiableList(Collections.emptyList()) : run(ordinal);
    }

    private List<V> run(int ordinal) {
        return Collections.unmodifiableList(new Run(ordinal));
    }

    private final class Run extends AbstractList<V> implements RandomAccess {
        private final int start;
        private final int length;
        Run(int ordinal) {
            start = valueOffsetLane[ordinal];
            length = valueOffsetLane[ordinal + 1] - start;
        }
        @Override public int size() { return length; }
        @Override public V get(int index) {
            return valueAt(start + Objects.checkIndex(index, length));
        }
    }

    public Set<K> keySet() {
        return Collections.unmodifiableSet(new AbstractSet<>() {
            @Override public int size() { return keyLane.length; }
            @Override public boolean contains(Object key) { return containsKey(key); }
            @Override public Iterator<K> iterator() {
                return new Iterator<>() {
                    private int cursor;
                    @Override public boolean hasNext() { return cursor < keyLane.length; }
                    @Override public K next() {
                        if (!hasNext()) { throw new NoSuchElementException(); }
                        return keyAt(cursor++);
                    }
                };
            }
        });
    }

    public Map<K, Collection<V>> asMap() {
        return Collections.unmodifiableMap(new AbstractMap<>() {
            @Override public int size() { return keyLane.length; }
            @Override public boolean containsKey(Object key) { return FrozenMultimap.this.containsKey(key); }
            @Override public Collection<V> get(Object key) {
                int ordinal = keyOrdinals.getOrDefault(key, -1);
                return ordinal < 0 ? null : run(ordinal);
            }
            @Override public Set<K> keySet() { return FrozenMultimap.this.keySet(); }
            @Override public Set<Map.Entry<K, Collection<V>>> entrySet() {
                return Collections.unmodifiableSet(new AbstractSet<>() {
                    @Override public int size() { return keyLane.length; }
                    @Override public Iterator<Map.Entry<K, Collection<V>>> iterator() {
                        return new Iterator<>() {
                            private int cursor;
                            @Override public boolean hasNext() { return cursor < keyLane.length; }
                            @Override public Map.Entry<K, Collection<V>> next() {
                                if (!hasNext()) { throw new NoSuchElementException(); }
                                int ordinal = cursor++;
                                return new AbstractMap.SimpleImmutableEntry<>(keyAt(ordinal), run(ordinal));
                            }
                        };
                    }
                });
            }
        });
    }

    /** Grouped encounter order, with a fresh immutable entry at the API boundary. */
    public Collection<Map.Entry<K, V>> entries() {
        return Collections.unmodifiableCollection(new AbstractCollection<>() {
            @Override public int size() { return size; }
            @Override public Iterator<Map.Entry<K, V>> iterator() {
                return new Iterator<>() {
                    private int key;
                    private int value;
                    @Override public boolean hasNext() { return value < size; }
                    @Override public Map.Entry<K, V> next() {
                        if (!hasNext()) { throw new NoSuchElementException(); }
                        while (value >= valueOffsetLane[key + 1]) { key++; }
                        return new AbstractMap.SimpleImmutableEntry<>(keyAt(key), valueAt(value++));
                    }
                };
            }
        });
    }

    @SuppressWarnings("unchecked")
    private K keyAt(int ordinal) { return (K) keyLane[ordinal]; }
    @SuppressWarnings("unchecked")
    private V valueAt(int ordinal) { return (V) valueLane[ordinal]; }

    public boolean put(K key, V value) {
        throw new UnsupportedOperationException("immutable multimap");
    }

    public Collection<V> removeAll(Object key) {
        throw new UnsupportedOperationException("immutable multimap");
    }
}
