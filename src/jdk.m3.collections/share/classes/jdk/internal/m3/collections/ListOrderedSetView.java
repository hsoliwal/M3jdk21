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

/** Ordered, unique elements in one Object[] lane, with optional primitive index. */
public final class ListOrderedSetView<E> {
    static final long STATE_OCCUPIED = 1L << 0;
    static final long STATE_DELETED = 1L << 1;
    private Object[] valueLane = new Object[0];
    private int[] orderLane = new int[0];
    private int[] hashIndexLane = new int[0];
    private long[] stateMaskWords = new long[0];
    private int size;
    private int epoch;

    public ListOrderedSetView() { }

    public static String scaffoldDescriptor() {
        return "M3ListOrderedSetView|LIST_ORDERED_SET|OBJECT|OCCUPIED,DELETED|Object[] valueLane;int[] orderLane;int[] hashIndexLane";
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
        if (hashIndexLane.length != 0 || size < 64 || size > (1 << 28)) return;
        for (int i = 0; i < size; i++) if (!stable(valueLane[i])) return;
        int capacity = 128;
        while (capacity < (long) size * 2) capacity <<= 1;
        int[] index = new int[capacity];
        for (int i = 0; i < size; i++) {
            int slot = hash(valueLane[i]) & (capacity - 1);
            while (index[slot] != 0) slot = (slot + 1) & (capacity - 1);
            index[slot] = i + 1;
        }
        hashIndexLane = index;
    }

    public int indexOf(Object element) {
        if (stable(element)) {
            indexIfEligible();
            if (hashIndexLane.length != 0) {
                int mask = hashIndexLane.length - 1;
                int slot = hash(element) & mask;
                for (int entry; (entry = hashIndexLane[slot]) != 0;
                        slot = (slot + 1) & mask) {
                    if (Objects.equals(element, valueLane[entry - 1])) return entry - 1;
                }
                return -1;
            }
        }
        for (int i = 0; i < size; i++) if (Objects.equals(element, valueLane[i])) return i;
        return -1;
    }

    private void grow(int required) {
        if (required <= valueLane.length) return;
        long next = valueLane.length == 0 ? 8L : (long) valueLane.length + (valueLane.length >>> 1) + 1;
        valueLane = Arrays.copyOf(valueLane, (int) Math.min(Integer.MAX_VALUE, Math.max(next, required)));
    }

    private void changed() { hashIndexLane = new int[0]; epoch++; }

    public boolean add(E element) {
        if (indexOf(element) >= 0) return false;
        grow(Math.addExact(size, 1));
        boolean keepIndex = hashIndexLane.length != 0 && stable(element)
                && (long) (size + 1) * 2 <= hashIndexLane.length;
        valueLane[size++] = element;
        if (keepIndex) {
            int mask = hashIndexLane.length - 1;
            int bucket = hash(element) & mask;
            while (hashIndexLane[bucket] != 0) bucket = (bucket + 1) & mask;
            hashIndexLane[bucket] = size;
            epoch++;
        } else {
            changed();
        }
        return true;
    }

    public void add(int index, E element) {
        if (index < 0 || index > size) throw new IndexOutOfBoundsException(index);
        if (indexOf(element) >= 0) return;
        grow(Math.addExact(size, 1));
        System.arraycopy(valueLane, index, valueLane, index + 1, size - index);
        valueLane[index] = element;
        size++;
        changed();
    }

    @SuppressWarnings("unchecked")
    public E get(int index) { return (E) valueLane[Objects.checkIndex(index, size)]; }

    @SuppressWarnings("unchecked")
    public E remove(int index) {
        Objects.checkIndex(index, size);
        E old = (E) valueLane[index];
        System.arraycopy(valueLane, index + 1, valueLane, index, size - index - 1);
        valueLane[--size] = null;
        changed();
        return old;
    }

    public boolean remove(Object element) {
        int index = indexOf(element);
        if (index < 0) return false;
        remove(index);
        return true;
    }

    /** Unmodifiable, live projection: neither order nor values are copied. */
    public List<E> asList() {
        return Collections.unmodifiableList(new AbstractList<>() {
            @Override public E get(int index) { return ListOrderedSetView.this.get(index); }
            @Override public int size() { return ListOrderedSetView.this.size; }
        });
    }

    public int size() { return size; }

    public void clear() {
        Arrays.fill(valueLane, 0, size, null);
        size = 0;
        changed();
    }

    public Object[] toArray() { return Arrays.copyOf(valueLane, size); }
}
