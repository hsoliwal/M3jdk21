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

import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Insertion-ordered set with stable payload slots and primitive index/link lanes.
 *
 * <p>Lookup/removal is expected O(1), with O(n) collision worst case. Unlinking a
 * known slot changes only its neighbors and bucket; surviving payloads never shift.
 * Growth/rehash is occasional O(n) expected work. No per-element entry or node is
 * retained. Keys must keep stable hash/equality and must not mutate this set from
 * those callbacks. Null adds are rejected. External iterator mutation is unspecified.
 *
 * @param <E> element type
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/DenseOrderedSet.java}
 * blob {@code 20e9aec10920fc4d4b3abd3d3132a03ff82c302a} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections batch 2: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3DenseOrderedSet<E> extends AbstractSet<E> {
    private Object[] order;
    // Zero is empty, -1 is deleted, positive values encode payload slots plus one.
    private int[] slots;
    private int[] previous;
    private int[] next;
    private int[] buckets;
    private int size;
    private int used;
    private int highWater;
    private int free = -1;
    private int first = -1;
    private int last = -1;

    public M3DenseOrderedSet() { this(4); }

    public M3DenseOrderedSet(int expectedSize) {
        if (expectedSize < 0) throw new IllegalArgumentException("expectedSize");
        int capacity = indexCapacity(expectedSize);
        int payloadCapacity = Math.max(1, expectedSize);
        order = new Object[payloadCapacity];
        slots = new int[capacity];
        previous = new int[payloadCapacity];
        next = new int[payloadCapacity];
        buckets = new int[payloadCapacity];
    }

    public M3DenseOrderedSet(Collection<? extends E> source) {
        this(source == null ? 0 : source.size());
        if (source != null) addAll(source);
    }

    private static int indexCapacity(int size) {
        int capacity = 8;
        while (size > capacity - (capacity >>> 2)) {
            if (capacity == 1 << 30) throw new IllegalArgumentException("index capacity overflow");
            capacity <<= 1;
        }
        return capacity;
    }

    private static int hash(Object value) {
        return M3CollectionHashes.mix(value.hashCode());
    }

    private int slot(Object value, int hash) {
        int at = hash & (slots.length - 1);
        int deleted = -1;
        while (slots[at] != 0) {
            int position = slots[at];
            if (position < 0) {
                if (deleted < 0) deleted = at;
            } else if (value.equals(order[position - 1])) {
                return at;
            }
            at = (at + 1) & (slots.length - 1);
        }
        return deleted < 0 ? at : deleted;
    }

    private static int emptySlot(int[] index, int hash) {
        int at = hash & (index.length - 1);
        while (index[at] != 0) at = (at + 1) & (index.length - 1);
        return at;
    }

    @Override public boolean add(E element) {
        Objects.requireNonNull(element, "element");
        int hash = hash(element);
        int at = slot(element, hash);
        if (slots[at] > 0) return false;
        int required = M3DenseArrayOrder.sum(size, 1);
        int row = free < 0 ? highWater : free;
        Object[] newOrder = order;
        int[] newPrevious = previous, newNext = next, newBuckets = buckets;
        if (row == order.length) {
            int capacity = M3DenseArrayOrder.capacity(order.length, M3DenseArrayOrder.sum(row, 1));
            newOrder = Arrays.copyOf(order, capacity);
            newPrevious = Arrays.copyOf(previous, capacity);
            newNext = Arrays.copyOf(next, capacity);
            newBuckets = Arrays.copyOf(buckets, capacity);
        }
        int[] newSlots = slots;
        int newUsed = used;
        if (slots[at] == 0 && used >= slots.length - (slots.length >>> 2)) {
            newSlots = new int[indexCapacity(required)];
            // Reverse positions must also remain unpublished until every hash succeeds.
            if (newBuckets == buckets) newBuckets = Arrays.copyOf(buckets, buckets.length);
            for (int live = first; live >= 0; live = next[live]) {
                int bucket = emptySlot(newSlots, hash(order[live]));
                newSlots[bucket] = live + 1;
                newBuckets[live] = bucket;
            }
            at = emptySlot(newSlots, hash);
            newUsed = size;
        }
        order = newOrder; previous = newPrevious; next = newNext; buckets = newBuckets;
        slots = newSlots;
        used = newUsed + (slots[at] == 0 ? 1 : 0);
        if (free < 0) highWater++;
        else free = next[row];
        order[row] = element;
        long endpoints = M3PackedSlotLinkAtoms.linkLast(previous, next, first, last, row);
        first = M3PackedSlotLinkAtoms.first(endpoints);
        last = M3PackedSlotLinkAtoms.last(endpoints);
        buckets[row] = at; slots[at] = row + 1;
        size++;
        return true;
    }

    private void removeAt(int row) {
        long endpoints = M3PackedSlotLinkAtoms.unlink(previous, next, first, last, row);
        first = M3PackedSlotLinkAtoms.first(endpoints);
        last = M3PackedSlotLinkAtoms.last(endpoints);
        slots[buckets[row]] = -1;
        order[row] = null;
        previous[row] = -1; buckets[row] = -1;
        next[row] = free; free = row;
        size--;
    }

    @Override public boolean remove(Object element) {
        if (element == null) return false;
        int row = slots[slot(element, hash(element))] - 1;
        if (row < 0) return false;
        removeAt(row);
        return true;
    }

    @Override public boolean contains(Object element) {
        return element != null && slots[slot(element, hash(element))] > 0;
    }

    @Override public int size() { return size; }
    @Override public boolean isEmpty() { return size == 0; }

    @Override public void clear() {
        Arrays.fill(order, 0, highWater, null);
        Arrays.fill(slots, 0);
        size = 0; used = 0; highWater = 0; free = -1; first = -1; last = -1;
    }

    @Override public Iterator<E> iterator() {
        return new Iterator<>() {
            private int following = first;
            private int returned = -1;
            @Override public boolean hasNext() { return following >= 0; }
            @Override public E next() {
                if (!hasNext()) throw new NoSuchElementException();
                returned = following;
                following = next[returned];
                @SuppressWarnings("unchecked") E value = (E) order[returned];
                return value;
            }
            @Override public void remove() {
                if (returned < 0) throw new IllegalStateException("next() required");
                removeAt(returned);
                returned = -1;
            }
        };
    }
}
