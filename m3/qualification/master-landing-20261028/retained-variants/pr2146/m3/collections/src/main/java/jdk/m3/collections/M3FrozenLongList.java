/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.PrimitiveIterator;

/**
 * Immutable list of long values in encounter order over a private primitive array.
 *
 * <p>Admission copies the caller's array and preserves duplicate values. Reads and
 * primitive traversal do not box elements. Array exports are detached mutable copies;
 * mutators always throw {@link UnsupportedOperationException}. These M3-specific APIs
 * preserve the selected JDK immutable-list operation contracts, without implementing
 * the boxed {@code java.util.List} interface.
 *
 * <p>Storage is one dense long lane and a shared owner state word, with no retained
 * per-element nodes or entries. First-party Synexia implementation, Apache-2.0;
 * JDK and Guava are behavioral references, with no donor implementation source copied.
 *
 * <p>Nonempty sublists share the immutable lane through checked offsets. Proper empty
 * slices use the empty lane, so they do not retain a larger parent array. Invalid-range
 * exception precedence follows the JDK 21 {@code List.copyOf} implementation; Guava's
 * reversed-range exception differs and is not claimed as equivalent.
 */
public final class M3FrozenLongList {
    private static final long[] EMPTY = new long[0];
    /** Owner state: every element is immutable. */
    static final long STATE_FROZEN = 1L << 0;
    private static final long[] FROZEN_STATE = {STATE_FROZEN};

    /** Immutable payload lane; never exposed to callers. */
    private final long[] valueLane;
    /** Shared immutable owner state; dense occupancy needs no per-slot mask. */
    private final long[] stateMaskWords = FROZEN_STATE;
    /** Live slot count. */
    private final int size;
    private final int offset;

    /** Creates an empty immutable list. */
    public M3FrozenLongList() { this(EMPTY, 0, 0); }

    private M3FrozenLongList(long[] frozen, int from, int length) {
        valueLane = frozen;
        offset = from;
        size = length;
    }

    /** Reports the declared state-mask bits and lanes for ledgers; never allocates per element. */
    public static String scaffoldDescriptor() {
        return "M3FrozenLongList|FROZEN_LONG_LIST|LONG|FROZEN|long[] valueLane";
    }

    /** JDK List.copyOf / Guava ImmutableList.copyOf. */
    public static M3FrozenLongList copyOf(long[] source) {
        Objects.requireNonNull(source, "source");
        return new M3FrozenLongList(source.clone(), 0, source.length);
    }

    /** List.size. */
    public int size() {
        return size;
    }

    /** List.isEmpty. */
    public boolean isEmpty() {
        return size == 0;
    }

    /** List.get (primitive). */
    public long get(int index) {
        return valueLane[offset + Objects.checkIndex(index, size)];
    }

    /** List.indexOf. */
    public int indexOf(long value) {
        for (int i = 0; i < size; i++) if (valueLane[offset + i] == value) return i;
        return -1;
    }

    /** List.contains. */
    public boolean contains(long value) {
        return indexOf(value) >= 0;
    }

    /** List.toArray (defensive copy). */
    public long[] toArray() {
        return Arrays.copyOfRange(valueLane, offset, offset + size);
    }

    /** List.subList (frozen view). */
    public M3FrozenLongList subList(int fromIndex, int toIndex) {
        if (fromIndex < 0) throw new IndexOutOfBoundsException("fromIndex: " + fromIndex);
        if (toIndex > size) throw new IndexOutOfBoundsException("toIndex: " + toIndex);
        if (fromIndex > toIndex) throw new IllegalArgumentException("fromIndex > toIndex");
        if (fromIndex == 0 && toIndex == size) return this;
        if (fromIndex == toIndex) return new M3FrozenLongList(EMPTY, 0, 0);
        return new M3FrozenLongList(valueLane, offset + fromIndex, toIndex - fromIndex);
    }

    /** Iterable.forEach. */
    public void forEach(java.util.function.LongConsumer action) {
        Objects.requireNonNull(action, "action");
        for (int i = 0; i < size; i++) action.accept(valueLane[offset + i]);
    }

    /** List.iterator. */
    public java.util.PrimitiveIterator.OfLong iterator() {
        final long[] values = valueLane;
        final int from = offset, end = offset + size;
        return new PrimitiveIterator.OfLong() {
            private int cursor = from;
            @Override public boolean hasNext() { return cursor < end; }
            @Override public long nextLong() {
                if (cursor >= end) throw new NoSuchElementException();
                return values[cursor++];
            }
        };
    }

    /** Immutable: always throws {@link UnsupportedOperationException}. */
    public boolean add(long value) {
        throw new UnsupportedOperationException();
    }

    /** Immutable: always throws {@link UnsupportedOperationException}. */
    public long set(int index, long value) {
        throw new UnsupportedOperationException();
    }
}
