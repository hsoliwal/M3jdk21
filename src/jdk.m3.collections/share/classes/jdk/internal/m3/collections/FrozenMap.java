/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.m3.collections;

import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
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
 * Construction rejects null keys/values and duplicate equal keys, even for a
 * source map with identity semantics. Null queries find no mapping. Facades
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
    private static final long[] FROZEN_STATE = {STATE_FROZEN};
    private static final Object[] EMPTY = new Object[0];

    private final Object[] keyLane;
    private final Object[] valueLane;
    private final ObjectIntHashMap<Object> ordinals;
    private final long[] stateMaskWords = FROZEN_STATE;
    private final int size;

    public FrozenMap() { this(EMPTY, EMPTY, new ObjectIntHashMap<>(0)); }

    private FrozenMap(Object[] keys, Object[] values, ObjectIntHashMap<Object> index) {
        keyLane = keys;
        valueLane = values;
        ordinals = index;
        size = keys.length;
    }

    /** Retained compatibility descriptor; lookup lanes are owned by ObjectIntHashMap. */
    public static String scaffoldDescriptor() {
        return "M3FrozenMap|FROZEN_MAP|OBJECT|FROZEN|Object[] keyLane;Object[] valueLane;int[] hashIndexLane";
    }

    public static <K, V> FrozenMap<K, V> copyOf(Map<? extends K, ? extends V> source) {
        Objects.requireNonNull(source, "source");
        ArrayList<Object> keys = new ArrayList<>();
        ArrayList<Object> values = new ArrayList<>();
        ObjectIntHashMap<Object> index = new ObjectIntHashMap<>(0);
        for (Map.Entry<? extends K, ? extends V> entry : source.entrySet()) {
            Objects.requireNonNull(entry, "entry");
            K key = Objects.requireNonNull(entry.getKey(), "key");
            V value = Objects.requireNonNull(entry.getValue(), "value");
            if (index.getOrDefault(key, -1) >= 0) {
                throw new IllegalArgumentException("duplicate equal key");
            }
            index.put(key, keys.size());
            keys.add(key);
            values.add(value);
        }
        return new FrozenMap<>(keys.toArray(), values.toArray(), index);
    }

    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }
    private int ordinal(Object key) { return key == null ? -1 : ordinals.getOrDefault(key, -1); }
    public boolean containsKey(Object key) { return ordinal(key) >= 0; }

    public boolean containsValue(Object value) {
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
                                Spliterator.ORDERED | Spliterator.DISTINCT | Spliterator.IMMUTABLE | Spliterator.NONNULL);
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
                                Spliterator.ORDERED | Spliterator.IMMUTABLE | Spliterator.NONNULL);
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
