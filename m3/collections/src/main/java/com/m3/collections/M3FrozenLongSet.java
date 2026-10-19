/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.PrimitiveIterator;
import java.util.function.LongConsumer;

/**
 * Immutable, sorted, duplicate-free set of {@code long} values over one dense value lane.
 *
 * <p>Provider lineage (LIBRARY_API_MATRIX.tsv, run 20261017-synexia-to-public/LIB-SUPERSET, leaf LS1):
 * <ul>
 *   <li>JDK {@code Set.copyOf}: immutable; mutators throw {@link UnsupportedOperationException}; duplicates collapse;
 *       {@code equals}/{@code hashCode} follow the {@code java.util.Set} contract (sum of {@code Long.hashCode}).</li>
 *   <li>GUAVA {@code ImmutableSortedSet}: natural ascending order; {@code first}/{@code last} throw
 *       {@link NoSuchElementException} when empty; {@code asList().get}/{@code indexOf} as {@link #get}/{@link #rank}.</li>
 *   <li>ECLIPSE {@code ImmutableLongSet}: primitive membership, size and sorted export without boxing.</li>
 * </ul>
 * Each provider contract is qualified only against that provider's own oracle; no equivalence between providers is
 * inferred. Iteration order is ascending, which is an M3 guarantee beyond the JDK contract (unspecified order there).
 *
 * <p>DOOP mapping: lane kind LONG; storage is the single {@code valueLane} (sorted distinct values, length = size) plus
 * the shared owner state word {@code FROZEN}; offsets are sorted ordinals located by binary search. Stored nodes: NONE.
 * Per-element allocation: 0 (construction allocates the one lane; queries, ordinal access, rank and {@code forEach}
 * allocate nothing; an {@link #iterator()} allocates one cursor per traversal, never per element). Donor:
 * first-party Synexia {@code PackedFrozenLongSet} (blob e2efeecf9c03), adapted. Not a {@code java.util} replacement.
 */
public final class M3FrozenLongSet {
    /** Owner state bit {@code FROZEN}: every slot of the lane is immutable. */
    static final long STATE_FROZEN = 1L << 0;
    private static final long[] EMPTY_LANE = new long[0];
    private static final long[] FROZEN_STATE = {STATE_FROZEN};
    private static final M3FrozenLongSet EMPTY = new M3FrozenLongSet();

    /** Storage lane {@code valueLane}: sorted distinct values. */
    private final long[] valueLane;
    /** Owner state word (FROZEN); shared, never mutated; occupancy is implicit (dense lane). */
    private final long[] stateMaskWords;
    /** Live slot count. */
    private final int size;

    /** Creates an empty frozen set. */
    public M3FrozenLongSet() {
        this(EMPTY_LANE, 0);
    }

    private M3FrozenLongSet(long[] valueLane, int size) {
        this.valueLane = valueLane;
        this.stateMaskWords = FROZEN_STATE;
        this.size = size;
    }

    /** Reports the declared state-mask bits and lanes for ledgers; never allocates per element. */
    public static String scaffoldDescriptor() {
        return "M3FrozenLongSet|FROZEN_LONG_SET|LONG|FROZEN|long[] valueLane|RECEIVED";
    }

    /** JDK Set.copyOf / Guava ImmutableSortedSet.copyOf: sorted copy, duplicates collapsed, source untouched. */
    public static M3FrozenLongSet copyOf(long[] source) {
        Objects.requireNonNull(source, "source");
        if (source.length == 0) return EMPTY;
        long[] lane = source.clone();
        Arrays.sort(lane);
        int count = 1;
        for (int i = 1; i < lane.length; i++) {
            if (lane[i] != lane[count - 1]) lane[count++] = lane[i];
        }
        return new M3FrozenLongSet(count == lane.length ? lane : Arrays.copyOf(lane, count), count);
    }

    /** Set.size. */
    public int size() {
        return size;
    }

    /** Set.isEmpty. */
    public boolean isEmpty() {
        return size == 0;
    }

    /** Set.contains: binary search over the sorted lane. */
    public boolean contains(long value) {
        return Arrays.binarySearch(valueLane, 0, size, value) >= 0;
    }

    /** ImmutableSortedSet.asList().get: value at the ascending ordinal. */
    public long get(int ordinal) {
        return valueLane[Objects.checkIndex(ordinal, size)];
    }

    /** SortedSet.first. */
    public long first() {
        if (size == 0) throw new NoSuchElementException();
        return valueLane[0];
    }

    /** SortedSet.last. */
    public long last() {
        if (size == 0) throw new NoSuchElementException();
        return valueLane[size - 1];
    }

    /** ImmutableSortedSet.asList().indexOf: ascending ordinal of the value, or -1 when absent. */
    public int rank(long value) {
        int index = Arrays.binarySearch(valueLane, 0, size, value);
        return index >= 0 ? index : -1;
    }

    /** Set.toArray: ascending defensive copy. */
    public long[] toArray() {
        return Arrays.copyOf(valueLane, size);
    }

    /** Iterable.forEach in ascending order. */
    public void forEach(LongConsumer action) {
        Objects.requireNonNull(action, "action");
        for (int i = 0; i < size; i++) action.accept(valueLane[i]);
    }

    /** Set.iterator: ascending; {@code remove} is unsupported. */
    public PrimitiveIterator.OfLong iterator() {
        return new PrimitiveIterator.OfLong() {
            private int cursor;

            @Override public boolean hasNext() {
                return cursor < size;
            }

            @Override public long nextLong() {
                if (cursor >= size) throw new NoSuchElementException();
                return valueLane[cursor++];
            }
        };
    }

    /** Immutable: always throws {@link UnsupportedOperationException} (JEP 269 / ImmutableCollection contract). */
    public boolean add(long value) {
        throw new UnsupportedOperationException("M3FrozenLongSet is immutable");
    }

    /** Immutable: always throws {@link UnsupportedOperationException} (JEP 269 / ImmutableCollection contract). */
    public boolean remove(long value) {
        throw new UnsupportedOperationException("M3FrozenLongSet is immutable");
    }

    /** Equal to another frozen set with the same members. */
    @Override public boolean equals(Object other) {
        return other == this || other instanceof M3FrozenLongSet set
                && Arrays.equals(valueLane, 0, size, set.valueLane, 0, set.size);
    }

    /** java.util.Set hash contract: sum of {@code Long.hashCode} of the members. */
    @Override public int hashCode() {
        int hash = 0;
        for (int i = 0; i < size; i++) hash += Long.hashCode(valueLane[i]);
        return hash;
    }

    /** Ascending {@code [a, b, c]} rendering (as TreeSet / ImmutableSortedSet). */
    @Override public String toString() {
        if (size == 0) return "[]";
        StringBuilder text = new StringBuilder(size * 4 + 2).append('[').append(valueLane[0]);
        for (int i = 1; i < size; i++) text.append(", ").append(valueLane[i]);
        return text.append(']').toString();
    }

    int stateWord() {
        return (int) stateMaskWords[0];
    }
}
