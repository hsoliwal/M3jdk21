/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.m3.collections;

import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

/**
 * Distinct, insertion-ordered values stored in one Object[] lane.
 * Projections are constructed only at API boundaries; no persistent hash
 * nodes, entries or wrappers are stored. Custom equality is evaluated in
 * candidate-to-stored order and never reordered by an internal hash table.
 */
public final class SetUniqueListView<E> {
    static final long STATE_UNIQUE = 1L << 0;
    private Object[] valueLane = new Object[0];
    private static final int[] EMPTY_INDEX = new int[0];
    private int[] hashIndexLane = EMPTY_INDEX;
    private long[] stateMaskWords = {STATE_UNIQUE};
    private int size;
    private int epoch;

    public SetUniqueListView() { }

    public static String scaffoldDescriptor() {
        return "M3SetUniqueListView|SET_UNIQUE_LIST|OBJECT|UNIQUE|Object[] valueLane;int[] hashIndexLane";
    }

    /** Hash acceleration only for exact immutable JDK scalars. */
    private static boolean stable(Object value) {
        return value == null || value.getClass() == String.class
                || value.getClass() == Integer.class || value.getClass() == Long.class;
    }

    private static int hash(Object key) {
        int code = key == null ? 0 : key.hashCode();
        return code ^ (code >>> 16);
    }

    private void insertIndex(int slot) {
        int mask = hashIndexLane.length - 1;
        int bucket = hash(valueLane[slot]) & mask;
        while (hashIndexLane[bucket] != 0) bucket = (bucket + 1) & mask;
        hashIndexLane[bucket] = slot + 1;
    }

    private void indexIfEligible() {
        if (hashIndexLane.length != 0 || size < 64 || size >= (1 << 28)) return;
        for (int i = 0; i < size; i++) if (!stable(valueLane[i])) return;
        int capacity = 128;
        while (capacity < size * 2) capacity <<= 1;
        hashIndexLane = new int[capacity];
        for (int i = 0; i < size; i++) insertIndex(i);
    }

    private int indexOf(Object candidate) {
        if (stable(candidate)) {
            indexIfEligible();
            if (hashIndexLane.length != 0) {
                int mask = hashIndexLane.length - 1;
                int bucket = hash(candidate) & mask;
                for (int slot; (slot = hashIndexLane[bucket]) != 0;
                        bucket = (bucket + 1) & mask) {
                    if (Objects.equals(candidate, valueLane[slot - 1])) return slot - 1;
                }
                return -1;
            }
        }
        for (int i = 0; i < size; i++) {
            if (Objects.equals(candidate, valueLane[i])) return i;
        }
        return -1;
    }

    private void reserve(int count) {
        if (count <= valueLane.length) return;
        long proposed = valueLane.length == 0 ? 8L
                : (long) valueLane.length + (valueLane.length >>> 1) + 1;
        valueLane = Arrays.copyOf(valueLane, (int) Math.min(Integer.MAX_VALUE, Math.max(count, proposed)));
    }

    private void changed() {
        epoch++;
        hashIndexLane = EMPTY_INDEX;
    }

    public boolean add(E element) {
        if (indexOf(element) >= 0) return false;
        reserve(Math.addExact(size, 1));
        // Keep the already-materialized index hot for simple append-only use.
        boolean indexed = hashIndexLane.length != 0 && stable(element)
                && (long) (size + 1) * 2 <= hashIndexLane.length;
        valueLane[size++] = element;
        if (indexed) {
            insertIndex(size - 1);
            epoch++;
        } else {
            changed();
        }
        return true;
    }

    /** Reject invalid indices even if a duplicate makes insertion a no-op. */
    public void add(int index, E element) {
        Objects.checkIndex(index, Math.addExact(size, 1));
        if (indexOf(element) >= 0) return;
        reserve(Math.addExact(size, 1));
        System.arraycopy(valueLane, index, valueLane, index + 1, size - index);
        valueLane[index] = element;
        size++;
        changed();
    }

    /** Commons SetUniqueList semantics: set and remove any earlier duplicate. */
    @SuppressWarnings("unchecked")
    public E set(int index, E element) {
        Objects.checkIndex(index, size);
        int duplicate = indexOf(element);
        E previous = (E) valueLane[index];
        valueLane[index] = element;
        if (duplicate != -1 && duplicate != index) {
            System.arraycopy(valueLane, duplicate + 1, valueLane, duplicate, size - duplicate - 1);
            valueLane[--size] = null;
        }
        changed();
        return previous;
    }

    @SuppressWarnings("unchecked")
    public E get(int index) {
        return (E) valueLane[Objects.checkIndex(index, size)];
    }

    public boolean remove(Object element) {
        int index = indexOf(element);
        if (index < 0) return false;
        System.arraycopy(valueLane, index + 1, valueLane, index, size - index - 1);
        valueLane[--size] = null;
        changed();
        return true;
    }

    /** Live, read-only set facade; changes still live in the owning Object[]. */
    public Set<E> asSet() {
        return Collections.unmodifiableSet(new AbstractSet<>() {
            @Override public int size() { return SetUniqueListView.this.size; }
            @Override public boolean contains(Object value) { return indexOf(value) >= 0; }
            @Override public Iterator<E> iterator() {
                return new Iterator<>() {
                    private final int expected = epoch;
                    private int next;
                    private void check() {
                        if (expected != epoch) throw new ConcurrentModificationException();
                    }
                    @Override public boolean hasNext() { check(); return next < size; }
                    @Override public E next() {
                        check();
                        if (next >= size) throw new NoSuchElementException();
                        return get(next++);
                    }
                };
            }
        });
    }

    public int size() { return size; }

    /** Additive detached snapshot; not a second persistent storage owner. */
    public Object[] toArray() { return Arrays.copyOf(valueLane, size); }
}
