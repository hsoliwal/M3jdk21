// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
// Modified 2026: DOOP offset-indexed binary priority heap; no entry or tree nodes.
package com.m3.collections;

import java.io.Serial;
import java.io.Serializable;
import java.util.AbstractQueue;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Generic min-priority queue in one dynamic Object[]; the binary tree is
 * implicit in parent=(offset-1)/2 and children=2*offset+1/2. No Entry/Node
 * objects are stored or manufactured during heap operations. Iterator
 * creation is an explicit API projection that copies references into one
 * flat Object[] and uses identity-based removal of the yielded occurrence.
 */
public final class M3PriorityQueue<E> extends AbstractQueue<E>
        implements Cloneable, Serializable {
    @Serial private static final long serialVersionUID = 1L;
    private Object[] heap = M3SlotArrays.EMPTY_OBJECTS;
    private final Comparator<? super E> comparator;
    private int size, revision;

    public M3PriorityQueue() { this((Comparator<? super E>) null); }
    public M3PriorityQueue(Comparator<? super E> comparator) {
        this.comparator = comparator;
    }
    public M3PriorityQueue(Collection<? extends E> source) {
        this(sourceComparator(Objects.requireNonNull(source, "source")));
        addAll(source);
    }
    @SuppressWarnings("unchecked")
    private static <E> Comparator<? super E> sourceComparator(Collection<? extends E> source) {
        if (source instanceof java.util.SortedSet<?> sorted)
            return (Comparator<? super E>) sorted.comparator();
        if (source instanceof java.util.PriorityQueue<?> sorted)
            return (Comparator<? super E>) sorted.comparator();
        if (source instanceof M3PriorityQueue<?> sorted)
            return (Comparator<? super E>) sorted.comparator();
        return null;
    }
    public Comparator<? super E> comparator() { return comparator; }
    public int capacity() { return heap.length; }
    @Override public int size() { return size; }

    @SuppressWarnings("unchecked")
    private E at(int offset) { return (E) heap[offset]; }

    @SuppressWarnings("unchecked")
    private int compare(E left, E right) {
        return comparator == null ? ((Comparable<? super E>) left).compareTo(right)
                : comparator.compare(left, right);
    }

    private void ensureCapacity(int required) {
        if (required <= heap.length) return;
        int next = M3SlotArrays.grow(heap.length);
        if (next < required) next = required;
        heap = Arrays.copyOf(heap, next);
    }

    @Override @SuppressWarnings("unchecked") public boolean offer(E value) {
        Objects.requireNonNull(value, "priority queue element");
        if (comparator == null) {
            // Match JDK natural-order admission, including the very first value.
            @SuppressWarnings("unused") Comparable<? super E> checked =
                    (Comparable<? super E>) value;
        }
        if (size == Integer.MAX_VALUE) throw new OutOfMemoryError("queue size exhausted");
        // Compare before mutating heap: a failing comparator does not corrupt
        // parents/children or insert a partially ordered element.
        int destination = size;
        while (destination > 0) {
            int parent = (destination - 1) >>> 1;
            if (compare(value, at(parent)) >= 0) break;
            destination = parent;
        }
        ensureCapacity(size + 1);
        for (int p = size; p != destination; p = (p - 1) >>> 1) {
            heap[p] = heap[(p - 1) >>> 1];
        }
        heap[destination] = value;
        size++;
        revision++;
        return true;
    }

    @Override public E peek() { return size == 0 ? null : at(0); }
    @Override public E poll() {
        if (size == 0) return null;
        E first = at(0);
        eraseAt(0);
        return first;
    }

    private void eraseAt(int position) {
        int lastIndex = size - 1;
        if (position == lastIndex) {
            heap[lastIndex] = null;
            size--;
            revision++;
            return;
        }

        E replacement = at(lastIndex);
        // Preflight sift-down without changing the heap, so exceptions from a
        // user comparator cannot drop the old root or strand its replacement.
        int target = position;
        int half = lastIndex >>> 1;
        while (target < half) {
            int child = (target << 1) + 1;
            int right = child + 1;
            if (right < lastIndex && compare(at(right), at(child)) < 0) child = right;
            if (compare(replacement, at(child)) <= 0) break;
            target = child;
        }

        if (target == position && position > 0) {
            // Replacement may move upward if its parent outranks it.
            while (target > 0) {
                int parent = (target - 1) >>> 1;
                if (compare(replacement, at(parent)) >= 0) break;
                target = parent;
            }
            for (int p = position; p != target; p = (p - 1) >>> 1)
                heap[p] = heap[(p - 1) >>> 1];
        } else {
            int p = position;
            while (p != target) {
                int child = (p << 1) + 1;
                int right = child + 1;
                if (right < lastIndex && compare(at(right), at(child)) < 0) child = right;
                heap[p] = heap[child];
                p = child;
            }
        }
        heap[target] = replacement;
        heap[lastIndex] = null;
        size--;
        revision++;
    }

    /** Remove the specific yielded object reference, as JDK PQ iterator.remove. */
    private boolean removeIdentity(Object value) {
        for (int s = 0; s < size; s++) {
            if (heap[s] == value) { eraseAt(s); return true; }
        }
        return false;
    }

    @Override public boolean remove(Object value) {
        if (value == null) return false;
        for (int s = 0; s < size; s++) {
            if (value.equals(heap[s])) { eraseAt(s); return true; }
        }
        return false;
    }
    @Override public boolean contains(Object value) {
        if (value == null) return false;
        for (int s = 0; s < size; s++) if (value.equals(heap[s])) return true;
        return false;
    }

    @Override public void clear() {
        Arrays.fill(heap, 0, size, null);
        size = 0;
        revision++;
    }

    @Override public Iterator<E> iterator() {
        // Only this explicit public projection allocates a traversal array.
        return new Iterator<>() {
            private final Object[] view = Arrays.copyOf(heap, size);
            private int next, last = -1, expected = revision;
            private void check() {
                if (revision != expected) throw new ConcurrentModificationException();
            }
            @Override public boolean hasNext() { check(); return next < view.length; }
            @Override @SuppressWarnings("unchecked") public E next() {
                if (!hasNext()) throw new NoSuchElementException();
                last = next;
                return (E) view[next++];
            }
            @Override public void remove() {
                check();
                if (last < 0) throw new IllegalStateException();
                if (!removeIdentity(view[last])) throw new ConcurrentModificationException();
                expected = revision;
                last = -1;
            }
        };
    }

    @Override public Object[] toArray() { return Arrays.copyOf(heap, size); }
    @Override @SuppressWarnings("unchecked") public <T> T[] toArray(T[] array) {
        Objects.requireNonNull(array, "array");
        if (array.length < size) return Arrays.copyOf(heap, size, (Class<? extends T[]>) array.getClass());
        System.arraycopy(heap, 0, array, 0, size);
        if (array.length > size) array[size] = null;
        return array;
    }

    /** Validate the parent/child heap order without constructing tree nodes. */
    public void verifyInvariants() {
        for (int i = 0; i < size; i++) {
            if (heap[i] == null) throw new AssertionError("empty heap offset");
            if (i > 0 && compare(at((i - 1) >>> 1), at(i)) > 0)
                throw new AssertionError("heap order at " + i);
        }
        for (int i = size; i < heap.length; i++)
            if (heap[i] != null) throw new AssertionError("stale reference at " + i);
    }

    @Override public M3PriorityQueue<E> clone() {
        M3PriorityQueue<E> copy = new M3PriorityQueue<>(comparator);
        for (int i = 0; i < size; i++) copy.offer(at(i));
        return copy;
    }
}
