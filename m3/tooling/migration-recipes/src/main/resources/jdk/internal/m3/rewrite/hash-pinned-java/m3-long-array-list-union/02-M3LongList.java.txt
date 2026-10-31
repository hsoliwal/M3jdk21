/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.function.LongPredicate;

/**
 * Mutable indexed primitive-long sequence.
 *
 * <p>Union contract of the two Synexia lineages that reached {@code com.m3.collections}: the
 * JDK-shaped positional surface ({@code boolean add}, {@code add(int, long)}, {@code remove(long)},
 * fail-fast iteration) and the {@code PackedLongList}-derived surface ({@code addIf},
 * {@code filterInPlace}, {@code sort}, {@code binarySearch}, {@code deduplicateSorted}). The
 * {@code boolean} return of {@link #add(long)} and {@link #addAll(long[])} is the JDK shape; the
 * Synexia {@code LongSequence} names {@code insert} and {@code removeValue} remain as defaulted
 * aliases so neither lineage's callers change. One semantic owner: {@link M3LongArrayList}.</p>
 */
public interface M3LongList extends M3LongCollection {
    boolean add(long value);

    void add(int index, long value);

    boolean addIf(long value, LongPredicate predicate);

    boolean addAll(long[] values);

    long get(int index);

    long set(int index, long value);

    long removeAt(int index);

    boolean remove(long value);

    int indexOf(long value);

    int lastIndexOf(long value);

    int filterInPlace(LongPredicate predicate);

    void sort();

    int binarySearch(long value);

    int deduplicateSorted();

    int capacity();

    void ensureCapacity(int minimumCapacity);

    void trimToSize();

    /** Synexia {@code LongSequence} alias of {@link #add(int, long)}. */
    default void insert(int index, long value) {
        add(index, value);
    }

    /** Synexia {@code LongSequence} alias of {@link #remove(long)}. */
    default boolean removeValue(long value) {
        return remove(value);
    }
}
