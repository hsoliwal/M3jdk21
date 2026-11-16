/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.m3.collections;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Insertion-ordered, key-unique map stored as parallel arrays, without entries. */
public final class ListOrderedMapView<K, V> {
    static final long STATE_OCCUPIED = 1L << 0;
    static final long STATE_DELETED = 1L << 1;
    private Object[] keyLane = new Object[0];
    private Object[] valueLane = new Object[0];
    private int[] orderLane = new int[0];
    private int[] hashIndexLane = new int[0];
    private long[] stateMaskWords = new long[0];
    private int size;

    public ListOrderedMapView() { }

    public static String scaffoldDescriptor() {
        return "M3ListOrderedMapView|LIST_ORDERED_MAP|OBJECT|OCCUPIED,DELETED|Object[] keyLane;Object[] valueLane;int[] orderLane;int[] hashIndexLane";
    }

    private static boolean stable(Object x) {
        return x == null || x.getClass() == String.class || x.getClass() == Integer.class
                || x.getClass() == Long.class;
    }

    private static int hash(Object x) {
        int h = x == null ? 0 : x.hashCode();
        return h ^ (h >>> 16);
    }

    private void indexIfEligible() {
        if (hashIndexLane.length > 0 || size < 64 || size > (1 << 28)) return;
        for (int i = 0; i < size; i++) if (!stable(keyLane[i])) return;
        int capacity = 128;
        while (capacity < (long) size * 2) capacity <<= 1;
        int[] index = new int[capacity];
        for (int i = 0; i < size; i++) {
            int bucket = hash(keyLane[i]) & (capacity - 1);
            while (index[bucket] != 0) bucket = (bucket + 1) & (capacity - 1);
            index[bucket] = i + 1;
        }
        hashIndexLane = index;
    }

    public int indexOf(Object key) {
        if (stable(key)) {
            indexIfEligible();
            if (hashIndexLane.length != 0) {
                int mask = hashIndexLane.length - 1;
                int bucket = hash(key) & mask;
                for (int entry; (entry = hashIndexLane[bucket]) != 0;
                        bucket = (bucket + 1) & mask) {
                    if (Objects.equals(key, keyLane[entry - 1])) return entry - 1;
                }
                return -1;
            }
        }
        for (int i = 0; i < size; i++) if (Objects.equals(key, keyLane[i])) return i;
        return -1;
    }

    private void reserve(int required) {
        if (required <= keyLane.length) return;
        long capacity = keyLane.length == 0 ? 8L : (long) keyLane.length + (keyLane.length >>> 1) + 1;
        int length = (int) Math.min(Integer.MAX_VALUE, Math.max(capacity, required));
        keyLane = Arrays.copyOf(keyLane, length);
        valueLane = Arrays.copyOf(valueLane, length);
    }

    private void changed() { hashIndexLane = new int[0]; }

    @SuppressWarnings("unchecked")
    public V put(K key, V value) {
        int oldIndex = indexOf(key);
        if (oldIndex >= 0) {
            V oldValue = (V) valueLane[oldIndex];
            valueLane[oldIndex] = value;
            return oldValue;
        }
        reserve(Math.addExact(size, 1));
        boolean keepIndex = hashIndexLane.length != 0 && stable(key)
                && (long) (size + 1) * 2 <= hashIndexLane.length;
        keyLane[size] = key;
        valueLane[size++] = value;
        if (keepIndex) {
            int mask = hashIndexLane.length - 1;
            int bucket = hash(key) & mask;
            while (hashIndexLane[bucket] != 0) bucket = (bucket + 1) & mask;
            hashIndexLane[bucket] = size;
        } else {
            changed();
        }
        return null;
    }

    /** A previous key moves to the requested ordinal; earlier removal shifts it left. */
    @SuppressWarnings("unchecked")
    public V put(int index, K key, V value) {
        if (index < 0 || index > size) throw new IndexOutOfBoundsException(index);
        int previousIndex = indexOf(key);
        V previous = null;
        if (previousIndex >= 0) {
            previous = (V) valueLane[previousIndex];
            removeAt(previousIndex);
            if (previousIndex < index) index--;
        }
        reserve(Math.addExact(size, 1));
        System.arraycopy(keyLane, index, keyLane, index + 1, size - index);
        System.arraycopy(valueLane, index, valueLane, index + 1, size - index);
        keyLane[index] = key;
        valueLane[index] = value;
        size++;
        changed();
        return previous;
    }

    @SuppressWarnings("unchecked")
    public V get(Object key) {
        int index = indexOf(key);
        return index < 0 ? null : (V) valueLane[index];
    }

    @SuppressWarnings("unchecked")
    public K get(int index) { return (K) keyLane[Objects.checkIndex(index, size)]; }

    @SuppressWarnings("unchecked")
    public V getValue(int index) { return (V) valueLane[Objects.checkIndex(index, size)]; }

    @SuppressWarnings("unchecked")
    private V removeAt(int index) {
        V previous = (V) valueLane[index];
        System.arraycopy(keyLane, index + 1, keyLane, index, size - index - 1);
        System.arraycopy(valueLane, index + 1, valueLane, index, size - index - 1);
        keyLane[--size] = null;
        valueLane[size] = null;
        changed();
        return previous;
    }

    public V remove(int index) { Objects.checkIndex(index, size); return removeAt(index); }

    public V remove(Object key) {
        int index = indexOf(key);
        return index < 0 ? null : removeAt(index);
    }

    public List<K> keyList() {
        return Collections.unmodifiableList(new AbstractList<>() {
            @Override public K get(int index) { return ListOrderedMapView.this.get(index); }
            @Override public int size() { return ListOrderedMapView.this.size; }
        });
    }

    /** Live values with indexed set/remove but no index-changing insertion. */
    public List<V> valueList() {
        return new AbstractList<>() {
            @Override public V get(int index) { return getValue(index); }
            @Override public int size() { return ListOrderedMapView.this.size; }
            @SuppressWarnings("unchecked")
            @Override public V set(int index, V value) {
                Objects.checkIndex(index, ListOrderedMapView.this.size);
                V previous = (V) valueLane[index];
                valueLane[index] = value;
                return previous;
            }
            @Override public V remove(int index) { return ListOrderedMapView.this.remove(index); }
        };
    }

    public int size() { return size; }

    public void clear() {
        Arrays.fill(keyLane, 0, size, null);
        Arrays.fill(valueLane, 0, size, null);
        size = 0;
        changed();
    }
}
