<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
=======
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
// Modified 2026: DOOP offset-indexed binary priority heap; no entry or tree nodes.
package com.m3.collections;

>>>>>>> 8357d7d4bac0847b6b5bc5f74d4eff4ec9b32fc7
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
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
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
=======

/**
 * Generic min-priority queue in one dynamic Object[]; the binary tree is
 * implicit in parent=(offset-1)/2 and children=2*offset+1/2. No Entry/Node
 * objects are stored or manufactured during heap operations. Iterator
 * creation is an explicit API projection that copies references into one
 * flat Object[] and uses identity-based removal of the yielded occurrence.
>>>>>>> 8357d7d4bac0847b6b5bc5f74d4eff4ec9b32fc7
 */
public final class M3PriorityQueue<E> extends AbstractQueue<E>
        implements Cloneable, Serializable {
    @Serial private static final long serialVersionUID = 1L;
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
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
    public M3PriorityQueue(Collection<? extends E> items) {
        this(Objects.requireNonNull(items, "items").size(), null);
        addAll(items);
=======
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
>>>>>>> 8357d7d4bac0847b6b5bc5f74d4eff4ec9b32fc7
    }
    public Comparator<? super E> comparator() { return comparator; }
    public int capacity() { return heap.length; }
    @Override public int size() { return size; }
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
    public long occurrenceIdAt(int slot) { return identities[Objects.checkIndex(slot, size)]; }
    public int stateAt(int slot) { return states[Objects.checkIndex(slot, size)] & 255; }
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
=======

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
>>>>>>> 8357d7d4bac0847b6b5bc5f74d4eff4ec9b32fc7
}
