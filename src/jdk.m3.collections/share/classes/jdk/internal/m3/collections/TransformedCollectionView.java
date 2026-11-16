/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.m3.collections;

import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Function;

/**
 * Collection with a write hook: every element passes through one function on its way in and is stored, in insertion
 * order, in one array lane.
 *
 * <p>The hook is the single owner of the decorator pattern of the provider families. A transforming hook returns the
 * element to store (Commons {@code TransformedCollection}); a validating hook returns its argument or throws
 * (Commons {@code PredicatedCollection}); a guarding hook always throws (an unmodifiable view). The hook runs before
 * any state changes, and {@link #addAll} runs it on every element before the first one is stored, so a hook that
 * throws leaves the collection as it was. Reads and {@link #remove} never call the hook. No node or entry object is
 * stored; an occupancy bit per slot mirrors the live prefix of the lane. Null elements are allowed when the hook
 * allows them.</p>
 *
 * <p>Provider comparison: Apache Commons Collections {@code TransformedCollection} over an {@code ArrayList}
 * (Apache-2.0): the same results, element order and iterator failure rules for the operations declared here.
 * Not thread safe. Not a {@code java.util} replacement.</p>
 *
 * @param <E> the element type
 */
public final class TransformedCollectionView<E> {
    /** State-mask bit {@code OCCUPIED} (per-slot state bit). */
    static final long STATE_OCCUPIED = 1L << 0;
    private static final int MIN_SLOTS = 8;
    private static final int WORD_SHIFT = 6;

    /** Storage lane {@code valueLane}: the stored elements, slots 0 to size - 1, in insertion order. */
    private Object[] valueLane = new Object[0];
    /** One OCCUPIED bit per slot, 64 slots per word; exactly the bits below {@code size} are set. */
    private long[] stateMaskWords = new long[0];
    /** Live slot count. */
    private int size;
    private int modCount;
    private final Function<? super E, ? extends E> hook;

    /** Creates an empty collection that stores every element as given. */
    public TransformedCollectionView() {
        this(Function.identity());
    }

    /**
     * Creates an empty collection that stores {@code hook.apply(element)} for every added element.
     *
     * @param hook the write hook: returns the element to store, or throws to refuse the element
     * @throws NullPointerException if {@code hook} is null
     */
    public TransformedCollectionView(Function<? super E, ? extends E> hook) {
        this.hook = Objects.requireNonNull(hook, "hook");
    }

    /**
     * Reports the declared state-mask bits and lanes for ledgers; never allocates per element.
     *
     * @return the descriptor
     */
    public static String scaffoldDescriptor() {
        return "TransformedCollectionView|TRANSFORMED_COLLECTION|OBJECT|OCCUPIED|Object[] valueLane";
    }

    /**
     * Stores the hook's result for {@code element} after the last element.
     *
     * @param element the element to pass through the hook
     * @return {@code true}
     */
    public boolean add(E element) {
        Object stored = hook.apply(element);
        reserve(size + 1);
        append(stored);
        modCount++;
        return true;
    }

    /**
     * Stores the hook's results for all {@code elements}, in their iteration order. The hook sees every element
     * before the first one is stored.
     *
     * @param elements the elements to pass through the hook
     * @return whether an element was added
     * @throws NullPointerException if {@code elements} is null
     */
    public boolean addAll(Collection<? extends E> elements) {
        Object[] stored = elements.toArray();
        for (int i = 0; i < stored.length; i++) {
            stored[i] = hook.apply(element(stored[i]));
        }
        if (stored.length == 0) {
            return false;
        }
        reserve(size + stored.length);
        for (Object value : stored) {
            append(value);
        }
        modCount++;
        return true;
    }

    /**
     * Removes the first stored element equal to {@code element}; the hook is not applied.
     *
     * @param element the element to remove
     * @return whether an element was removed
     */
    public boolean remove(Object element) {
        for (int slot = 0; slot < size; slot++) {
            if (Objects.equals(element, valueLane[slot])) {
                removeSlot(slot);
                return true;
            }
        }
        return false;
    }

    /**
     * Returns an iterator over the stored elements in insertion order. It supports {@code remove} and fails with
     * {@link ConcurrentModificationException} after any other change of the collection.
     *
     * @return the iterator
     */
    public Iterator<E> iterator() {
        return new Walk();
    }

    /**
     * Returns the number of stored elements.
     *
     * @return the size
     */
    public int size() {
        return size;
    }

    @SuppressWarnings("unchecked")
    private E element(Object value) {
        return (E) value;
    }

    private void reserve(int slots) {
        if (slots < 0) {
            throw new OutOfMemoryError("TransformedCollectionView: more than Integer.MAX_VALUE elements");
        }
        if (slots > valueLane.length) {
            int capacity = Math.max(Math.max(MIN_SLOTS, slots), valueLane.length + (valueLane.length >> 1));
            valueLane = Arrays.copyOf(valueLane, capacity);
            stateMaskWords = Arrays.copyOf(stateMaskWords, (valueLane.length + (1 << WORD_SHIFT) - 1) >>> WORD_SHIFT);
        }
    }

    private void append(Object value) {
        valueLane[size] = value;
        stateMaskWords[size >>> WORD_SHIFT] |= STATE_OCCUPIED << size;
        size++;
    }

    private void removeSlot(int slot) {
        int last = size - 1;
        System.arraycopy(valueLane, slot + 1, valueLane, slot, last - slot);
        valueLane[last] = null;
        stateMaskWords[last >>> WORD_SHIFT] &= ~(STATE_OCCUPIED << last);
        size = last;
        modCount++;
    }

    /** Fail-fast cursor over the live prefix of the lane. */
    private final class Walk implements Iterator<E> {
        private int next;
        private int returned = -1;
        private int expected = modCount;

        @Override
        public boolean hasNext() {
            return next != size;
        }

        @Override
        public E next() {
            check();
            if (next >= size) {
                throw new NoSuchElementException();
            }
            returned = next++;
            return element(valueLane[returned]);
        }

        @Override
        public void remove() {
            if (returned < 0) {
                throw new IllegalStateException();
            }
            check();
            removeSlot(returned);
            next = returned;
            returned = -1;
            expected = modCount;
        }

        private void check() {
            if (expected != modCount) {
                throw new ConcurrentModificationException();
            }
        }
    }
}
