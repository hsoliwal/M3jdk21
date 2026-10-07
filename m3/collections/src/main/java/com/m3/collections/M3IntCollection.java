/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.Objects;
import java.util.Spliterator;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

/**
 * Minimal primitive-int collection contract, the int lane of {@link M3LongCollection}.
 *
 * <p>The contract deliberately avoids {@code Collection<Integer>} so retained state and
 * iteration do not require one wrapper object per primitive value.</p>
 */
public interface M3IntCollection {
    int size();

    boolean contains(int value);

    void clear();

    M3IntIterator iterator();

    int[] toArray();

    default boolean isEmpty() {
        return size() == 0;
    }

    default void forEachInt(IntConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        M3IntIterator iterator = iterator();
        while (iterator.hasNext()) {
            consumer.accept(iterator.nextInt());
        }
    }

    /**
     * Primitive traversal without boxing or an element snapshot. The fallback cannot split;
     * indexed owners override this to partition indices directly. Mutation rules are those of
     * iterator(); callbacks must not interfere with a non-concurrent collection.
     */
    default Spliterator.OfInt intSpliterator() {
        M3IntIterator cursor = iterator();
        return new Spliterator.OfInt() {
            @Override
            public boolean tryAdvance(IntConsumer action) {
                Objects.requireNonNull(action, "action");
                if (!cursor.hasNext()) {
                    return false;
                }
                action.accept(cursor.nextInt());
                return true;
            }

            @Override
            public OfInt trySplit() {
                return null;
            }

            @Override
            public long estimateSize() {
                return Long.MAX_VALUE;
            }

            @Override
            public int characteristics() {
                return NONNULL;
            }
        };
    }

    /** Lazy primitive stream; no Integer wrapper is created by the source. */
    default IntStream intStream() {
        return StreamSupport.intStream(intSpliterator(), false);
    }

    /** Parallel-capable stream; actual splitting depends on the owner's spliterator. */
    default IntStream parallelIntStream() {
        return StreamSupport.intStream(intSpliterator(), true);
    }
}
