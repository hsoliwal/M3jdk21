/*
 * Copyright 2026 Hitesh Soliwal and contributors
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
 * Modified 2026 by Hitesh Soliwal and contributors: drain failed bounded lanes until the coordinator observes their failure and cancels workers.
=======
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
>>>>>>> 2e36d73217cc08e4f5f9bdbe37fb5fea3472889f
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Explicit key-affinity execution plan for M3ObjectStream.
 *
 * <p>Every input key is computed once by the coordinator and routed to one logical
 * lane using its hash. A lane has one worker, so equal keys are processed in source
 * order (assuming the Java equals/hashCode contract). Work admission is globally
 * bounded by maxInFlight, even when a partition is slow. Results are merged by
 * source ordinal through a bounded primitive-mask ring, not by worker completion order.
 * The admission window also bounds retained out-of-order results. No unbounded task queue, hidden
 * common pool, retries, or implicit parallel reduction is used.</p>
 *
 * <p>Mapper code for different lanes may run concurrently. Shared state and external
 * side effects remain the caller's responsibility. A mapping failure cancels remaining
 * work and is rethrown; prior mapper side effects cannot be rolled back.</p>
 */
public final class M3KeyedParallelismPlan<T, K> {
    private static final int MAX_PARTITIONS = 256;
    private static final int MAX_IN_FLIGHT = 65_536;

    private final M3ObjectStream<T> source;
    private final Function<? super T, ? extends K> keyExtractor;

    M3KeyedParallelismPlan(M3ObjectStream<T> source,
                           Function<? super T, ? extends K> keyExtractor) {
        this.source = Objects.requireNonNull(source, "source");
        this.keyExtractor = Objects.requireNonNull(keyExtractor, "keyExtractor");
    }

    /**
     * Transform items concurrently within different key-affinity lanes and return a
     * read-only list merged in original source order. Both admission and result count
     * are bounded. Null inputs and null mapped values are allowed.
     */
    public <R> List<R> mapOrdered(Function<? super T, ? extends R> mapper,
                                  int partitionCount, int maxInFlight, int maxElements) {
        Objects.requireNonNull(mapper, "mapper");
        requireRange(partitionCount, 1, MAX_PARTITIONS, "partitionCount");
        requireRange(maxInFlight, 1, MAX_IN_FLIGHT, "maxInFlight");
        if (maxElements < 0) throw new IllegalArgumentException("maxElements must be nonnegative");

        AtomicInteger threadId = new AtomicInteger();
        ThreadFactory factory = task -> {
            Thread thread = new Thread(task, "m3-key-lane-" + threadId.getAndIncrement());
            thread.setDaemon(false);
            return thread;
        };
        ExecutorService workers = Executors.newFixedThreadPool(partitionCount, factory);
        // Reserve bounded queue storage across all lanes, not maxInFlight slots per lane.
        int perLaneCapacity = Math.max(1, (maxInFlight + partitionCount - 1) / partitionCount);
        List<BlockingQueue<Work<T>>> queues = new ArrayList<>(partitionCount);
        for (int lane = 0; lane < partitionCount; lane++) {
            queues.add(new ArrayBlockingQueue<>(perLaneCapacity));
        }
        BlockingQueue<Completion<R>> completed = new ArrayBlockingQueue<>(maxInFlight);
        AtomicReference<Throwable> laneFailure = new AtomicReference<>();
        for (int lane = 0; lane < partitionCount; lane++) {
            final BlockingQueue<Work<T>> queue = queues.get(lane);
            workers.execute(() -> runLane(queue, completed, mapper, laneFailure));
        }

        OrderedResults<R> results = new OrderedResults<>(maxInFlight);
        int inFlight = 0;
        long submitted = 0;
        try (Stream<T> input = source.stream()) {
            var iterator = input.iterator();
            while (iterator.hasNext()) {
                if (submitted >= maxElements) throw new IllegalStateException("parallel result budget exceeded");
                // Bound ordinal distance as well as active work. Otherwise a slow first
                // ordinal lets later completions accumulate without limit in the merger.
                while (inFlight >= maxInFlight
                        || submitted - results.nextOrdinal >= maxInFlight) {
                    results.accept(take(completed));
                    inFlight--;
                }
                T item = iterator.next();
                K key = keyExtractor.apply(item);
                int lane = laneFor(key, partitionCount);
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
                put(queues.get(lane), new Work<>(submitted, item, false), laneFailure);
=======
                inFlight -= admit(queues.get(lane), new Work<>(submitted, item, false),
                        completed, results);
>>>>>>> 2e36d73217cc08e4f5f9bdbe37fb5fea3472889f
                submitted++;
                inFlight++;
            }
            while (inFlight > 0) {
                results.accept(take(completed));
                inFlight--;
            }
            for (BlockingQueue<Work<T>> queue : queues) {
                put(queue, new Work<>(-1, null, true), laneFailure);
            }
            workers.shutdown();
            awaitWorkers(workers);
            return Collections.unmodifiableList(results.ordered);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            workers.shutdownNow();
            throw new IllegalStateException("interrupted while executing keyed M3 stream", interrupted);
        } catch (RuntimeException | Error failure) {
            workers.shutdownNow();
            throw failure;
        }
    }

