/*
 * Copyright (c) 2026, Hitesh Soliwal.
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.util;

import java.util.Arrays;

/**
 * Primitive-indexed, insertion-ordered storage for M3 collection experiments.
 * Physical slots remain stable across sorting. A sorted permutation is rebuilt
 * only after mutation; it never changes the underlying insertion order.
 *
 * This is an internal opt-in building block, not a replacement for JDK List.
 * Mutations are single-thread confined; comparator consistency is the caller's
 * responsibility. No arrays are exposed or returned to a pool while live.
 */
public final class M3IndexedStorage<E> {
    private Object[] values;
    private int size;
    private long version;
    private int[] sorted;
    private long sortedVersion = -1;
    private java.util.Comparator<? super E> sortedComparator;

    public M3IndexedStorage() {
        this(16);
    }

    public M3IndexedStorage(int initialCapacity) {
        if (initialCapacity < 0) throw new IllegalArgumentException("capacity");
        values = new Object[initialCapacity];
    }

    public int size() { return size; }
    public long version() { return version; }

    public void add(E value) {
        ensureCapacity(size + 1);
        values[size++] = value;
        version++;
    }

    @SuppressWarnings("unchecked")
    public E get(int insertionIndex) {
        checkIndex(insertionIndex);
        return (E) values[insertionIndex];
    }

    @SuppressWarnings("unchecked")
    public E set(int insertionIndex, E value) {
        checkIndex(insertionIndex);
        E previous = (E) values[insertionIndex];
        values[insertionIndex] = value;
        version++;
        return previous;
    }

    @SuppressWarnings("unchecked")
    public E removeAt(int insertionIndex) {
        checkIndex(insertionIndex);
        E previous = (E) values[insertionIndex];
        int moved = size - insertionIndex - 1;
        if (moved > 0) System.arraycopy(values, insertionIndex + 1,
                values, insertionIndex, moved);
        values[--size] = null;
        version++;
        return previous;
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= size) throw new IndexOutOfBoundsException(index);
    }

    private void ensureCapacity(int needed) {
        if (needed <= values.length) return;
        int old = values.length;
        int grown = old + (old >> 1) + 1;
        values = Arrays.copyOf(values, Math.max(needed, grown));
    }

    /**
     * Return a defensive snapshot of insertion positions in sorted order.
     * Equal values retain insertion order through the secondary index key.
     */
    @SuppressWarnings("unchecked")
    public int[] sortedPositions(java.util.Comparator<? super E> comparator) {
        java.util.Objects.requireNonNull(comparator);
        if (sortedVersion != version || sortedComparator != comparator) {
            // Primitive permutation with an in-place heapsort; no Integer boxing.
            int[] permutation = new int[size];
            for (int i = 0; i < size; i++) permutation[i] = i;
            for (int i = size / 2 - 1; i >= 0; i--) sift(permutation, i, size, comparator);
            for (int end = size - 1; end > 0; end--) {
                int t = permutation[0];
                permutation[0] = permutation[end];
                permutation[end] = t;
                sift(permutation, 0, end, comparator);
            }
            sorted = permutation;
            sortedVersion = version;
            sortedComparator = comparator;
        }
        return sorted.clone();
    }

    @SuppressWarnings("unchecked")
    private int comparePositions(int left, int right,
            java.util.Comparator<? super E> comparator) {
        int comparison = comparator.compare((E) values[left], (E) values[right]);
        return comparison != 0 ? comparison : Integer.compare(left, right);
    }

    private void sift(int[] positions, int root, int count,
            java.util.Comparator<? super E> comparator) {
        while (root < count / 2) {
            int child = root * 2 + 1;
            if (child + 1 < count &&
                    comparePositions(positions[child], positions[child + 1], comparator) < 0) {
                child++;
            }
            if (comparePositions(positions[root], positions[child], comparator) >= 0) break;
            int tmp = positions[root];
            positions[root] = positions[child];
            positions[child] = tmp;
            root = child;
        }
    }

    /** Release backing and derived metadata together; no retained references. */
    public void clear() {
        Arrays.fill(values, 0, size, null);
        size = 0;
        sorted = null;
        sortedComparator = null;
        sortedVersion = -1;
        version++;
    }

    /** Discard optional sorted metadata when the VM is under memory pressure. */
    public void discardDerivedIndexes() {
        sorted = null;
        sortedComparator = null;
        sortedVersion = -1;
    }
}
