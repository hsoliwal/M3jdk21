/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.m3.collections;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Positional object list using a shared primitive rank-index owner.
 *
 * <p>An {@link IndexedLongList} stores stable integer slot IDs in an
 * order-statistics AVL tree. Elements live only in this parent's dynamic
 * Object[] lane; next/previous integer offset lanes provide O(1) sequential
 * traversal and O(n) equality search. Inserting/removing by rank takes
 * O(log n) worst-case, and get/set take O(log n). No per-element object
 * wrapper, link node, or iterator is retained by the collection.</p>
 *
 * <p>Two slot-state bits (occupied/deleted) are packed into each 64-bit word,
 * 32 physical slots per word. AVL balance lives in the delegated order index,
 * which also recycles its own primitive node slots. This is an M3-specific
 * extension, not an implementation of every Apache Commons TreeList API.</p>
 */
public final class IndexedList<E> {
    /** Historic public inventory constants, retained as-is. */
    static final long STATE_OCCUPIED = 1L << 0;
    static final long STATE_DELETED = 1L << 1;
    static final long STATE_BALANCE = 1L << 2;
    private static final int MAX_CAPACITY = Integer.MAX_VALUE - 8;
    private static final long SLOT_STATE_MASK = 3L;

    /** Rank-to-slot tree: all AVL links/heights/subtree sizes are primitive. */
    private final IndexedLongList orderLane = new IndexedLongList();
    /** One canonical reference per live slot, immediately nulled on release. */
    private Object[] valueLane = new Object[0];
    private int[] nextLane = new int[0];
    /** Also carries the free-list link on deleted slots. */
    private int[] previousLane = new int[0];
    /** Two bits per slot, 32 physical slots in a word. */
    private long[] stateMaskWords = new long[0];
    private int firstSlot;
    private int lastSlot;
    private int allocated;
    private int freeHead;
    private int modCount;

    public IndexedList() { }

    /**
     * Historic inventory token retained byte-for-byte for consumers that
     * previously recorded the scaffold. Actual rank links are now composed
     * inside IndexedLongList; this owner adds the object and traversal lanes.
     */
    public static String scaffoldDescriptor() {
        return "M3IndexedList|INDEXED_LIST|OBJECT|OCCUPIED,DELETED,BALANCE|Object[] valueLane;int[] leftLane;int[] rightLane;int[] sizeLane";
    }

    @SuppressWarnings("unchecked")
    public E get(int index) {
        return (E) valueLane[(int) orderLane.get(index)];
    }

    /** Insert before the current logical index; size() is valid for append. */
    public void add(int index, E element) {
        int oldSize = size();
        if (index < 0 || index > oldSize) throw new IndexOutOfBoundsException(index);
        if (oldSize == MAX_CAPACITY) throw new OutOfMemoryError("indexed list exhausted");

        int before = index == 0 ? 0 : (int) orderLane.get(index - 1);
        int after = index == oldSize ? 0 : (int) orderLane.get(index);
        int slot = allocate(element);
        try {
            orderLane.add(index, slot);
        } catch (RuntimeException | Error failure) {
            release(slot);
            throw failure;
        }
        nextLane[slot] = after;
        previousLane[slot] = before;
        if (before == 0) firstSlot = slot;
        else nextLane[before] = slot;
        if (after == 0) lastSlot = slot;
        else previousLane[after] = slot;
        modCount++;
    }

    /** Append at the end of the positional sequence. */
    public boolean add(E element) {
        add(size(), element);
        return true;
    }

    @SuppressWarnings("unchecked")
    public E set(int index, E element) {
        int slot = (int) orderLane.get(index);
        E previous = (E) valueLane[slot];
        valueLane[slot] = element;
        return previous;
    }

    /** Remove at rank and release the reference, reusing its offset later. */
    @SuppressWarnings("unchecked")
    public E remove(int index) {
        int slot = (int) orderLane.removeAt(index);
        E removed = (E) valueLane[slot];
        int before = previousLane[slot];
        int after = nextLane[slot];
        if (before == 0) firstSlot = after;
        else nextLane[before] = after;
        if (after == 0) lastSlot = before;
        else previousLane[after] = before;
        release(slot);
        modCount++;
        return removed;
    }

    /** Linear equality search, including null, via contiguous integer links. */
    public int indexOf(Object element) {
        int rank = 0;
        for (int slot = firstSlot; slot != 0; slot = nextLane[slot]) {
            if (Objects.equals(valueLane[slot], element)) return rank;
            rank++;
        }
        return -1;
    }

    /** Lazily materialized bidirectional fail-fast cursor. */
    public ListIterator<E> listIterator(int index) {
        if (index < 0 || index > size()) throw new IndexOutOfBoundsException(index);
        return new IndexedIterator(index);
    }

