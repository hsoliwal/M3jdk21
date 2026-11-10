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
package jdk.internal.m3.collections.packed;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Double-ended priority queue using one short array and integer heap coordinates.
 * Duplicates remain separate values. Order is {@link Short#compare(short, short)}.
 * Minimum/maximum access is O(1); scalar insertion/removal is O(log n), excluding growth.
 * No per-element object, second heap or position index is retained. Not thread-safe.
 *
 * <p>Generated from the primitive-minmax task template; change the recipe template first.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedShortMinMaxHeap.java}
 * blob {@code 7753c08880f735d3d9a4ea8756188ab61f39529e} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections batch 2: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PackedShortMinMaxHeap {
    private short[] values;
    private int size;

    /** Starts with eight available primitive slots. */
    public PackedShortMinMaxHeap() { this(8); }

    /** Allocates exactly the requested initial capacity; zero is permitted. */
    public PackedShortMinMaxHeap(int expectedSize) {
        checkCapacity(expectedSize);
        values = new short[expectedSize];
    }

    /** Copies caller values once, then constructs the heap bottom-up in O(n). */
    public PackedShortMinMaxHeap(short[] initial) {
        Objects.requireNonNull(initial, "initial");
        checkCapacity(initial.length);
        values = initial.clone();
        size = initial.length;
        heapify();
    }

    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }
    public int capacity() { return values.length; }

    /** Logical reset; retains the allocated primitive lane. */
    public void clear() { size = 0; }

    /** Reserves at least the requested capacity without changing logical contents. */
    public void ensureCapacity(int minimum) {
        checkCapacity(minimum);
        if (minimum > values.length) {
            values = Arrays.copyOf(values, PackedSupport.growArrayCapacity(values.length, minimum));
        }
    }

    /** Explicit copy that releases spare primitive capacity. */
    public void trimToSize() {
        if (size != values.length) values = Arrays.copyOf(values, size);
    }

    /** Allocates only when the retained lane must grow. */
    public void add(short value) {
        ensureCapacity(PackedArrayCapacity.required(size, 1));
        int index = size++;
        values[index] = value;
        if (index == 0) return;
        int parent = PackedHeapAtoms.parent(index);
        boolean minimum = PackedMinMaxHeapAtoms.minLevel(index);
        if (better(values[parent], values[index], minimum)) {
            swap(index, parent);
            bubble(parent, !minimum);
        } else {
            bubble(index, minimum);
        }
    }

    /** Copies the batch and rebuilds bottom-up in O(size + batch length). */
    public void addAll(short[] source) {
        Objects.requireNonNull(source, "source");
        addAll(source, 0, source.length);
    }

    /** Validates the complete slice and size before mutating any element. */
    public void addAll(short[] source, int offset, int length) {
        Objects.requireNonNull(source, "source");
        Objects.checkFromIndexSize(offset, length, source.length);
        int required = PackedArrayCapacity.required(size, length);
        if (length == 0) return;
        ensureCapacity(required);
        System.arraycopy(source, offset, values, size, length);
        size = required;
        heapify();
    }

    /** Least retained value; throws when empty. */
    public short min() { requireElement(); return values[0]; }

    /** Greatest retained value; throws when empty. */
    public short max() { requireElement(); return values[maxIndex()]; }

    public short minOr(short fallback) { return size == 0 ? fallback : values[0]; }
    public short maxOr(short fallback) { return size == 0 ? fallback : values[maxIndex()]; }

    /** Removes one minimum occurrence; throws when empty. */
    public short removeMin() {
        requireElement();
        short result = values[0];
        if (--size > 0) {
            values[0] = values[size];
            descend(0, true);
        }
        return result;
    }

    /** Removes one maximum occurrence; throws when empty. */
    public short removeMax() {
        requireElement();
        int index = maxIndex();
        short result = values[index];
        if (index < --size) {
            values[index] = values[size];
            descend(index, false);
        }
        return result;
    }

    /** Independent snapshot in unspecified heap order. */
    public short[] toArray() { return Arrays.copyOf(values, size); }

    /** Independent ascending snapshot; the queue remains unchanged. */
    public short[] toSortedArray() {
        short[] copy = toArray();
        Arrays.sort(copy);
        return copy;
    }

    /** Copies one primitive lane, then streams that independent snapshot without boxing. */
    public java.util.stream.IntStream snapshotStream() {
        short[] copy = toArray();
        return java.util.stream.IntStream.range(0, copy.length).map(i -> copy[i]);
    }

    private int maxIndex() {
        if (size == 1) return 0;
        return size == 2 || Short.compare(values[1], values[2]) >= 0 ? 1 : 2;
    }

    private void heapify() {
        for (int i = PackedHeapAtoms.lastParent(size); i >= 0; i--) {
            descend(i, PackedMinMaxHeapAtoms.minLevel(i));
        }
    }

    private void bubble(int index, boolean minimum) {
        while (index >= 3) {
            int grandparent = PackedMinMaxHeapAtoms.grandparent(index);
            if (!better(values[index], values[grandparent], minimum)) return;
            swap(index, grandparent);
            index = grandparent;
        }
    }

    /** Restore one alternating level; the crossed parent belongs to the opposite order. */
    private void descend(int index, boolean minimum) {
        while (PackedHeapAtoms.hasChildren(index, size)) {
            int best = bestDescendant(index, minimum);
            if (!better(values[best], values[index], minimum)) return;
            swap(index, best);
            if (best < 4L * index + 3) return;
            int parent = PackedHeapAtoms.parent(best);
            if (better(values[parent], values[best], minimum)) swap(parent, best);
            index = best;
        }
    }

    /** At most two children and four grandchildren; long arithmetic avoids index overflow. */
    private int bestDescendant(int index, boolean minimum) {
        int best = PackedHeapAtoms.left(index);
        int right = best + 1;
        if (right < size && better(values[right], values[best], minimum)) best = right;
        long first = 4L * index + 3;
        long end = Math.min((long) size, first + 4);
        for (long candidate = first; candidate < end; candidate++) {
            int next = (int) candidate;
            if (better(values[next], values[best], minimum)) best = next;
        }
        return best;
    }

    private static boolean better(short left, short right, boolean minimum) {
        int comparison = Short.compare(left, right);
        return minimum ? comparison < 0 : comparison > 0;
    }

    private void swap(int first, int second) {
        short value = values[first];
        values[first] = values[second];
        values[second] = value;
    }

    private void requireElement() {
        if (size == 0) throw new NoSuchElementException("empty min-max heap");
    }

    private static void checkCapacity(int capacity) {
        if (capacity < 0 || capacity > PackedSupport.MAX_ARRAY_SIZE) {
            throw new IllegalArgumentException("capacity");
        }
    }
}
