/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.m3.collections;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

/** Live ordered composition of maps without storing a second copy of their keys. */
public final class CompositeMapView<K, V> {
    static final long STATE_SELECTED = 1L << 0;
    private Object[] backingLane = new Object[0];
    private long[] stateMaskWords = new long[0];
    private int size;

    public CompositeMapView() { }

    public static String scaffoldDescriptor() {
        return "M3CompositeMapView|COMPOSITE_MAP|OBJECT|SELECTED|Object[] backingLane";
    }

    /** Null backing is ignored. Reject key overlap before changing registry. */
    @SuppressWarnings("unchecked")
    public synchronized void addComposited(Map<K, V> incoming) {
        if (incoming == null) return;
        for (int i = size - 1; i >= 0; i--) {
            Map<K, V> existing = (Map<K, V>) backingLane[i];
            for (K key : incoming.keySet()) {
                if (existing.containsKey(key)) {
                    throw new IllegalArgumentException("Key collision adding Map to CompositeMap");
                }
            }
        }
        if (size == backingLane.length) {
            long grown = size == 0 ? 4L : (long) size + (size >>> 1) + 1;
            backingLane = Arrays.copyOf(backingLane, (int) Math.min(grown, Integer.MAX_VALUE));
        }
        backingLane[size++] = incoming;
    }

    /** Remove the first map comparing equal, returning the caller's argument. */
    public synchronized Map<K, V> removeComposited(Map<K, V> backing) {
        for (int i = 0; i < size; i++) {
            if (Objects.equals(backingLane[i], backing)) {
                System.arraycopy(backingLane, i + 1, backingLane, i, size - i - 1);
                backingLane[--size] = null;
                return backing;
            }
        }
        return null;
    }

    /** Last registered backing has precedence, including explicit null mappings. */
    @SuppressWarnings("unchecked")
    public V get(Object key) {
        for (int i = size - 1; i >= 0; i--) {
            Map<K, V> map = (Map<K, V>) backingLane[i];
            if (map.containsKey(key)) return map.get(key);
        }
        return null;
    }

    /** Commons CompositeMap defaults to refusing insertion without a mutator. */
    public V put(K key, V value) {
        throw new UnsupportedOperationException("No mutator specified");
    }

    /** The highest-precedence backing is the owner of an existing mapping. */
    @SuppressWarnings("unchecked")
    public V remove(Object key) {
        for (int i = size - 1; i >= 0; i--) {
            Map<K, V> map = (Map<K, V>) backingLane[i];
            if (map.containsKey(key)) return map.remove(key);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public boolean containsKey(Object key) {
        for (int i = size - 1; i >= 0; i--) {
            if (((Map<K, V>) backingLane[i]).containsKey(key)) return true;
        }
        return false;
    }

    /** Provider contract counts all registered backings, saturating at int maximum. */
    @SuppressWarnings("unchecked")
    public int size() {
        long count = 0L;
        for (int i = size - 1; i >= 0; i--) {
            count += ((Map<K, V>) backingLane[i]).size();
            if (count >= Integer.MAX_VALUE) return Integer.MAX_VALUE;
        }
        return (int) count;
    }

    @SuppressWarnings("unchecked")
    public boolean isEmpty() {
        for (int i = size - 1; i >= 0; i--) {
            if (!((Map<K, V>) backingLane[i]).isEmpty()) return false;
        }
        return true;
    }

    /** Additive operation present on the Commons donor, mutating each backing. */
    @SuppressWarnings("unchecked")
    public void clear() {
        for (int i = size - 1; i >= 0; i--) {
            ((Map<K, V>) backingLane[i]).clear();
        }
    }
}
