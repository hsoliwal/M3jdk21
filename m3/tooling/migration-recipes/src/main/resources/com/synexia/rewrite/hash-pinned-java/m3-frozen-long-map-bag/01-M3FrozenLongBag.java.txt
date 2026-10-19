/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.LongConsumer;

/**
 * Immutable multiset (bag) of {@code long} values: sorted distinct element lane plus parallel occurrence-count lane.
 *
 * <p>Provider lineage (LIBRARY_API_MATRIX.tsv, run 20261017-synexia-to-public/LIB-SUPERSET, leaf LS2):
 * <ul>
 *   <li>GUAVA {@code ImmutableMultiset}: {@code size()} = total occurrences, {@code count} = occurrences (0 when
 *       absent), {@code elementSet().size()} = distinct count, mutators throw {@link UnsupportedOperationException},
 *       {@code hashCode} = sum of {@code Long.hashCode(element) ^ count}. GUAVA {@code ImmutableSortedMultiset}: ascending
 *       distinct order and {@code [e x n, ...]} rendering. Insertion order of {@code ImmutableMultiset} is NOT claimed.</li>
 * </ul>
 * Each provider contract is qualified only against that provider's own oracle; no equivalence is inferred.
 *
 * <p>DOOP mapping: lane kind LONG; {@code elementLane} (sorted distinct) and {@code countLane} (occurrences at the
 * same ordinal) plus the shared FROZEN owner state word; offsets are sorted ordinals located by binary search. Stored
 * nodes: NONE; no {@code Multiset.Entry} is stored or created. Per-element allocation: 0 (construction allocates
 * the lanes; queries, ordinal access and {@code forEachDistinct} allocate nothing). Donor: first-party Synexia
 * {@code PackedLongBag} / {@code PackedLongSortedBag} counted-lane pattern, adapted. Not a {@code java.util}
 * replacement.
 */
public final class M3FrozenLongBag {
    /** Owner state bit {@code FROZEN}: every slot of both lanes is immutable. */
    static final long STATE_FROZEN = 1L << 0;
    private static final long[] EMPTY_ELEMENTS = new long[0];
    private static final int[] EMPTY_COUNTS = new int[0];
    private static final long[] FROZEN_STATE = {STATE_FROZEN};
    private static final M3FrozenLongBag EMPTY = new M3FrozenLongBag();

    /** Storage lane {@code elementLane}: sorted distinct elements. */
    private final long[] elementLane;
    /** Storage lane {@code countLane}: occurrences of the element at the same ordinal (always >= 1). */
    private final int[] countLane;
    /** Owner state word (FROZEN); shared, never mutated. */
    private final long[] stateMaskWords;
    /** Live slot (distinct element) count. */
    private final int size;
    /** Total occurrences across all slots. */
    private final int total;

    /** Creates an empty frozen bag. */
    public M3FrozenLongBag() {
        this(EMPTY_ELEMENTS, EMPTY_COUNTS, 0, 0);
    }

    private M3FrozenLongBag(long[] elementLane, int[] countLane, int size, int total) {
        this.elementLane = elementLane;
        this.countLane = countLane;
        this.stateMaskWords = FROZEN_STATE;
        this.size = size;
        this.total = total;
    }

    /** Reports the declared state-mask bits and lanes for ledgers; never allocates per element. */
    public static String scaffoldDescriptor() {
        return "M3FrozenLongBag|FROZEN_LONG_BAG|LONG|FROZEN|long[] elementLane;int[] countLane|RECEIVED";
    }

    /** Guava ImmutableMultiset.copyOf: every occurrence counted, source untouched. */
    public static M3FrozenLongBag copyOf(long[] source) {
        Objects.requireNonNull(source, "source");
        if (source.length == 0) return EMPTY;
        long[] sorted = source.clone();
        Arrays.sort(sorted);
        int[] counts = new int[sorted.length];
        int distinct = 0;
        for (int i = 0; i < sorted.length; i++) {
            if (distinct > 0 && sorted[distinct - 1] == sorted[i]) {
                counts[distinct - 1]++;
            } else {
                sorted[distinct] = sorted[i];
                counts[distinct++] = 1;
            }
        }
        long[] elements = distinct == sorted.length ? sorted : Arrays.copyOf(sorted, distinct);
        int[] lane = distinct == counts.length ? counts : Arrays.copyOf(counts, distinct);
        return new M3FrozenLongBag(elements, lane, distinct, source.length);
    }

    /** Multiset.size: total occurrences. */
    public int size() {
        return total;
    }

    /** Multiset.elementSet().size: distinct elements. */
    public int distinctSize() {
        return size;
    }

    /** Multiset.count: occurrences, 0 when absent. */
    public int occurrencesOf(long element) {
        int index = Arrays.binarySearch(elementLane, 0, size, element);
        return index >= 0 ? countLane[index] : 0;
    }

    /** Multiset.contains. */
    public boolean contains(long element) {
        return Arrays.binarySearch(elementLane, 0, size, element) >= 0;
    }

    /** ImmutableSortedMultiset.elementSet().asList().get. */
    public long elementAt(int ordinal) {
        return elementLane[Objects.checkIndex(ordinal, size)];
    }

    /** Occurrences of the element at the ascending distinct ordinal. */
    public int countAt(int ordinal) {
        return countLane[Objects.checkIndex(ordinal, size)];
    }

    /** Multiset.elementSet().toArray in ascending order (defensive copy). */
    public long[] distinctToArray() {
        return Arrays.copyOf(elementLane, size);
    }

    /** Distinct elements in ascending order. */
    public void forEachDistinct(LongConsumer action) {
        Objects.requireNonNull(action, "action");
        for (int i = 0; i < size; i++) action.accept(elementLane[i]);
    }

    /** Immutable: always throws {@link UnsupportedOperationException}. */
    public int addOccurrences(long element, int occurrences) {
        throw new UnsupportedOperationException("M3FrozenLongBag is immutable");
    }

    /** Immutable: always throws {@link UnsupportedOperationException}. */
    public boolean removeOccurrences(long element, int occurrences) {
        throw new UnsupportedOperationException("M3FrozenLongBag is immutable");
    }

    /** Equal to another frozen bag with the same element counts. */
    @Override public boolean equals(Object other) {
        return other == this || other instanceof M3FrozenLongBag bag
                && Arrays.equals(elementLane, 0, size, bag.elementLane, 0, bag.size)
                && Arrays.equals(countLane, 0, size, bag.countLane, 0, bag.size);
    }

    /** Guava Multiset hash contract: sum of {@code Long.hashCode(element) ^ count}. */
    @Override public int hashCode() {
        int hash = 0;
        for (int i = 0; i < size; i++) hash += Long.hashCode(elementLane[i]) ^ countLane[i];
        return hash;
    }

    /** Ascending {@code [e x n, f]} rendering (as ImmutableSortedMultiset). */
    @Override public String toString() {
        StringBuilder text = new StringBuilder(size * 6 + 2).append('[');
        for (int i = 0; i < size; i++) {
            if (i > 0) text.append(", ");
            text.append(elementLane[i]);
            if (countLane[i] != 1) text.append(" x ").append(countLane[i]);
        }
        return text.append(']').toString();
    }

    int stateWord() {
        return (int) stateMaskWords[0];
    }
}
