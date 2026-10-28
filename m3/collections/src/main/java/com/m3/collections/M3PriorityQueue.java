/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
import java.util.function.Predicate;

/**
 * M3-owned reference-type min heap, with a GC-traced Object[] payload lane,
 * byte[] physical state masks, and long[] occurrence identities for exact
 * iterator removal. It does not wrap, copy from, or delegate to a vendor queue.
 *
 * <p>Priorities use the supplied Comparator, or natural ordering. Insertions,
 * removals and bulk compaction plan comparator work before mutating the owner,
 * so comparator exceptions do not leave an inconsistent heap. Iterators are
 * fail-fast, snapshot primitive occurrence identities and reference payloads,
 * but never allocate a node/entry per queue element. Not thread-safe.
 */
public final class M3PriorityQueue<E> extends AbstractQueue<E>
        implements Cloneable, Serializable {
    @Serial private static final long serialVersionUID = 1L;
    public static final int LIVE = 1;
    public static final int VISIBLE = 2;
    public static final int DIRTY = 4;
    public static final int DEFERRED = 8;
    private static final int MAX_CAPACITY = Integer.MAX_VALUE - 8;

    private transient Object[] heap;
    private transient byte[] states;
    private transient long[] identities;
    @SuppressWarnings("serial") // JDK-compatible comparator may not be Serializable.
    private final Comparator<? super E> comparator;
    private transient int size;
    private transient int changes;
    private transient long sequence;

    public M3PriorityQueue() { this(0, null); }
    public M3PriorityQueue(Comparator<? super E> comparator) { this(0, comparator); }
    public M3PriorityQueue(int capacity) { this(capacity, null); }
    public M3PriorityQueue(int capacity, Comparator<? super E> comparator) {
        if (capacity < 0 || capacity > MAX_CAPACITY) {
            throw new IllegalArgumentException("invalid initial capacity");
        }
        heap = capacity == 0 ? M3SlotArrays.EMPTY_OBJECTS : new Object[capacity];
        states = capacity == 0 ? M3SlotArrays.EMPTY_BYTES : new byte[capacity];
        identities = new long[capacity];
        this.comparator = comparator;
    }
    /** Like java.util.PriorityQueue: a SortedSet or priority-queue source passes its ordering on. */
    public M3PriorityQueue(Collection<? extends E> items) {
        this(Objects.requireNonNull(items, "items").size(), sourceComparator(items));
        addAll(items);
    }
    // Atom ported from the PR #1330 variant (retained under qualification/master-landing-20261028/retained-variants).
    @SuppressWarnings("unchecked")
    private static <E> Comparator<? super E> sourceComparator(Collection<? extends E> source) {
        if (source instanceof java.util.SortedSet<?> sorted) return (Comparator<? super E>) sorted.comparator();
        if (source instanceof java.util.PriorityQueue<?> queue) return (Comparator<? super E>) queue.comparator();
        if (source instanceof M3PriorityQueue<?> queue) return (Comparator<? super E>) queue.comparator();
        return null;
    }
    public Comparator<? super E> comparator() { return comparator; }
    public int capacity() { return heap.length; }
    @Override public int size() { return size; }
    public long occurrenceIdAt(int slot) { return identities[Objects.checkIndex(slot, size)]; }
    public int stateAt(int slot) { return states[Objects.checkIndex(slot, size)] & 255; }
    /**
     * Checks the three lanes (test use, package-private): each slot below the size holds a payload, the LIVE flag
     * and an occurrence identity in 1..sequence, in heap order; every slot from the size on is cleared.
     */
    void verifyInvariants() {
        if (size < 0 || size > heap.length || states.length != heap.length || identities.length != heap.length) {
            throw new AssertionError("lane lengths");
        }
        for (int i = 0; i < heap.length; i++) {
            boolean live = i < size;
            if (live != (heap[i] != null) || live != ((states[i] & LIVE) != 0) || (live
                    ? identities[i] <= 0 || identities[i] > sequence : identities[i] != 0 || states[i] != 0)) {
                throw new AssertionError("lane state at " + i);
            }
            if (live && i > 0 && compare(get((i - 1) >>> 1), get(i)) > 0) throw new AssertionError("heap order at " + i);
        }
    }
    private static void userBits(int bits) {
        if ((bits & ~0xfe) != 0) throw new IllegalArgumentException("flags must use bits 1..7");
    }
    private static void selectedBits(int required, int forbidden) {
        if (((required | forbidden) & ~255) != 0 || (required & forbidden) != 0) {
            throw new IllegalArgumentException("invalid selection masks");
        }
    }
    public int updateState(int slot, int setBits, int clearBits) {
        userBits(setBits); userBits(clearBits);
        if ((setBits & clearBits) != 0) throw new IllegalArgumentException("overlapping state bits");
        int position = Objects.checkIndex(slot, size);
        int old = states[position] & 255;
        int next = (old | setBits) & ~clearBits;
        if (old != next) { states[position] = (byte) next; changes++; }
        return old;
    }
    /** Return a detached index list of the current heap-physical positions. */
    public int[] selectedPhysicalIndices(int required, int forbidden,
            M3IndexMaskCompactor.Backend backend) {
        selectedBits(required, forbidden);
        Objects.requireNonNull(backend, "backend");
        if (backend == M3IndexMaskCompactor.Backend.NATIVE_REQUIRED) {
            int[] positions = new int[size]; byte[] admitted = new byte[size];
            for (int i = 0; i < size; i++) {
                int flags = states[i] & 255;
                positions[i] = i;
                admitted[i] = (byte) (((flags & required) == required
                        && (flags & forbidden) == 0) ? 1 : 0);
            }
            return M3IndexMaskCompactor.select(positions, admitted, 1, backend);
        }
        int count = 0;
        for (int i = 0; i < size; i++) {
            int flags = states[i] & 255;
            if ((flags & required) == required && (flags & forbidden) == 0) count++;
        }
        int[] positions = new int[count];
        for (int i = 0, written = 0; i < size; i++) {
            int flags = states[i] & 255;
            if ((flags & required) == required && (flags & forbidden) == 0) positions[written++] = i;
        }
        return positions;
    }
    @SuppressWarnings("unchecked")
    private int compare(E a, E b) {
        return comparator == null ? ((Comparable<? super E>) a).compareTo(b) : comparator.compare(a, b);
    }
    /** A comparator is a user callback, never permitted to publish a stale heap plan. */
    private int compareChecked(E left, E right, int expectedChanges) {
        int result = compare(left, right);
        if (changes != expectedChanges) {
            throw new ConcurrentModificationException("comparator modified M3 priority queue");
        }
        return result;
    }
    @SuppressWarnings("unchecked") private E get(int index) { return (E) heap[index]; }
    private void grow(int needed) {
        if (needed <= heap.length) return;
        if (needed > MAX_CAPACITY) throw new OutOfMemoryError("M3 PriorityQueue capacity exhausted");
        int next = heap.length;
        do { next = M3SlotArrays.grow(next); } while (next < needed);
        Object[] nextHeap = Arrays.copyOf(heap, next);
        byte[] nextStates = Arrays.copyOf(states, next);
        long[] nextIds = Arrays.copyOf(identities, next);
        heap = nextHeap; states = nextStates; identities = nextIds;
    }
    @Override public boolean offer(E item) {
        Objects.requireNonNull(item, "item");
        if (comparator == null && !(item instanceof Comparable<?>)) {
            throw new ClassCastException("natural priority requires Comparable elements");
        }
        if (sequence == Long.MAX_VALUE) throw new IllegalStateException("identity space exhausted");
        int dest = size;
        int expectedChanges = changes;
        // Plan ALL user comparator calls before touching the heap.
        while (dest > 0 && compareChecked(item, get((dest - 1) >>> 1), expectedChanges) < 0) {
            dest = (dest - 1) >>> 1;
        }
        grow(size + 1);
        for (int current = size; current > dest;) {
            int parent = (current - 1) >>> 1;
            move(parent, current);
            current = parent;
        }
        heap[dest] = item; states[dest] = LIVE; identities[dest] = ++sequence;
        size++; changes++;
        return true;
    }
    private void move(int from, int to) {
        heap[to] = heap[from]; states[to] = states[from]; identities[to] = identities[from];
    }
    private void empty(int index) { heap[index] = null; states[index] = 0; identities[index] = 0; }
    @Override public E peek() { return size == 0 ? null : get(0); }
    @Override public E poll() { return size == 0 ? null : erase(0); }
    /** Delete the exact stored occurrence. All comparisons are preflighted. */
    private E erase(int index) {
        E result = get(index);
        int last = size - 1;
        if (index == last) {
            empty(last); size--; changes++; return result;
        }
        E replacement = get(last);
        byte replacementFlags = states[last];
        long replacementId = identities[last];
        int dest = index;
        int expectedChanges = changes;
        if (index > 0 && compareChecked(replacement, get((index - 1) >>> 1), expectedChanges) < 0) {
            // Sift towards root; comparator failure leaves every lane unchanged.
            while (dest > 0 && compareChecked(replacement, get((dest - 1) >>> 1), expectedChanges) < 0) {
                dest = (dest - 1) >>> 1;
            }
            for (int current = index; current > dest;) {
                int parent = (current - 1) >>> 1;
                move(parent, current);
                current = parent;
            }
        } else {
            // Sift to smallest child; find a final descendant before moving data.
            while (dest < last) {
                long firstChild = 2L * dest + 1;
                if (firstChild >= last) break;
                int child = (int) firstChild;
                if (child + 1 < last && compareChecked(get(child + 1), get(child), expectedChanges) < 0) child++;
                if (compareChecked(replacement, get(child), expectedChanges) <= 0) break;
                dest = child;
            }
            int current = index;
            int depth = (31 - Integer.numberOfLeadingZeros(dest + 1))
                    - (31 - Integer.numberOfLeadingZeros(index + 1));
            for (int shift = depth - 1; shift >= 0; shift--) {
                int child = 2 * current + 1 + (((dest + 1) >>> shift) & 1);
                move(child, current);
                current = child;
            }
        }
        heap[dest] = replacement; states[dest] = replacementFlags;
        identities[dest] = replacementId;
        empty(last); size--; changes++;
        return result;
    }
    @Override public boolean remove(Object element) {
        if (element == null) return false;
        for (int i = 0; i < size; i++) {
            if (element.equals(heap[i])) { erase(i); return true; }
        }
        return false;
    }
    @Override public boolean contains(Object wanted) {
        if (wanted == null) return false;
        for (int i = 0; i < size; i++) if (wanted.equals(heap[i])) return true;
        return false;
    }
    @Override public void clear() {
        if (size == 0) return;
        Arrays.fill(heap, 0, size, null);
        Arrays.fill(states, 0, size, (byte) 0);
        Arrays.fill(identities, 0, size, 0L);
        size = 0; changes++;
    }
    @Override public Object[] toArray() { return Arrays.copyOf(heap, size); }
    @SuppressWarnings("unchecked")
    @Override public <T> T[] toArray(T[] array) {
        Objects.requireNonNull(array, "array");
        T[] result = array.length >= size ? array
                : (T[]) java.lang.reflect.Array.newInstance(array.getClass().getComponentType(), size);
        System.arraycopy(heap, 0, result, 0, size);
        if (result.length > size) result[size] = null;
        return result;
    }
    /** Build a prepared replacement heap first; predicate/comparator failures are atomic. */
    @Override public boolean removeIf(Predicate<? super E> predicate) {
        Objects.requireNonNull(predicate, "predicate");
        final int expected = changes;
        Object[] nextValues = new Object[size];
        byte[] nextStates = new byte[size];
        long[] nextIds = new long[size];
        int retained = 0;
        for (int i = 0; i < size; i++) {
            boolean remove = predicate.test(get(i));
            if (expected != changes) throw new ConcurrentModificationException();
            if (!remove) {
                nextValues[retained] = heap[i]; nextStates[retained] = states[i];
                nextIds[retained] = identities[i]; retained++;
            }
        }
        if (retained == size) return false;
        // Bottom-up heapification works on detached parallel arrays.
        for (int i = retained / 2 - 1; i >= 0; i--) {
            int dest = i;
            @SuppressWarnings("unchecked") E value = (E) nextValues[i];
            byte flags = nextStates[i]; long id = nextIds[i];
            while (2L * dest + 1 < retained) {
                int child = 2 * dest + 1;
                if (child + 1 < retained && compareChecked(cast(nextValues[child + 1]),
                        cast(nextValues[child]), expected) < 0) child++;
                if (compareChecked(value, cast(nextValues[child]), expected) <= 0) break;
                nextValues[dest] = nextValues[child]; nextStates[dest] = nextStates[child];
                nextIds[dest] = nextIds[child]; dest = child;
            }
            nextValues[dest] = value; nextStates[dest] = flags; nextIds[dest] = id;
        }
        heap = nextValues; states = nextStates; identities = nextIds;
        size = retained; changes++;
        return true;
    }
    @SuppressWarnings("unchecked") private E cast(Object item) { return (E) item; }

    /** Snapshot occurrence order, not a result-value adapter or node list. */
    @Override public Iterator<E> iterator() {
        Object[] observedValues = Arrays.copyOf(heap, size);
        long[] observedIds = Arrays.copyOf(identities, size);
        return new Iterator<>() {
            private int cursor, last = -1;
            private int expected = changes;
            private void live() { if (expected != changes) throw new ConcurrentModificationException(); }
            @Override public boolean hasNext() { live(); return cursor < observedIds.length; }
            @Override public E next() {
                live(); if (!hasNext()) throw new NoSuchElementException();
                last = cursor;
                @SuppressWarnings("unchecked") E result = (E) observedValues[cursor++];
                return result;
            }
            @Override public void remove() {
                live(); if (last < 0) throw new IllegalStateException("next required");
                long token = observedIds[last];
                int index = -1;
                for (int i = 0; i < size; i++) if (identities[i] == token) { index = i; break; }
                if (index < 0) throw new ConcurrentModificationException("occurrence removed");
                erase(index);
                expected = changes; last = -1;
            }
        };
    }
    /** Returns priorities in removal order, but does not mutate this heap. */
    public Object[] toSortedArray() {
        M3PriorityQueue<E> copy = clone();
        Object[] result = new Object[copy.size];
        for (int i = 0; i < result.length; i++) result[i] = copy.poll();
        return result;
    }
    @Override public M3PriorityQueue<E> clone() {
        M3PriorityQueue<E> copy = new M3PriorityQueue<>(size, comparator);
        copy.heap = Arrays.copyOf(heap, size);
        copy.states = Arrays.copyOf(states, size);
        copy.identities = Arrays.copyOf(identities, size);
        copy.size = size; copy.sequence = sequence;
        return copy;
    }
    @Serial private void writeObject(ObjectOutputStream output) throws IOException {
        int expected = changes;
        output.defaultWriteObject();
        output.writeInt(size);
        for (int i = 0; i < size; i++) {
            output.writeObject(heap[i]); output.writeByte(states[i]);
        }
        if (expected != changes) throw new ConcurrentModificationException();
    }
    @Serial private void readObject(ObjectInputStream input) throws IOException, ClassNotFoundException {
        input.defaultReadObject();
        int count = input.readInt();
        if (count < 0 || count > MAX_CAPACITY) throw new InvalidObjectException("invalid queue size");
        heap = new Object[count]; states = new byte[count]; identities = new long[count];
        size = 0; changes = 0; sequence = 0;
        for (int i = 0; i < count; i++) {
            @SuppressWarnings("unchecked") E value = (E) input.readObject();
            byte flags = input.readByte();
            if (value == null || (flags & LIVE) == 0) throw new InvalidObjectException("invalid heap entry");
            offer(value);
            // Occurrences reorder during heap insert; locate the newly installed ID.
            for (int pos = 0; pos < size; pos++) {
                if (identities[pos] == sequence) { states[pos] = flags; break; }
            }
        }
        changes = 0;
    }
}
