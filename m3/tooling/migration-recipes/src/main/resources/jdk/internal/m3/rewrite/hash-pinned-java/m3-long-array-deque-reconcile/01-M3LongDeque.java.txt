/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.function.LongPredicate;

/**
 * Primitive-long double-ended queue contract.
 *
 * <p>Union of the JDK-shaped backend surface ({@code offerFirst/offerLast}, {@code getFirst/getLast},
 * {@code removeFirstLong/removeLastLong}, occurrence removal, descending iteration, fail-fast
 * iterators) and the Synexia {@code LongDeque} lineage ({@code addLastIf}, indexed {@code get},
 * {@code filterInPlace}, queue {@code offer/peek/poll}). The Synexia names {@code first},
 * {@code last}, {@code removeFirst} and {@code removeLast} remain as defaulted aliases so neither
 * lineage's callers change. One semantic owner: {@link M3LongArrayDeque}.</p>
 */
public interface M3LongDeque extends M3LongQueue {
    void addFirst(long value);

    void addLast(long value);

    boolean offerFirst(long value);

    boolean offerLast(long value);

    boolean addLastIf(long value, LongPredicate predicate);

    long getFirst();

    long getLast();

    long removeFirstLong();

    long removeLastLong();

    boolean removeFirstOccurrence(long value);

    boolean removeLastOccurrence(long value);

    long get(int index);

    int filterInPlace(LongPredicate predicate);

    M3LongIterator descendingIterator();

    /** Synexia {@code LongDeque} alias of {@link #getFirst()}. */
    default long first() {
        return getFirst();
    }

    /** Synexia {@code LongDeque} alias of {@link #getLast()}. */
    default long last() {
        return getLast();
    }

    /** Synexia {@code LongDeque} alias of {@link #removeFirstLong()}. */
    default long removeFirst() {
        return removeFirstLong();
    }

    /** Synexia {@code LongDeque} alias of {@link #removeLastLong()}. */
    default long removeLast() {
        return removeLastLong();
    }
}