    public int size() {
        return orderLane.size();
    }

    /** Drop all retained references and reset slot/high-water state. */
    public void clear() {
        if (size() == 0) return;
        orderLane.clear();
        Arrays.fill(valueLane, 1, allocated + 1, null);
        Arrays.fill(nextLane, 1, allocated + 1, 0);
        Arrays.fill(previousLane, 1, allocated + 1, 0);
        Arrays.fill(stateMaskWords, 0L);
        firstSlot = lastSlot = allocated = freeHead = 0;
        modCount++;
    }

    private int allocate(Object value) {
        if (freeHead == 0 && allocated >= valueLane.length - 1) grow();
        int slot;
        if (freeHead != 0) {
            slot = freeHead;
            freeHead = previousLane[slot];
        } else {
            slot = ++allocated;
        }
        valueLane[slot] = value;
        nextLane[slot] = previousLane[slot] = 0;
        setState(slot, (int) STATE_OCCUPIED);
        return slot;
    }

    private void release(int slot) {
        valueLane[slot] = null;
        nextLane[slot] = 0;
        previousLane[slot] = freeHead;
        freeHead = slot;
        setState(slot, (int) STATE_DELETED);
    }

    private void setState(int slot, int state) {
        int zeroBased = slot - 1;
        int word = zeroBased >>> 5;
        int shift = (zeroBased & 31) << 1;
        long mask = SLOT_STATE_MASK << shift;
        stateMaskWords[word] = (stateMaskWords[word] & ~mask) | ((long) state << shift);
    }

    /** Copy-and-publish of all aligned slot lanes; never a partial growth. */
    private void grow() {
        int previous = valueLane.length - 1;
        if (previous >= MAX_CAPACITY) throw new OutOfMemoryError("indexed list exhausted");
        long proposed = previous < 0 ? 8 : Math.max(8L, previous + (previous >> 1) + 1L);
        int next = (int) Math.min(MAX_CAPACITY, proposed);
        Object[] newValues = Arrays.copyOf(valueLane, next + 1);
        int[] newNext = Arrays.copyOf(nextLane, next + 1);
        int[] newPrevious = Arrays.copyOf(previousLane, next + 1);
        long[] newStates = Arrays.copyOf(stateMaskWords, (int) ((next + 31L) >>> 5));
        valueLane = newValues;
        nextLane = newNext;
        previousLane = newPrevious;
        stateMaskWords = newStates;
    }

    private final class IndexedIterator implements ListIterator<E> {
        private int cursor;
        private int nextSlot;
        private int lastReturned;
        private boolean movedForward;
        private int expectedModCount = modCount;

        IndexedIterator(int index) {
            cursor = index;
            nextSlot = index == size() ? 0 : (int) orderLane.get(index);
        }

        @Override
        public boolean hasNext() {
            return cursor < size();
        }

        @Override
        public E next() {
            checkForComodification();
            if (!hasNext()) throw new NoSuchElementException();
            lastReturned = nextSlot;
            nextSlot = nextLane[lastReturned];
            cursor++;
            movedForward = true;
            return getSlot(lastReturned);
        }

        @Override
        public boolean hasPrevious() {
            return cursor > 0;
        }

        @Override
        public E previous() {
            checkForComodification();
            if (!hasPrevious()) throw new NoSuchElementException();
            int slot = nextSlot == 0 ? lastSlot : previousLane[nextSlot];
            nextSlot = lastReturned = slot;
            cursor--;
            movedForward = false;
            return getSlot(slot);
        }

        @Override
        public int nextIndex() { return cursor; }

        @Override
        public int previousIndex() { return cursor - 1; }

        @Override
        public void remove() {
            checkForComodification();
            if (lastReturned == 0) throw new IllegalStateException();
            int afterRemoved = nextLane[lastReturned];
            IndexedList.this.remove(movedForward ? cursor - 1 : cursor);
            if (movedForward) cursor--;
            else nextSlot = afterRemoved;
            lastReturned = 0;
            expectedModCount = modCount;
        }

        @Override
        public void set(E element) {
            checkForComodification();
            if (lastReturned == 0) throw new IllegalStateException();
            valueLane[lastReturned] = element;
        }

        @Override
        public void add(E element) {
            checkForComodification();
            IndexedList.this.add(cursor, element);
            cursor++;
            lastReturned = 0;
            expectedModCount = modCount;
        }

        @SuppressWarnings("unchecked")
        private E getSlot(int slot) {
            return (E) valueLane[slot];
        }

        private void checkForComodification() {
            if (modCount != expectedModCount) throw new ConcurrentModificationException();
        }
    }
}
