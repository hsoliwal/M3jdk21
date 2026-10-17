// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
package com.m3.collections.parallel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Bounded one-batch-at-a-time keyed execution. Batch completion is a barrier, so a key
 * cannot overlap itself across batches. At most batchSize source references are pulled
 * ahead of completed work; callback retention and upstream iterator buffering are excluded.
 * Single-use across all configuration views. The iterator and pool are borrowed, never closed.
 * No blocking/asynchronous callback support, retry, rollback, or cross-call lock is implied.
 */
public final class M3Batches<T, K> {
    private final Iterator<? extends T> source;
    private final int batchSize;
    private final Function<? super T, ? extends K> key;
    private final ForkJoinPool pool;
    private final BooleanSupplier cancelled;
    private final AtomicBoolean used;
    M3Batches(Iterator<? extends T> source, int batchSize, Function<? super T, ? extends K> key) {
        this(source, batchSize, key, ForkJoinPool.commonPool(), () -> false, new AtomicBoolean());
    }
    private M3Batches(Iterator<? extends T> source, int batchSize, Function<? super T, ? extends K> key,
            ForkJoinPool pool, BooleanSupplier cancelled, AtomicBoolean used) {
        if (batchSize <= 0) throw new IllegalArgumentException("nonpositive batch size");
        this.source = Objects.requireNonNull(source); this.batchSize = batchSize;
        this.key = Objects.requireNonNull(key); this.pool = Objects.requireNonNull(pool);
        this.cancelled = Objects.requireNonNull(cancelled); this.used = used;
    }
    public M3Batches<T, K> using(ForkJoinPool executor) {
        return new M3Batches<>(source, batchSize, key, executor, cancelled, used);
    }
    public M3Batches<T, K> cancelWhen(BooleanSupplier predicate) {
        return new M3Batches<>(source, batchSize, key, pool, predicate, used);
    }
    private void checkCancelled() {
        if (cancelled.getAsBoolean()) throw new CancellationException("batch operation cancelled");
    }
    /** Returns the number of successfully completed elements, or throws after draining callbacks. */
    public long forEach(Consumer<? super T> action) {
        Objects.requireNonNull(action);
        if (!used.compareAndSet(false, true)) throw new IllegalStateException("batch source already consumed");
        long completed = 0;
        ArrayList<T> batch = new ArrayList<>(Math.min(batchSize, 1024));
        for (;;) {
            checkCancelled(); batch.clear();
            while (batch.size() < batchSize) {
                checkCancelled();
                if (!source.hasNext()) break;
                checkCancelled(); batch.add(source.next());
            }
            checkCancelled(); if (batch.isEmpty()) return completed;
            M3Parallel.by(batch, key).using(pool).cancelWhen(cancelled).forEach(action);
            completed = Math.addExact(completed, batch.size());
        }
    }
}
