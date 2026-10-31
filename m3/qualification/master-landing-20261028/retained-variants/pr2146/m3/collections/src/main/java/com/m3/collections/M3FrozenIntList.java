/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.PrimitiveIterator;

/**
 * Immutable list of int values in encounter order over a private primitive array.
 *
 * <p>Admission copies the caller's array and preserves duplicate values. Reads and
 * primitive traversal do not box elements. Array exports are detached mutable copies;
 * mutators always throw {@link UnsupportedOperationException}. These M3-specific APIs
 * preserve the selected JDK immutable-list operation contracts, without implementing
 * the boxed {@code java.util.List} interface.
 *
 * <p>Storage is one dense int lane and a shared owner state word, with no retained
 * per-element nodes or entries. First-party Synexia implementation, Apache-2.0;
 * JDK and Guava are behavioral references, with no donor implementation source copied.
 */
public final class M3FrozenIntList {
    private static final int[] EMPTY = new int[0];
    /** Owner state: every element is immutable. */
    static final long STATE_FROZEN = 1L << 0;
    private static final long[] FROZEN_STATE = {STATE_FROZEN};

    /** Immutable payload lane; never exposed to callers. */
    private final int[] valueLane;
    /** Shared immutable owner state; dense occupancy needs no per-slot mask. */
    private final long[] stateMaskWords = FROZEN_STATE;
    /** Live slot count. */
    private final int size;

    /** Creates an empty immutable list. */
    public M3FrozenIntList() { this(EMPTY); }

    private M3FrozenIntList(int[] frozen) {
        valueLane = frozen;
        size = frozen.length;
    }

    /** Reports the declared state-mask bits and lanes for ledgers; never allocates per element. */
    public static String scaffoldDescriptor() {
        return "M3FrozenIntList|FROZEN_INT_LIST|INT|FROZEN|int[] valueLane";
    }

    /** JDK List.copyOf (int lane). */
    public static M3FrozenIntList copyOf(int[] source) {
        Objects.requireNonNull(source, "source");
        return new M3FrozenIntList(source.clone());
    }

    /** List.size. */
    public int size() {
        return size;
    }

    /** List.get (primitive). */
    public int get(int index) {
        return valueLane[Objects.checkIndex(index, size)];
    }

    /** List.indexOf. */
    public int indexOf(int value) {
        for (int i = 0; i < size; i++) if (valueLane[i] == value) return i;
        return -1;
    }

    /** List.toArray (defensive copy). */
    public int[] toArray() {
        return valueLane.clone();
    }

    /** Iterable.forEach. */
    public void forEach(java.util.function.IntConsumer action) {
        Objects.requireNonNull(action, "action");
        for (int i = 0; i < size; i++) action.accept(valueLane[i]);
    }

    /** List.iterator. */
    public java.util.PrimitiveIterator.OfInt iterator() {
        final int[] values = valueLane;
        return new PrimitiveIterator.OfInt() {
            private int cursor;
            @Override public boolean hasNext() { return cursor < values.length; }
            @Override public int nextInt() {
                if (cursor >= values.length) throw new NoSuchElementException();
                return values[cursor++];
            }
        };
    }

    /** Immutable: always throws {@link UnsupportedOperationException}. */
    public boolean add(int value) {
        throw new UnsupportedOperationException();
    }
}
