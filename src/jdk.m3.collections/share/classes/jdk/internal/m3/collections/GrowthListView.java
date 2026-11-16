/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.m3.collections;

import java.util.Arrays;

/**
 * Dynamic indexed list over one parent-owned {@code Object[]} lane. Indexed
 * writes and inserts beyond the logical end extend the list with null slots,
 * matching Commons Collections GrowthList without its list decorator graph.
 * Stored elements have no Node, Cell or Entry objects.
 *
 * <p>Not thread-safe. Element references and their encounter order are retained.
 */
public final class GrowthListView<E> {
    static final long STATE_GROWTH = 1L << 0;
    private Object[] valueLane = new Object[0];
    private final long[] stateMaskWords = {STATE_GROWTH};
    private int size;

    public GrowthListView() { }

    /** Keep the existing public catalogue descriptor and compatibility identity. */
    public static String scaffoldDescriptor() {
        return "M3GrowthListView|GROWTH_LIST|OBJECT|GROWTH|Object[] valueLane";
    }

    @SuppressWarnings("unchecked")
    public E get(int index) {
        checkExisting(index);
        return (E) valueLane[index];
    }

    /** Replace one value, or grow and null-pad up to the requested index. */
    @SuppressWarnings("unchecked")
    public E set(int index, E element) {
        int required = requiredLength(index);
        if (index < size) {
            E previous = (E) valueLane[index];
            valueLane[index] = element;
            return previous;
        }
        reserve(required);
        Arrays.fill(valueLane, size, index, null);
        valueLane[index] = element;
        size = required;
        return null;
    }

    /** Insert in encounter order, or append after null-padding a gap. */
    public void add(int index, E element) {
        int required = requiredLength(index);
        if (index < size) {
            reserve(increment(size));
            System.arraycopy(valueLane, index, valueLane, index + 1, size - index);
            valueLane[index] = element;
            size++;
        } else if (index == size) {
            reserve(increment(size));
            valueLane[size++] = element;
        } else {
            reserve(required);
            Arrays.fill(valueLane, size, index, null);
            valueLane[index] = element;
            size = required;
        }
    }

    public boolean add(E element) {
        reserve(increment(size));
        valueLane[size++] = element;
        return true;
    }

    public int size() { return size; }

    /** Explicit snapshot boundary; not the retained backing array. */
    public Object[] toArray() { return Arrays.copyOf(valueLane, size); }

    /** Release owned references while retaining the reusable capacity. */
    public void clear() {
        Arrays.fill(valueLane, 0, size, null);
        size = 0;
    }

    private void checkExisting(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("index=" + index + " size=" + size);
        }
    }

    private static int requiredLength(int index) {
        if (index < 0) throw new IndexOutOfBoundsException("index=" + index);
        return increment(index);
    }

    private static int increment(int index) {
        if (index == Integer.MAX_VALUE) throw new OutOfMemoryError("list too large");
        return index + 1;
    }

    private void reserve(int minimum) {
        if (minimum <= valueLane.length) return;
        long grown = valueLane.length == 0 ? 8L
                : (long) valueLane.length + (valueLane.length >>> 1) + 1;
        int capacity = (int) Math.min(Integer.MAX_VALUE, Math.max(grown, minimum));
        valueLane = Arrays.copyOf(valueLane, capacity);
    }
}
