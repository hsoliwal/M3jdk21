/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.PrimitiveIterator;
import java.util.function.IntConsumer;
import jdk.internal.m3.collections.LaneSearch;

/**
 * Immutable, ascending, duplicate-free int values in one dense primitive lane.
 *
 * <p>Construction copies the source and collapses duplicates, as Set.copyOf does.
 * Ascending ordinal order is an explicit M3 contract. This class is not a boxed
 * java.util.Set implementation. Input and exported arrays never alias the owner.
 * The existing FrozenLongSet and LaneSearch supply the collection algorithms;
 * no provider implementation source is copied.
 *
 * <p>Storage is one int array and a shared frozen state word. There are no
 * retained entry/node objects, per-slot state arrays or redundant hash indexes.
 */
public final class FrozenIntSet {
    static final long STATE_FROZEN = 1L << 0;
    private static final int[] EMPTY_LANE = new int[0];
    private static final long[] FROZEN_STATE = {STATE_FROZEN};
    private static final FrozenIntSet EMPTY = new FrozenIntSet();

    private final int[] valueLane;
    private final long[] stateMaskWords = FROZEN_STATE;
    private final int size;

    /** Creates an empty immutable set. */
    public FrozenIntSet() {
        this(EMPTY_LANE);
    }

    private FrozenIntSet(int[] values) {
        valueLane = values;
        size = values.length;
    }

    /** Historical descriptor identity retained for existing callers. */
    public static String scaffoldDescriptor() {
        return "M3FrozenIntSet|FROZEN_INT_SET|INT|FROZEN|int[] valueLane";
    }

    /** Sorted defensive copy; duplicates collapse and a null source is rejected. */
    public static FrozenIntSet copyOf(int[] source) {
        Objects.requireNonNull(source, "source");
        if (source.length == 0) return EMPTY;
        int[] values = source.clone();
        Arrays.sort(values);
        int count = 1;
        for (int index = 1; index < values.length; index++) {
            if (values[index] != values[count - 1]) values[count++] = values[index];
        }
        return new FrozenIntSet(count == values.length ? values : Arrays.copyOf(values, count));
    }

    public int size() {
        return size;
    }

    public boolean contains(int value) {
        int index = LaneSearch.lowerBound(valueLane, 0, size, value);
        return index < size && valueLane[index] == value;
    }

    /** Value at the zero-based ascending ordinal. */
    public int get(int ordinal) {
        return valueLane[Objects.checkIndex(ordinal, size)];
    }

    public int[] toArray() {
        return valueLane.clone();
    }

    public void forEach(IntConsumer action) {
        Objects.requireNonNull(action, "action");
        for (int index = 0; index < size; index++) action.accept(valueLane[index]);
    }

    /** One cursor per traversal; no allocation or boxing per element. */
    public PrimitiveIterator.OfInt iterator() {
        return new PrimitiveIterator.OfInt() {
            private int cursor;

            @Override public boolean hasNext() {
                return cursor < size;
            }

            @Override public int nextInt() {
                if (cursor >= size) throw new NoSuchElementException();
                return valueLane[cursor++];
            }
        };
    }

    /** Always rejected, including an already-present value. */
    public boolean add(int value) {
        throw new UnsupportedOperationException("M3FrozenIntSet is immutable");
    }
}
