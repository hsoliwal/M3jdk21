/*
 * Copyright 2026 Synexia <hsoliwal@gmail.com>
 * Licensed under the Apache License, Version 2.0
 */
package com.m3.collections;

import java.util.Objects;
import java.util.function.LongConsumer;

/**
 * Minimal primitive-long collection contract.
 *
 * <p>The contract deliberately avoids {@code Collection<Long>} so retained state and
 * iteration do not require one wrapper object per primitive value.
 */
public interface M3LongCollection {
    int size();

    boolean contains(long value);

    void clear();

    M3LongIterator iterator();

    long[] toArray();

    default boolean isEmpty() {
        return size() == 0;
    }

    default void forEachLong(LongConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        M3LongIterator iterator = iterator();
        while (iterator.hasNext()) {
            consumer.accept(iterator.nextLong());
        }
    }
    /**
     * Primitive traversal without boxing or an element snapshot. The fallback cannot split;
     * indexed owners override this to partition indices directly. Mutation rules are those
     * of iterator(); callbacks must not interfere with a non-concurrent collection.
     */
    default java.util.Spliterator.OfLong longSpliterator() {
        M3LongIterator cursor = iterator();
        return new java.util.Spliterator.OfLong() {
            @Override public boolean tryAdvance(LongConsumer action) {
                Objects.requireNonNull(action, "action");
                if (!cursor.hasNext()) return false;
                action.accept(cursor.nextLong());
                return true;
            }
            @Override public OfLong trySplit() { return null; }
            @Override public long estimateSize() { return Long.MAX_VALUE; }
            @Override public int characteristics() { return NONNULL; }
        };
    }

    /** Lazy primitive stream; no Long wrapper is created by the source. */
    default java.util.stream.LongStream longStream() {
        return java.util.stream.StreamSupport.longStream(longSpliterator(), false);
    }

    /** Parallel-capable stream; actual splitting depends on the owner's spliterator. */
    default java.util.stream.LongStream parallelLongStream() {
        return java.util.stream.StreamSupport.longStream(longSpliterator(), true);
    }
}
