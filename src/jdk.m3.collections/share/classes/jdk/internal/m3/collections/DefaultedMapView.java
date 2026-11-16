/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.m3.collections;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;

/**
 * Key/value/slot-index lanes with no retained per-mapping Entry or Node.
 * Missing-key generation is either DefaultedMap-style (do not store) or
 * LazyMap-style (insert only after a successful callback).
 */
public final class DefaultedMapView<K, V> {
    static final long STATE_OCCUPIED = 1L << 0;
    static final long STATE_DELETED = 1L << 1;
    private Object[] keyLane = new Object[0];
    private Object[] valueLane = new Object[0];
    private int[] hashIndexLane = new int[0];
    private long[] stateMaskWords = {STATE_OCCUPIED};
    private int size;
    private Function<? super K, ? extends V> onMiss;
    private boolean storeOnMiss;

    public DefaultedMapView() { onMiss = key -> null; }

    public DefaultedMapView(V defaultValue) { onMiss = key -> defaultValue; }

    public DefaultedMapView(Function<? super K, ? extends V> factory, boolean lazy) {
        onMiss = Objects.requireNonNull(factory, "factory");
        storeOnMiss = lazy;
    }

    public static String scaffoldDescriptor() {
        return "M3DefaultedMapView|DEFAULTED_MAP|OBJECT|OCCUPIED,DELETED|Object[] keyLane;Object[] valueLane;int[] hashIndexLane";
    }

    private static int mix(Object key) {
        int h = key == null ? 0 : key.hashCode();
        return h ^ (h >>> 16);
    }

    /** Returns the physical slot or -1; probes stop at a genuinely empty bucket. */
    private int find(Object key) {
        if (hashIndexLane.length == 0) return -1;
        int mask = hashIndexLane.length - 1;
        int bucket = mix(key) & mask;
        for (int index; (index = hashIndexLane[bucket]) != 0; bucket = (bucket + 1) & mask) {
            int slot = index - 1;
            if (Objects.equals(key, keyLane[slot])) return slot;
        }
        return -1;
    }

    private void indexExisting(int slot) {
        int mask = hashIndexLane.length - 1;
        int bucket = mix(keyLane[slot]) & mask;
        while (hashIndexLane[bucket] != 0) bucket = (bucket + 1) & mask;
        hashIndexLane[bucket] = slot + 1;
    }

    private void rebuild(int capacity) {
        hashIndexLane = new int[capacity];
        for (int i = 0; i < size; i++) indexExisting(i);
    }

    private void reserve(int needed) {
        if (needed > keyLane.length) {
            long grown = keyLane.length == 0 ? 8L
                    : (long) keyLane.length + (keyLane.length >>> 1) + 1;
            int capacity = (int) Math.min(Integer.MAX_VALUE, Math.max(grown, needed));
            keyLane = Arrays.copyOf(keyLane, capacity);
            valueLane = Arrays.copyOf(valueLane, capacity);
        }
        if (hashIndexLane.length == 0 || (long) needed * 2 > hashIndexLane.length) {
            int capacity = hashIndexLane.length == 0 ? 16 : hashIndexLane.length;
            while ((long) needed * 2 > capacity) capacity <<= 1;
            rebuild(capacity);
        }
    }

    /** Missing-key callbacks never run for an existing null mapping. */
    @SuppressWarnings("unchecked")
    public V get(Object key) {
        int slot = find(key);
        if (slot >= 0) return (V) valueLane[slot];
        V missing = onMiss.apply((K) key);
        if (storeOnMiss) put((K) key, missing);
        return missing;
    }

    @SuppressWarnings("unchecked")
    public V put(K key, V value) {
        int slot = find(key);
        if (slot >= 0) {
            V old = (V) valueLane[slot];
            valueLane[slot] = value;
            return old;
        }
        reserve(Math.addExact(size, 1));
        keyLane[size] = key;
        valueLane[size] = value;
        indexExisting(size++);
        return null;
    }

    @SuppressWarnings("unchecked")
    public V remove(Object key) {
        int slot = find(key);
        if (slot < 0) return null;
        V old = (V) valueLane[slot];
        int last = --size;
        keyLane[slot] = keyLane[last];
        valueLane[slot] = valueLane[last];
        keyLane[last] = null;
        valueLane[last] = null;
        rebuild(hashIndexLane.length);
        return old;
    }

    public boolean containsKey(Object key) { return find(key) >= 0; }
    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }

    public void clear() {
        Arrays.fill(keyLane, 0, size, null);
        Arrays.fill(valueLane, 0, size, null);
        Arrays.fill(hashIndexLane, 0);
        size = 0;
    }
}
