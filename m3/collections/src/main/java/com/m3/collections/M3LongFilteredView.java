/*
 * Copyright 2026 Synexia <hsoliwal@gmail.com>
 * Licensed under the Apache License, Version 2.0
 */
package com.m3.collections;

import java.util.Arrays;
import java.util.Spliterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.LongConsumer;
import java.util.function.LongPredicate;

/**
 * Read-only live selection over any M3LongCollection. Stores one source and one predicate,
 * not a retained filtered copy. Iteration order is the source's order. Stable predicates
 * and no mutation during traversal are required; this is not a snapshot or a JDK view.
 * Nested filtering retains one backing owner, no intermediate payload/array,
 * and no per-element node. The explicit primitive Collection contract permits
 * composition with the existing concat/stream owners without boxing.
 */
public final class M3LongFilteredView implements M3LongCollection {
    private final M3LongCollection source;
    private final LongPredicate predicate;

    public M3LongFilteredView(M3LongCollection source, LongPredicate predicate) {
        M3LongCollection actual = Objects.requireNonNull(source, "source");
        LongPredicate selected = Objects.requireNonNull(predicate, "predicate");
        if (actual instanceof M3LongFilteredView previous) {
            // Flatten nested filters without retaining intermediate view owners.
            // Both predicates still run in encounter order and short-circuit.
            this.source = previous.source;
            this.predicate = previous.predicate.and(selected);
        } else {
            this.source = actual;
            this.predicate = selected;
        }
    }

    /** Fail closed for every mutation, including an empty/no-op clear. */
    @Override
    public void clear() {
        throw new UnsupportedOperationException("read-only filtered collection");
    }

    public boolean contains(long value) {
        return source.contains(value) && predicate.test(value);
    }

    public int size() {
        int count = 0;
        M3LongIterator iterator = iterator();
        while (iterator.hasNext()) {
            iterator.nextLong();
            count++;
        }
        return count;
    }

    public boolean isEmpty() {
        return !iterator().hasNext();
    }

    public void forEachLong(LongConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        iterator().forEachRemaining(consumer);
    }

    /** Avoids querying source.size() or allocating a growth buffer when no key passes. */
    @Override
    public long[] toArray() {
        long[] result = null;
        int count = 0;
        M3LongIterator iterator = iterator();
        while (iterator.hasNext()) {
            if (result == null) {
                result = new long[8];
            } else if (count == result.length) {
                int extra = result.length >>> 1;
                if (result.length > Integer.MAX_VALUE - extra) {
                    throw new OutOfMemoryError("filtered long view exceeds array limit");
                }
                result = Arrays.copyOf(result, Math.max(count + 1, result.length + extra));
            }
            result[count++] = iterator.nextLong();
        }
        if (result == null) {
            return new long[0];
        }
        return count == result.length ? result : Arrays.copyOf(result, count);
    }

    /**
     * Unknown-size, late-binding, unsplittable cursor. SIZED, SORTED and
     * DISTINCT cannot be promised for an arbitrary M3LongCollection source.
     * Parallel streams remain correct but traverse sequentially at the source.
     */
    @Override
    public Spliterator.OfLong longSpliterator() {
        return new Spliterator.OfLong() {
            private M3LongIterator cursor;

            @Override
            public boolean tryAdvance(LongConsumer action) {
                Objects.requireNonNull(action, "action");
                if (cursor == null) {
                    cursor = iterator();
                }
                if (!cursor.hasNext()) {
                    return false;
                }
                action.accept(cursor.nextLong());
                return true;
            }

            @Override public OfLong trySplit() { return null; }
            @Override public long estimateSize() { return Long.MAX_VALUE; }
            @Override public int characteristics() { return NONNULL; }
        };
    }

    /** One source iterator plus one filtering cursor per traversal, not per value. */
    public M3LongIterator iterator() {
        M3LongIterator input = source.iterator();
        return new M3LongIterator() {
            private long next;
            private boolean ready;
            private boolean ended;

            @Override
            public boolean hasNext() {
                if (ready) {
                    return true;
                }
                if (ended) {
                    return false;
                }
                while (input.hasNext()) {
                    long value = input.nextLong();
                    if (predicate.test(value)) {
                        next = value;
                        ready = true;
                        return true;
                    }
                }
                ended = true;
                return false;
            }

            @Override
            public long nextLong() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                ready = false;
                return next;
            }
        };
    }
}
