/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.m3.collections;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiConsumer;
import jdk.m3.collections.ObjectIntHashMap;

/**
 * Immutable encounter-ordered bijection over private key/value lanes.
 *
 * <p>The existing object-to-int owner supplies each direction's ordinal index.
 * Construction rejects nulls and duplicate equal keys or values. Null lookup
 * finds no mapping. A cached reciprocal inverse shares both payload lanes and
 * both indexes, preserving the source entry order in either direction.
 *
 * <p>The selected Guava ImmutableBiMap lineage is retained from the existing
 * FROZEN_BIMAP owner and LIBRARY_API_MATRIX.tsv, run
 * 20261017-m3-to-public/LIB-SUPERSET. This class implements that frozen bijection
 * contract; it does not implement a Guava interface or replace a public JDK map.
 * Map entries are fresh immutable projections, never retained payload objects.
 */
public final class FrozenBiMap<K, V> {
    static final long STATE_FROZEN = 1L << 0;
    private static final Object[] EMPTY = new Object[0];

    private final Object[] keyLane;
    private final Object[] valueLane;
    private final ObjectIntHashMap<Object> forwardIndex;
    private final ObjectIntHashMap<Object> inverseIndex;
    private final long[] stateMaskWords;
    private final int size;
    private final FrozenBiMap<V, K> inverse;

    public FrozenBiMap() {
        this(EMPTY, EMPTY, new ObjectIntHashMap<>(0), new ObjectIntHashMap<>(0), null);
    }

    @SuppressWarnings("unchecked")
    private FrozenBiMap(Object[] keys, Object[] values,
            ObjectIntHashMap<Object> forward, ObjectIntHashMap<Object> reverse,
            FrozenBiMap<V, K> inverseOf) {
        keyLane = keys;
        valueLane = values;
        forwardIndex = forward;
        inverseIndex = reverse;
        size = keys.length;
        if (inverseOf != null) {
            stateMaskWords = inverseOf.stateMaskWords;
        } else {
            int tail = size & 63;
            stateMaskWords = new long[(size >>> 6) + (tail == 0 ? 0 : 1)];
            Arrays.fill(stateMaskWords, -1L);
            if (tail != 0) stateMaskWords[stateMaskWords.length - 1] = (1L << tail) - 1;
        }
        if (inverseOf != null) {
            inverse = inverseOf;
        } else if (size == 0) {
            // No element can cross the generic domains of the empty owner.
            inverse = (FrozenBiMap<V, K>) (FrozenBiMap<?, ?>) this;
        } else {
            inverse = new FrozenBiMap<>(values, keys, reverse, forward, this);
        }
    }

    /** Retained compatibility descriptor; each index owns its integer lanes. */
    public static String scaffoldDescriptor() {
        return "M3FrozenBiMap|FROZEN_BIMAP|OBJECT|FROZEN|Object[] keyLane;Object[] valueLane;int[] forwardIndexLane;int[] inverseIndexLane";
    }

    public static <K, V> FrozenBiMap<K, V> copyOf(Map<? extends K, ? extends V> source) {
        Objects.requireNonNull(source, "source");
        ArrayList<Object> keys = new ArrayList<>();
        ArrayList<Object> values = new ArrayList<>();
        ObjectIntHashMap<Object> forward = new ObjectIntHashMap<>(0);
        ObjectIntHashMap<Object> reverse = new ObjectIntHashMap<>(0);
        for (Map.Entry<? extends K, ? extends V> entry : source.entrySet()) {
            Objects.requireNonNull(entry, "entry");
            K key = Objects.requireNonNull(entry.getKey(), "key");
            V value = Objects.requireNonNull(entry.getValue(), "value");
            if (forward.getOrDefault(key, -1) >= 0) {
                throw new IllegalArgumentException("duplicate equal key");
            }
            if (reverse.getOrDefault(value, -1) >= 0) {
                throw new IllegalArgumentException("duplicate equal value");
            }
            int ordinal = keys.size();
            forward.put(key, ordinal);
            reverse.put(value, ordinal);
            keys.add(key);
            values.add(value);
        }
        return new FrozenBiMap<>(keys.toArray(), values.toArray(), forward, reverse, null);
    }

    public int size() { return size; }

    private int ordinal(Object key) {
        return key == null ? -1 : forwardIndex.getOrDefault(key, -1);
    }

    public boolean containsKey(Object key) { return ordinal(key) >= 0; }
    public boolean containsValue(Object value) { return inverse.containsKey(value); }

    public V get(Object key) {
        int ordinal = ordinal(key);
        return ordinal < 0 ? null : valueAt(ordinal);
    }

    public K inverseGet(Object value) { return inverse.get(value); }
    public FrozenBiMap<V, K> inverse() { return inverse; }

    @SuppressWarnings("unchecked")
    private K keyAt(int ordinal) { return (K) keyLane[ordinal]; }

    @SuppressWarnings("unchecked")
    private V valueAt(int ordinal) { return (V) valueLane[ordinal]; }

    private Set<K> keyView() {
        return Collections.unmodifiableSet(new AbstractSet<>() {
            @Override public int size() { return size; }
            @Override public boolean contains(Object key) { return containsKey(key); }
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
                        Spliterator.ORDERED | Spliterator.DISTINCT
                                | Spliterator.IMMUTABLE | Spliterator.NONNULL);
            }
        });
    }

    private Set<Map.Entry<K, V>> entries() {
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
                        Spliterator.ORDERED | Spliterator.DISTINCT
                                | Spliterator.IMMUTABLE | Spliterator.NONNULL);
            }
        });
    }

    public Map<K, V> asMap() {
        return Collections.unmodifiableMap(new AbstractMap<>() {
            @Override public int size() { return size; }
            @Override public V get(Object key) { return FrozenBiMap.this.get(key); }
            @Override public V getOrDefault(Object key, V defaultValue) {
                int ordinal = ordinal(key);
                return ordinal < 0 ? defaultValue : valueAt(ordinal);
            }
            @Override public boolean containsKey(Object key) { return FrozenBiMap.this.containsKey(key); }
            @Override public boolean containsValue(Object value) { return FrozenBiMap.this.containsValue(value); }
            @Override public Set<Map.Entry<K, V>> entrySet() { return entries(); }
            @Override public Set<K> keySet() { return keyView(); }
            @Override public Collection<V> values() { return inverse.keyView(); }
            @Override public void forEach(BiConsumer<? super K, ? super V> action) {
                Objects.requireNonNull(action, "action");
                for (int ordinal = 0; ordinal < size; ordinal++) {
                    action.accept(keyAt(ordinal), valueAt(ordinal));
                }
            }
        });
    }

    public V put(K key, V value) {
        throw new UnsupportedOperationException("immutable bimap");
    }

    public V forcePut(K key, V value) {
        throw new UnsupportedOperationException("immutable bimap");
    }

    public V remove(Object key) {
        throw new UnsupportedOperationException("immutable bimap");
    }
}
