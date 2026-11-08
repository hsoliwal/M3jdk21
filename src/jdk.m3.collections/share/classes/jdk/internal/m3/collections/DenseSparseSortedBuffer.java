/*******************************************************************************
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

import java.util.AbstractCollection;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Sorted payload slots with holes and an int[] rank-to-slot index.
 *
 * <p>Binary search uses logical ranks, never a binary search over null holes.
 * Local shifts reduce moved payload references in some workloads; rank-index
 * movement still has an O(n) worst case. This is not a logarithmic-update tree.
 * Null is the vacant-slot marker. All equal-ranked items retain insertion order.
 *
 * @param <E> element type
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/DenseSparseSortedBuffer.java}
 * blob {@code b0d8996b0c0d2197d0e7922ede87370fe41341c8} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (common collections batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class DenseSparseSortedBuffer<E> extends AbstractCollection<E> {
    private Object[] slots;
    private int[] positions;
    private final double loadFactor;
    private final Comparator<? super E> comparator;
    private int size;
    private int modifications;
    private long movedReferences;

    public DenseSparseSortedBuffer(int initialCapacity, double loadFactor,
            Comparator<? super E> comparator) {
        if (initialCapacity < 2 || initialCapacity > Integer.MAX_VALUE - 8) {
            throw new IllegalArgumentException("initialCapacity must be at least two");
        }
        if (!(loadFactor > 0.0 && loadFactor < 1.0)) {
            throw new IllegalArgumentException("loadFactor must be strictly between zero and one");
        }
        this.slots = new Object[initialCapacity];
        this.positions = new int[initialCapacity];
        this.loadFactor = loadFactor;
        this.comparator = comparator;
    }

    @Override public int size() { return size; }
    public int capacity() { return slots.length; }
    public int vacantSlots() { return slots.length - size; }
    public long movedReferences() { return movedReferences; }

    @SuppressWarnings("unchecked")
    public E get(int rank) {
        Objects.checkIndex(rank, size);
        return (E) slots[positions[rank]];
    }

    @SuppressWarnings("unchecked")
    private int compare(Object left, Object right) {
        Objects.requireNonNull(left, "element");
        Objects.requireNonNull(right, "element");
        return comparator == null ? ((Comparable<? super E>) left).compareTo((E) right)
                : comparator.compare((E) left, (E) right);
    }

    public int lowerBound(E element) { return bound(element, false); }
    public int upperBound(E element) { return bound(element, true); }

    private int bound(E element, boolean upper) {
        compare(element, element);
        int left = 0;
        int right = size;
        while (left < right) {
            int middle = (left + right) >>> 1;
            int comparison = compare(slots[positions[middle]], element);
            if (comparison < 0 || upper && comparison == 0) { left = middle + 1; }
            else { right = middle; }
        }
        return left;
    }

    private int capacityFor(int required) {
        int capacity = slots.length;
        while (required > (int) Math.floor(capacity * loadFactor)) {
            int grown = DenseArrayOrder.capacity(capacity, DenseArrayOrder.sum(required, 1));
            if (grown <= capacity) { throw new IllegalArgumentException("density capacity overflow"); }
            capacity = grown;
        }
        return capacity;
    }

    private void redistribute(int capacity) {
        Object[] newSlots = new Object[capacity];
        int[] newPositions = new int[capacity];
        for (int i = 0; i < size; i++) {
            int position = (int) (((i + 1L) * capacity) / (size + 1L));
            newSlots[position] = slots[positions[i]];
            newPositions[i] = position;
        }
        movedReferences += size;
        slots = newSlots;
        positions = newPositions;
    }

    @Override public boolean add(E element) {
        int rank = upperBound(element);
        int required = DenseArrayOrder.sum(size, 1);
        int capacity = capacityFor(required);
        if (capacity != slots.length) { redistribute(capacity); }
        int before = rank == 0 ? -1 : positions[rank - 1];
        int after = rank == size ? slots.length : positions[rank];
        int inserted;
        if (after - before > 1) {
            inserted = before + ((after - before) >>> 1);
        } else {
            int holeLeft = before - 1;
            while (holeLeft >= 0 && slots[holeLeft] != null) { holeLeft--; }
            int holeRight = after + 1;
            while (holeRight < slots.length && slots[holeRight] != null) { holeRight++; }
            int leftDistance = holeLeft < 0 ? Integer.MAX_VALUE : before - holeLeft;
            int rightDistance = holeRight >= slots.length ? Integer.MAX_VALUE : holeRight - after;
            if (leftDistance == Integer.MAX_VALUE && rightDistance == Integer.MAX_VALUE) {
                throw new IllegalStateException("density invariant: no vacant slot");
            }
            if (leftDistance <= rightDistance) {
                System.arraycopy(slots, holeLeft + 1, slots, holeLeft, leftDistance);
                inserted = before;
                for (int i = 0; i < rank; i++) {
                    if (positions[i] > holeLeft) { positions[i]--; }
                }
                movedReferences += leftDistance;
            } else {
                System.arraycopy(slots, after, slots, after + 1, rightDistance);
                inserted = after;
                for (int i = rank; i < size && positions[i] < holeRight; i++) {
                    positions[i]++;
                }
                movedReferences += rightDistance;
            }
        }
        System.arraycopy(positions, rank, positions, rank + 1, size - rank);
        positions[rank] = inserted;
        slots[inserted] = element;
        size++;
        modifications++;
        return true;
    }

    /** Stable batch sort followed by deterministic spreading; no repeated insertion shifts. */
    @Override public boolean addAll(Collection<? extends E> source) {
        Objects.requireNonNull(source, "source");
        int expected = modifications;
        Object[] incoming = source.toArray();
        if (incoming.length == 0) { return false; }
        int total = DenseArrayOrder.sum(size, incoming.length);
        Object[] ordered = new Object[total];
        for (int i = 0; i < size; i++) { ordered[i] = slots[positions[i]]; }
        for (int i = 0; i < incoming.length; i++) {
            compare(incoming[i], incoming[i]);
            ordered[size + i] = incoming[i];
        }
        Arrays.sort(ordered, this::compare);
        int capacity = capacityFor(total);
        Object[] newSlots = new Object[capacity];
        int[] newPositions = new int[capacity];
        for (int i = 0; i < total; i++) {
            int position = (int) (((i + 1L) * capacity) / (total + 1L));
            newSlots[position] = ordered[i];
            newPositions[i] = position;
        }
        if (expected != modifications) { throw new ConcurrentModificationException(); }
        slots = newSlots;
        positions = newPositions;
        size = total;
        movedReferences += total;
        modifications++;
        return true;
    }

    public E removeAt(int rank) {
        E previous = get(rank);
        slots[positions[rank]] = null;
        System.arraycopy(positions, rank + 1, positions, rank, size - rank - 1);
        positions[--size] = 0;
        modifications++;
        return previous;
    }

    @Override public void clear() {
        if (size != 0) {
            Arrays.fill(slots, null);
            Arrays.fill(positions, 0);
            size = 0;
            modifications++;
        }
    }

    @Override public Iterator<E> iterator() {
        return new Iterator<>() {
            private int expected = modifications;
            private int next;
            private int last = -1;
            private void check() {
                if (expected != modifications) { throw new ConcurrentModificationException(); }
            }
            @Override public boolean hasNext() { check(); return next < size; }
            @Override public E next() {
                if (!hasNext()) { throw new NoSuchElementException(); }
                last = next;
                return get(next++);
            }
            @Override public void remove() {
                check();
                if (last < 0) { throw new IllegalStateException(); }
                removeAt(last);
                next = last;
                last = -1;
                expected = modifications;
            }
        };
    }
}