    public int laneForKey(K key, int partitionCount) {
        requireRange(partitionCount, 1, MAX_PARTITIONS, "partitionCount");
        return laneFor(key, partitionCount);
    }

    private static int laneFor(Object key, int partitionCount) {
        int hash = key == null ? 0 : key.hashCode();
        hash ^= hash >>> 16;
        return Math.floorMod(hash, partitionCount);
    }

    private static <T, R> void runLane(BlockingQueue<Work<T>> queue,
                                       BlockingQueue<Completion<R>> completed,
                                       Function<? super T, ? extends R> mapper,
                                       AtomicReference<Throwable> laneFailure) {
        try {
            for (;;) {
                Work<T> work = queue.take();
                if (work.end()) return;
                try {
                    R value = mapper.apply(work.value());
                    completed.put(new Completion<>(work.ordinal(), value, null));
                } catch (RuntimeException | Error failure) {
                    laneFailure.compareAndSet(null, failure);
                    completed.put(new Completion<>(work.ordinal(), null, failure));
                    // The coordinator may be blocked admitting more work to this bounded lane.
                    // Keep freeing its slots until the posted failure reaches the coordinator.
                    // Global in-flight accounting makes that failure reachable before any
                    // discarded completion is awaited; no cancelled mapper is invoked.
                    while (!queue.take().end()) { }
                    return;
                }
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Admit one item without hiding a failed lane behind its full input queue.
     * A one-millisecond wait applies only while that queue is saturated. Polling
     * completions after the wait observes worker failure without requiring a
     * mapper to finish when a dequeue has already made lane capacity available.
     * The returned count releases only already-submitted global admission credits.
     */
    private static <T, R> int admit(BlockingQueue<Work<T>> queue, Work<T> work,
                                    BlockingQueue<Completion<R>> completed,
                                    OrderedResults<R> results) throws InterruptedException {
        int drained = 0;
        while (!queue.offer(work, 1, TimeUnit.MILLISECONDS)) {
            Completion<R> completion;
            while ((completion = completed.poll()) != null) {
                results.accept(completion);
                drained++;
            }
        }
        return drained;
    }

    private static <R> Completion<R> take(BlockingQueue<Completion<R>> queue) throws InterruptedException {
        return queue.take();
    }

    /**
     * Fast path is a nonblocking queue admission. When a lane is full, bounded timed
     * retries observe failed workers so a dead lane cannot strand the coordinator.
     */
    private static <E> void put(BlockingQueue<E> queue, E value,
                                AtomicReference<Throwable> laneFailure)
            throws InterruptedException {
        if (queue.offer(value)) return;
        do {
            checkFailure(laneFailure.get());
        } while (!queue.offer(value, 25, TimeUnit.MILLISECONDS));
        checkFailure(laneFailure.get());
    }

    private static void checkFailure(Throwable failure) {
        if (failure instanceof RuntimeException runtime) throw runtime;
        if (failure instanceof Error error) throw error;
        if (failure != null) throw new IllegalStateException(
                "keyed M3 stream worker failed", failure);
    }

    private static void awaitWorkers(ExecutorService workers) throws InterruptedException {
        while (!workers.isTerminated()) workers.awaitTermination(1, java.util.concurrent.TimeUnit.DAYS);
    }

    private static void requireRange(int value, int minimum, int maximum, String name) {
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException(name + " must be in [" + minimum + "," + maximum + "]");
        }
    }

    private record Work<T>(long ordinal, T value, boolean end) { }
    private record Completion<R>(long ordinal, R value, Throwable failure) { }

    /**
     * One bounded Object[] value lane and one primitive 64-bit occupancy mask.
     * Ordinals are masked into power-of-two slots; no TreeMap entries, sorting,
     * retained per-result nodes or modulo division are required.
     */
    private static final class OrderedResults<R> {
        private final Object[] pending;
        private final long[] occupied;
        private final int indexMask;
        private final ArrayList<R> ordered = new ArrayList<>();
        private long nextOrdinal;

        private OrderedResults(int maxInFlight) {
            int capacity = 1;
            while (capacity < maxInFlight) capacity <<= 1;
            this.pending = new Object[capacity];
            this.occupied = new long[(capacity + Long.SIZE - 1) / Long.SIZE];
            this.indexMask = capacity - 1;
        }

        @SuppressWarnings("unchecked")
        private void accept(Completion<R> result) {
            checkFailure(result.failure());
            long ordinal = result.ordinal();
            long distance = ordinal - nextOrdinal;
            if (distance < 0 || distance >= pending.length) {
                throw new IllegalStateException("completion outside bounded ordinal window");
            }
            int slot = (int) ordinal & indexMask;
            int word = slot >>> 6;
            long bit = 1L << slot;
            if ((occupied[word] & bit) != 0L) {
                throw new IllegalStateException("duplicate keyed stream completion");
            }
            pending[slot] = result.value();
            occupied[word] |= bit;

            for (;;) {
                int first = (int) nextOrdinal & indexMask;
                int firstWord = first >>> 6;
                long firstBit = 1L << first;
                if ((occupied[firstWord] & firstBit) == 0L) return;
                R value = (R) pending[first];
                pending[first] = null;
                occupied[firstWord] &= ~firstBit;
                ordered.add(value);
                nextOrdinal++;
            }
        }
    }
}
