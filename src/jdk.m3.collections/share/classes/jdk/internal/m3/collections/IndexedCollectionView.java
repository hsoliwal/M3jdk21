/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.m3.collections;

import java.util.AbstractCollection;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Function;

/**
 * Indexed collection with array-owned element, key and primitive offset lanes.
 * It does not retain a Map<K, Collection<E>> or per-element entry/node graph.
 * Lookup uses a compact open-addressed int[] of the first ordinal per key.
 * Keys are cached until explicitly reindexed after external key mutation.
 */
public final class IndexedCollectionView<K, E> {
    static final long STATE_OCCUPIED = 1L << 0;
    static final long STATE_INDEX_DIRTY = 1L << 1;
    private Object[] elementLane = new Object[0];
    private Object[] indexKeyLane = new Object[0];
    private int[] indexOffsetLane = new int[0];
    private long[] stateMaskWords = {STATE_OCCUPIED};
    private int size;
    private final Function<? super E, ? extends K> keyTransformer;
    private final boolean uniqueKeys;

    /** Backward-compatible no-arg constructor uses the element as its key. */
    @SuppressWarnings("unchecked")
    public IndexedCollectionView() { this(value -> (K) value, false); }

    public IndexedCollectionView(Function<? super E, ? extends K> keyTransformer) {
        this(keyTransformer, false);
    }

    public IndexedCollectionView(Function<? super E, ? extends K> keyTransformer,
                                 boolean uniqueKeys) {
        this.keyTransformer = Objects.requireNonNull(keyTransformer, "keyTransformer");
        this.uniqueKeys = uniqueKeys;
    }

    public static String scaffoldDescriptor() {
        return "M3IndexedCollectionView|INDEXED_COLLECTION|OBJECT|OCCUPIED,INDEX_DIRTY|Object[] elementLane;Object[] indexKeyLane;int[] indexOffsetLane";
    }

    private static int hash(Object value) {
        int h = value == null ? 0 : value.hashCode();
        return h ^ (h >>> 16);
    }

    private static int find(Object key, Object[] keys, int[] offsets) {
        if (offsets.length == 0) return -1;
        int mask = offsets.length - 1;
        int bucket = hash(key) & mask;
        for (int slot; (slot = offsets[bucket]) != 0; bucket = (bucket + 1) & mask) {
            if (Objects.equals(key, keys[slot - 1])) return slot - 1;
        }
        return -1;
    }

    private static void putIndex(Object[] keys, int[] offsets, int ordinal) {
        int mask = offsets.length - 1;
        int bucket = hash(keys[ordinal]) & mask;
        while (offsets[bucket] != 0) {
            if (Objects.equals(keys[ordinal], keys[offsets[bucket] - 1])) return;
            bucket = (bucket + 1) & mask;
        }
        offsets[bucket] = ordinal + 1;
    }

    private void reserve(int required) {
        if (required > elementLane.length) {
            long expanded = elementLane.length == 0 ? 8L
                    : (long) elementLane.length + (elementLane.length >>> 1) + 1;
            int capacity = (int) Math.min(Integer.MAX_VALUE, Math.max(expanded, required));
            elementLane = Arrays.copyOf(elementLane, capacity);
            indexKeyLane = Arrays.copyOf(indexKeyLane, capacity);
        }
        if ((long) required * 2 > indexOffsetLane.length) {
            int next = Math.max(16, indexOffsetLane.length);
            while ((long) required * 2 > next) next <<= 1;
            int[] replacement = new int[next];
            for (int i = 0; i < size; i++) putIndex(indexKeyLane, replacement, i);
            indexOffsetLane = replacement;
        }
    }

    public boolean add(E element) {
        K key = keyTransformer.apply(element);
        if (uniqueKeys && find(key, indexKeyLane, indexOffsetLane) >= 0) {
            throw new IllegalArgumentException("Duplicate key in uniquely indexed collection.");
        }
        reserve(Math.addExact(size, 1));
        elementLane[size] = element;
        indexKeyLane[size] = key;
        putIndex(indexKeyLane, indexOffsetLane, size);
        size++;
        return true;
    }

    public boolean remove(Object element) {
        for (int i = 0; i < size; i++) {
            if (Objects.equals(element, elementLane[i])) {
                System.arraycopy(elementLane, i + 1, elementLane, i, size - i - 1);
                System.arraycopy(indexKeyLane, i + 1, indexKeyLane, i, size - i - 1);
                elementLane[--size] = null;
                indexKeyLane[size] = null;
                Arrays.fill(indexOffsetLane, 0);
                for (int j = 0; j < size; j++) putIndex(indexKeyLane, indexOffsetLane, j);
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public E get(K key) {
        int index = find(key, indexKeyLane, indexOffsetLane);
        return index < 0 ? null : (E) elementLane[index];
    }

    /** Live, read-only view; absent keys return null as in Commons IndexedCollection. */
    public Collection<E> values(K key) {
        if (find(key, indexKeyLane, indexOffsetLane) < 0) return null;
        return new AbstractCollection<>() {
            @Override public int size() {
                int count = 0;
                for (int i = 0; i < IndexedCollectionView.this.size; i++) {
                    if (Objects.equals(key, indexKeyLane[i])) count++;
                }
                return count;
            }
            @Override public Iterator<E> iterator() {
                return new Iterator<>() {
                    int cursor;
                    private int advance(int from) {
                        while (from < IndexedCollectionView.this.size
                                && !Objects.equals(key, indexKeyLane[from])) from++;
                        return from;
                    }
                    @Override public boolean hasNext() {
                        return advance(cursor) < IndexedCollectionView.this.size;
                    }
                    @SuppressWarnings("unchecked")
                    @Override public E next() {
                        int next = advance(cursor);
                        if (next >= IndexedCollectionView.this.size) throw new NoSuchElementException();
                        cursor = next + 1;
                        return (E) elementLane[next];
                    }
                };
            }
        };
    }

    /** Atomic refresh of all extractor-produced keys and their indexed positions. */
    @SuppressWarnings("unchecked")
    public void reindex() {
        Object[] newKeys = Arrays.copyOf(indexKeyLane, indexKeyLane.length);
        int[] replacement = new int[indexOffsetLane.length];
        for (int i = 0; i < size; i++) {
            Object key = keyTransformer.apply((E) elementLane[i]);
            if (uniqueKeys && find(key, newKeys, replacement) >= 0) {
                throw new IllegalArgumentException("Duplicate key in uniquely indexed collection.");
            }
            newKeys[i] = key;
            putIndex(newKeys, replacement, i);
        }
        indexKeyLane = newKeys;
        indexOffsetLane = replacement;
    }

    /** Commons semantics: membership is evaluated using the derived key. */
    @SuppressWarnings("unchecked")
    public boolean contains(Object element) {
        K key = keyTransformer.apply((E) element);
        return find(key, indexKeyLane, indexOffsetLane) >= 0;
    }

    public int size() { return size; }

    public void clear() {
        Arrays.fill(elementLane, 0, size, null);
        Arrays.fill(indexKeyLane, 0, size, null);
        Arrays.fill(indexOffsetLane, 0);
        size = 0;
    }
}
