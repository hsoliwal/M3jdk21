/* *****************************************************************************
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

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Immutable sorted map stored as parallel key/value arrays.
 *
 * <p>No tree node and no Map.Entry object is retained per mapping. Entry objects
 * are materialized only when a caller explicitly iterates {@link #entrySet()}.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/DenseSortedMap.java}
 * blob {@code e68cf0ba24c8c1cca6db59a99a246a26265bd204} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (common collections batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class DenseSortedMap<K extends Comparable<? super K>, V>
        extends AbstractMap<K, V> {
    private static final DenseSortedMap<?, ?> EMPTY =
            new DenseSortedMap<>(new Object[0], new Object[0]);

    private final Object[] keys;
    private final Object[] values;

    private DenseSortedMap(Object[] keys, Object[] values) {
        this.keys = keys;
        this.values = values;
    }

    @SuppressWarnings("unchecked")
    public static <K extends Comparable<? super K>, V> DenseSortedMap<K,V> empty() {
        return (DenseSortedMap<K,V>) EMPTY;
    }

    public static <K extends Comparable<? super K>, V> DenseSortedMap<K,V> copyOf(
            Map<? extends K, ? extends V> source) {
        Objects.requireNonNull(source, "source");
        if (source.isEmpty()) return empty();
        Object[] keys = source.keySet().toArray();
        Arrays.sort(keys);
        Object[] values = new Object[keys.length];
        for (int i = 0; i < keys.length; i++) {
            @SuppressWarnings("unchecked") K key = (K) keys[i];
            if (key == null) throw new NullPointerException("null key");
            values[i] = Objects.requireNonNull(source.get(key), "null value");
        }
        return new DenseSortedMap<>(keys, values);
    }

    @Override public int size() { return keys.length; }
    @Override public boolean isEmpty() { return keys.length == 0; }

    @Override public boolean containsKey(Object key) { return indexOf(key) >= 0; }

    @Override public V get(Object key) {
        int index = indexOf(key);
        return index < 0 ? null : valueAt(index);
    }

    /** Direct value-lane membership without entry materialization. */
    @Override public boolean containsValue(Object value) {
        for (Object retained : values) {
            if (Objects.equals(value, retained)) { return true; }
        }
        return false;
    }

    /** Standard Map equality over the retained lanes. */
    @Override public boolean equals(Object other) {
        if (other == this) { return true; }
        if (!(other instanceof Map<?, ?> map) || map.size() != keys.length) { return false; }
        try {
            for (int i = 0; i < keys.length; i++) {
                if (!Objects.equals(values[i], map.get(keys[i]))) { return false; }
            }
        } catch (ClassCastException | NullPointerException incompatible) {
            return false;
        }
        return true;
    }

    /** Standard Map hash without materializing entries. */
    @Override public int hashCode() {
        int hash = 0;
        for (int i = 0; i < keys.length; i++) {
            hash += Objects.hashCode(keys[i]) ^ Objects.hashCode(values[i]);
        }
        return hash;
    }

    /** Standard Map text in retained key order, without temporary entries. */
    @Override public String toString() {
        StringBuilder text = new StringBuilder("{");
        for (int i = 0; i < keys.length; i++) {
            if (i != 0) { text.append(", "); }
            text.append(keys[i] == this ? "(this Map)" : keys[i]).append('=')
                    .append(values[i] == this ? "(this Map)" : values[i]);
        }
        return text.append('}').toString();
    }

    @SuppressWarnings("unchecked")
    private int indexOf(Object key) {
        if (key == null) return -1;
        try {
            return Arrays.binarySearch(keys, (K) key);
        } catch (ClassCastException incompatible) {
            return -1;
        }
    }

    @SuppressWarnings("unchecked")
    private K keyAt(int index) { return (K) keys[index]; }

    @SuppressWarnings("unchecked")
    private V valueAt(int index) { return (V) values[index]; }

    /** Allocation-free traversal over the retained parallel arrays. */
    public void forEachKeyValue(BiConsumer<? super K, ? super V> consumer) {
        Objects.requireNonNull(consumer, "consumer");
        for (int i = 0; i < keys.length; i++) consumer.accept(keyAt(i), valueAt(i));
    }

    @Override public void forEach(BiConsumer<? super K, ? super V> consumer) {
        forEachKeyValue(consumer);
    }

    @Override public Set<K> keySet() {
        return new AbstractSet<>() {
            @Override public int size() { return keys.length; }
            @Override public boolean contains(Object key) { return containsKey(key); }
            @Override public Iterator<K> iterator() {
                return new Iterator<>() {
                    private int index;
                    @Override public boolean hasNext() { return index < keys.length; }
                    @Override public K next() {
                        if (!hasNext()) throw new NoSuchElementException();
                        return keyAt(index++);
                    }
                };
            }
        };
    }

    @Override public Set<Entry<K,V>> entrySet() {
        return new AbstractSet<>() {
            @Override public int size() { return keys.length; }
            @Override public Iterator<Entry<K,V>> iterator() {
                return new Iterator<>() {
                    private int index;
                    @Override public boolean hasNext() { return index < keys.length; }
                    @Override public Entry<K,V> next() {
                        if (!hasNext()) throw new NoSuchElementException();
                        int current = index++;
                        return new SimpleImmutableEntry<>(keyAt(current), valueAt(current));
                    }
                };
            }
        };
    }

    @Override public Collection<V> values() {
        return new java.util.AbstractCollection<>() {
            @Override public int size() { return values.length; }
            @Override public Iterator<V> iterator() {
                return new Iterator<>() {
                    private int index;
                    @Override public boolean hasNext() { return index < values.length; }
                    @Override public V next() {
                        if (!hasNext()) throw new NoSuchElementException();
                        return valueAt(index++);
                    }
                };
            }
        };
    }
}
