/*
 * Copyright 2026 Synexia <hsoliwal@gmail.com>
 * Licensed under the Apache License, Version 2.0
 */
package com.m3.collections;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.LongConsumer;
import java.util.function.LongPredicate;

/**
 * Read-only live selection over any M3LongCollection. Stores one source and one predicate,
 * not a retained filtered copy. Iteration order is the source's order. Stable predicates
 * and no mutation during traversal are required; this is not a snapshot or a JDK view.
 */
public final class M3LongFilteredView {
    private final M3LongCollection source;
    private final LongPredicate predicate;

    public M3LongFilteredView(M3LongCollection source, LongPredicate predicate) {
        this.source = Objects.requireNonNull(source, "source");
        this.predicate = Objects.requireNonNull(predicate, "predicate");
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

    /** Allocates only a primitive result/growth buffer, never a boxed value. */
    public long[] toArray() {
        long[] result = new long[Math.min(8, source.size())];
        int count = 0;
        M3LongIterator iterator = iterator();
        while (iterator.hasNext()) {
            if (count == result.length) {
                result = Arrays.copyOf(result, Math.max(count + 1, Math.max(8, result.length + (result.length >>> 1))));
            }
            result[count++] = iterator.nextLong();
        }
        // The buffer belongs to this invocation; an exact fit is already a detached snapshot.
        return count == result.length ? result : Arrays.copyOf(result, count);
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
