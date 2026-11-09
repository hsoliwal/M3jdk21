/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import java.util.Arrays;
import java.util.function.LongPredicate;

/**
 * Primitive-long priority queue: repeated removal yields ascending priority order.
 *
 * <p>API_UNION U3 (2026-11-09): one owner for the concept. The Synexia contract (the common-collections
 * LongPriorityQueue) and the members of the former M3 class meet here; {@link #of()} returns the natural-order
 * array min-heap.</p>
 */
public interface LongPriorityQueue extends LongCollection {
    /** Returns an empty natural-order min-heap. */
    static LongPriorityQueue of() { return new ArrayLongPriorityQueue(); }

    /** Returns an empty natural-order min-heap with room for {@code initialCapacity} values. */
    static LongPriorityQueue of(int initialCapacity) { return new ArrayLongPriorityQueue(initialCapacity); }

    /** Returns a natural-order min-heap of a copy of {@code values}, built bottom-up in O(n). */
    static LongPriorityQueue of(long[] values) { return new ArrayLongPriorityQueue(values); }

    /** Inserts {@code value}; returns true (the receipt contract: Synexia returns void). */
    boolean offer(long value);

    /** Inserts {@code value} when {@code predicate} accepts it; returns whether it was inserted. */
    boolean offerIf(long value, LongPredicate predicate);

    /** Returns the head without removing it; throws NoSuchElementException when empty. */
    long peek();

    /** Removes and returns the head; throws NoSuchElementException when empty. */
    long poll();

    /** Replaces the head with {@code value} and returns the previous head; throws NoSuchElementException when empty. */
    long replaceTop(long value);

    /** Keeps the values {@code predicate} accepts; returns how many were removed. */
    int filterInPlace(LongPredicate predicate);

    /** Returns the current backing capacity. */
    int capacity();

    /** Inserts {@code value} (M3 name of {@link #offer(long)}). */
    default boolean add(long value) { return offer(value); }

    /** Returns the head (M3 name of {@link #peek()}). */
    default long firstLong() { return peek(); }

    /** Removes and returns the head (M3 name of {@link #poll()}). */
    default long removeFirstLong() { return poll(); }

    /** Removes one occurrence of {@code value}; returns whether one was present. */
    default boolean remove(long value) {
        boolean[] removed = new boolean[1];
        return filterInPlace(v -> removed[0] || v != value || !(removed[0] = true)) == 1;
    }

    /** Returns a sorted snapshot without mutating this queue. */
    default long[] toSortedArray() {
        long[] result = toArray();
        Arrays.sort(result);
        return result;
    }
}
