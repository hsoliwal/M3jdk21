/* *****************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
/*
 * Modified 2026 by Hitesh Soliwal and contributors: drain failed bounded lanes until the coordinator observes their failure and cancels workers.
 */
package jdk.m3.collections;

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
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.LongConsumer;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import java.util.function.ToLongFunction;
import java.util.stream.Collector;
import jdk.internal.m3.collections.FrozenMap;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;

/**
 * Explicit key-affinity execution plan for ObjectStream.
 *
 * <p>Every input key is computed once by the coordinator and routed to one logical
 * lane using its hash or an explicitly configured stable integer signal. A lane
 * has one worker, so equal keys are processed in source order when equal keys
 * have equal hash codes or configured signals. Work admission is globally
 * bounded by maxInFlight, even when a partition is slow. Ordered mapping results are merged by
 * source ordinal through a bounded primitive-mask ring, not by worker completion order.
 * The admission window also bounds retained out-of-order results. No unbounded task queue, hidden
 * common pool, retries, or implicit parallel reduction is used.</p>
 *
 * <p>Mapper code for different lanes may run concurrently. Shared state and external
 * side effects remain the caller's responsibility. A mapping failure cancels remaining
 * work and is rethrown; prior mapper side effects cannot be rolled back.</p>
 *
 * <p>Grouped terminals retain one caller state per equal key and finish on the
 * invoking thread after successful source close and worker termination. Execution
 * bounds are 1..256 partitions, 1..65,536 in-flight items, a nonnegative data
 * element limit, and (for grouped terminals) 0..1,048,576 distinct keys. These
 * limits do not bound the contents allocated by caller callbacks. Opt-in marker
 * frames are classified by the coordinator and never passed to key extractors
 * or data callbacks; their separate frame limit also bounds replay seek.</p>
 */
public final class KeyedParallelismPlan<T, K> {
    private static final int MAX_PARTITIONS = 256;
    private static final int MAX_IN_FLIGHT = 65_536;
    private static final int MAX_KEYS = 1_048_576;

    private final ObjectStream<T> source;
    private final Function<? super T, ? extends K> keyExtractor;
    // Null retains the original smeared-hash policy without invoking client code.
    private final ToIntFunction<? super K> partitionSignal;

    private final ToLongFunction<? super T> markerClassifier;
    private final LongConsumer markerAcknowledgement;
    private final int maxFrames;
    private final long replayAfter;

    KeyedParallelismPlan(ObjectStream<T> source,
            Function<? super T, ? extends K> keyExtractor) {
        this(source, keyExtractor, null, null, null, 0, -1);
    }

    private KeyedParallelismPlan(ObjectStream<T> source,
            Function<? super T, ? extends K> keyExtractor,
            ToIntFunction<? super K> partitionSignal,
            ToLongFunction<? super T> markerClassifier,
            LongConsumer markerAcknowledgement, int maxFrames, long replayAfter) {
        this.source = Objects.requireNonNull(source, "source");
        this.keyExtractor = Objects.requireNonNull(keyExtractor, "keyExtractor");
        this.partitionSignal = partitionSignal;
        this.markerClassifier = markerClassifier;
        this.markerAcknowledgement = markerAcknowledgement;
        this.maxFrames = maxFrames;
        this.replayAfter = replayAfter;
    }

    /**
     * Return an independent plan using a stable integer signal from each extracted
     * key. The signal is normalized with {@link Math#floorMod(int, int)} for the
     * requested partition count; it is not mixed or treated as an absolute index.
     *
     * <p>The signal is called by the coordinator once per input key during a
     * successful traversal, including null keys. Equal keys must produce the same
     * signal throughout each traversal. A thrown exception or error stops that
     * traversal and is propagated under the existing source-close and worker
     * cancellation rules. Explicit {@link #laneForKey(Object, int)} calls evaluate
     * the signal independently.</p>
     *
     * <p>This method does not traverse the source or change this plan. Source,
     * callback and replay ownership remain unchanged.</p>
     *
     * @param signal the non-null partition signal for extracted keys
     * @return a new plan with the requested routing policy
     * @throws NullPointerException if signal is null
     */
    public KeyedParallelismPlan<T, K> partitionSignal(ToIntFunction<? super K> signal) {
        return new KeyedParallelismPlan<>(source, keyExtractor,
                Objects.requireNonNull(signal, "signal"), markerClassifier,
                markerAcknowledgement, maxFrames, replayAfter);
    }

    /**
     * Return an independent plan with explicit marker barriers. The classifier is
     * called once per pulled frame on the invoking thread: -1 means data, and a
     * nonnegative result is a marker ID. Marker IDs must increase strictly; any
     * other negative result is invalid. Marker frames never reach the key
     * extractor or worker callbacks.
     *
     * <p>All earlier admitted data complete before acknowledgement runs on the
     * invoking thread. No later source frame is requested until it returns.
     * {@code maxFrames} bounds all frames, including markers and replay seek;
     * terminal {@code maxElements} separately bounds admitted data. A source
     * iterator may perform its own prefetch in {@code hasNext()}.</p>
     *
     * <p>This configuration retains no data and provides no durable or exactly-once
     * guarantee. Source replayability and callback side effects remain the caller's
     * responsibility. The callbacks and this plan's routing policy are retained
     * by reference.</p>
     */
    public KeyedParallelismPlan<T, K> markers(ToLongFunction<? super T> classifier,
            LongConsumer acknowledgement, int maxFrames) {
        Objects.requireNonNull(classifier, "classifier");
        Objects.requireNonNull(acknowledgement, "acknowledgement");
        if (maxFrames < 0) throw new IllegalArgumentException("maxFrames must be nonnegative");
        return new KeyedParallelismPlan<>(source, keyExtractor, partitionSignal,
                classifier, acknowledgement, maxFrames, replayAfter);
    }

    /**
     * Seek past this exact marker on each new terminal traversal before any key
     * extraction or worker callback. Skipped markers, including the matching one,
     * are not acknowledged. A missing marker throws IllegalArgumentException.
     * Markers must already be configured and the source must permit another
     * traversal if the plan has previously been consumed.
     */
    public KeyedParallelismPlan<T, K> reprocessAfter(long markerId) {
        if (markerId < 0) throw new IllegalArgumentException("negative replay marker");
        if (markerClassifier == null) throw new IllegalStateException("markers are not configured");
        return new KeyedParallelismPlan<>(source, keyExtractor, partitionSignal,
                markerClassifier, markerAcknowledgement, maxFrames, markerId);
    }

    /**
     * Transform items concurrently within different key-affinity lanes and return a
     * read-only list merged in original source order. Both admission and result count
     * are bounded. Null inputs and null mapped values are allowed.
     */
    public <R> List<R> mapOrdered(Function<? super T, ? extends R> mapper,
            int partitionCount, int maxInFlight, int maxElements) {
        Objects.requireNonNull(mapper, "mapper");
        requireExecutionBounds(partitionCount, maxInFlight, maxElements);
        OrderedResults<R> results = new OrderedResults<>(maxInFlight);
        execute(work -> mapper.apply(work.value()), null, results::accept,
                () -> results.nextOrdinal, partitionCount, maxInFlight, maxElements,
                markerClassifier != null);
        return Collections.unmodifiableList(results.ordered);
    }

    /**
     * Invoke an action in source order within each key-affinity lane. Different
     * lanes may run concurrently. Successful return means that all accepted
     * actions, marker acknowledgements, source close and worker termination have
     * completed. Cancellation interrupts and joins workers; callbacks must
     * cooperate with interruption. Earlier side effects cannot be rolled back.
     */
    public Receipt forEachKeyOrdered(Consumer<? super T> action,
            int partitionCount, int maxInFlight, int maxElements) {
        Objects.requireNonNull(action, "action");
        requireExecutionBounds(partitionCount, maxInFlight, maxElements);
        return execute(work -> { action.accept(work.value()); return null; }, null,
                result -> checkFailure(result.failure()), null,
                partitionCount, maxInFlight, maxElements, true);
    }

    /** Successful data and marker deliveries in one terminal invocation. */
    public record Receipt(long delivered, long markersAcknowledged, long lastMarkerId) { }

    /**
     * Create one non-null mutable state per equal key on that key's worker, then
     * accumulate its values in strict encounter order. The returned map is
     * immutable and iterates keys in first-encounter order; the client state
     * objects remain mutable. Null input values and null keys are permitted.
     *
     * <p>Bounds cover admitted data, distinct keys and pending work, not the
     * contents allocated by client callbacks. Equal keys must retain stable
     * equality, hashes and placement signals. Cancellation joins cooperative
     * workers before returning or throwing.</p>
     */
    public <A> Map<K, A> collectByKey(Supplier<? extends A> seed,
            BiConsumer<? super A, ? super T> accumulator,
            int partitionCount, int maxInFlight, int maxElements, int maxKeys) {
        return reduceByKey(seed, accumulator, Function.identity(),
                partitionCount, maxInFlight, maxElements, maxKeys);
    }

    /**
     * Strict, non-associative per-key accumulation, followed by one finisher call
     * per key on the invoking thread in first-key order. The finisher runs only
     * after all workers have terminated and the source has closed successfully.
     * Seed results must be non-null; finisher results may be null. The returned
     * map is immutable. No hot-key splitting or combiner is used.
     */
    public <A, R> Map<K, R> reduceByKey(Supplier<? extends A> seed,
            BiConsumer<? super A, ? super T> accumulator,
            Function<? super A, ? extends R> finisher,
            int partitionCount, int maxInFlight, int maxElements, int maxKeys) {
        Objects.requireNonNull(seed, "seed");
        Objects.requireNonNull(accumulator, "accumulator");
        Objects.requireNonNull(finisher, "finisher");
        requireGroupBounds(partitionCount, maxInFlight, maxElements, maxKeys);
        KeyStates<K, A> states = new KeyStates<>(maxKeys);
        execute(work -> {
            A state = states.initialize(work.group(), seed, false);
            accumulator.accept(state, work.value());
            return null;
        }, states, result -> checkFailure(result.failure()), null,
                partitionCount, maxInFlight, maxElements, true);
        return states.finish(finisher);
    }

    /**
     * Adapt a collector to strict lane-owned accumulation. Its combiner and
     * concurrency characteristics are not used, because a key is never split
     * between workers. The ordinary supplier/accumulator/finisher contracts and
     * this plan's explicit bounds still apply.
     */
    public <A, R> Map<K, R> collectByKey(Collector<? super T, A, R> collector,
            int partitionCount, int maxInFlight, int maxElements, int maxKeys) {
        Objects.requireNonNull(collector, "collector");
        requireGroupBounds(partitionCount, maxInFlight, maxElements, maxKeys);
        return reduceByKey(collector.supplier(), collector.accumulator(), collector.finisher(),
                partitionCount, maxInFlight, maxElements, maxKeys);
    }

    /**
     * Fold each key's values in strict encounter order on its worker. Both the
     * initial and replacement state may be null. The seed is called exactly once
     * per encountered key, including a key whose state stays null. The immutable
     * result preserves first-key order. This operation makes no associativity or
     * purity assumption about the folder.
     */
    public <A> Map<K, A> foldByKey(Supplier<? extends A> seed,
            BiFunction<? super A, ? super T, ? extends A> folder,
            int partitionCount, int maxInFlight, int maxElements, int maxKeys) {
        Objects.requireNonNull(seed, "seed");
        Objects.requireNonNull(folder, "folder");
        requireGroupBounds(partitionCount, maxInFlight, maxElements, maxKeys);
        KeyStates<K, A> states = new KeyStates<>(maxKeys);
        execute(work -> {
            A state = states.initialize(work.group(), seed, true);
            states.replace(work.group(), folder.apply(state, work.value()));
            return null;
        }, states, result -> checkFailure(result.failure()), null,
                partitionCount, maxInFlight, maxElements, true);
        return states.finish(Function.identity());
    }

    // Shared coordinator for the incumbent ordered map and additive terminals.
    // The optional ordinal supplier retains mapOrdered's original reorder bound.
    private <R> Receipt execute(Function<? super Work<T>, ? extends R> mapper,
            KeyStates<K, ?> groups, Consumer<Completion<R>> sink, LongSupplier nextOrdinal,
            int partitionCount, int maxInFlight, int maxElements, boolean joinOnFailure) {
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

        int inFlight = 0;
        long submitted = 0;
        long frames = 0;
        long acknowledged = 0;
        long lastAcknowledged = -1;
        long lastObservedMarker = -1;
        boolean seeking = replayAfter >= 0;
        try {
            for (int lane = 0; lane < partitionCount; lane++) {
                final BlockingQueue<Work<T>> queue = queues.get(lane);
                workers.execute(() -> runWorkLane(queue, completed, mapper,
                        laneFailure, joinOnFailure));
            }
            try (Stream<T> input = source.stream()) {
                var iterator = input.iterator();
                while (iterator.hasNext()) {
                    T item;
                    if (markerClassifier == null) {
                        // Preserve the incumbent overflow and admission callback order.
                        if (submitted >= maxElements) {
                            throw new IllegalStateException("parallel result budget exceeded");
                        }
                        while (inFlight >= maxInFlight || nextOrdinal != null
                                && submitted - nextOrdinal.getAsLong() >= maxInFlight) {
                            sink.accept(take(completed));
                            inFlight--;
                        }
                        item = iterator.next();
                    } else {
                        if (frames >= maxFrames) {
                            throw new IllegalStateException("parallel frame budget exceeded");
                        }
                        item = iterator.next();
                        frames++;
                        long marker = markerClassifier.applyAsLong(item);
                        if (marker < -1)
                            throw new IllegalArgumentException("invalid marker id: " + marker);
                        if (marker >= 0) {
                            if (marker <= lastObservedMarker) {
                                throw new IllegalArgumentException(
                                        "marker ids must increase strictly");
                            }
                            lastObservedMarker = marker;
                            if (seeking) {
                                if (marker == replayAfter) seeking = false;
                                continue;
                            }
                            while (inFlight > 0) {
                                sink.accept(take(completed));
                                inFlight--;
                            }
                            markerAcknowledgement.accept(marker);
                            acknowledged++;
                            lastAcknowledged = marker;
                            continue;
                        }
                        if (seeking) continue;
                        if (submitted >= maxElements) {
                            throw new IllegalStateException("parallel result budget exceeded");
                        }
                        while (inFlight >= maxInFlight || nextOrdinal != null
                                && submitted - nextOrdinal.getAsLong() >= maxInFlight) {
                            sink.accept(take(completed));
                            inFlight--;
                        }
                    }
                    K key = keyExtractor.apply(item);
                    int lane = laneFor(key, partitionCount);
                    int group = groups == null ? -1 : groups.register(key, lane);
                    put(queues.get(lane), new Work<>(submitted, item, false, group), laneFailure);
                    submitted++;
                    inFlight++;
                }
                if (seeking)
                    throw new IllegalArgumentException("replay marker not present: " + replayAfter);
                while (inFlight > 0) {
                    sink.accept(take(completed));
                    inFlight--;
                }
                for (BlockingQueue<Work<T>> queue : queues) {
                    put(queue, new Work<>(-1, null, true), laneFailure);
                }
                workers.shutdown();
                awaitWorkers(workers);
            }
            return new Receipt(submitted, acknowledged, lastAcknowledged);
        } catch (InterruptedException interrupted) {
            workers.shutdownNow();
            if (joinOnFailure) joinCancelledWorkers(workers);
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted while executing keyed M3 stream", interrupted);
        } catch (RuntimeException | Error failure) {
            workers.shutdownNow();
            if (joinOnFailure) joinCancelledWorkers(workers);
            throw failure;
        }
    }

    public int laneForKey(K key, int partitionCount) {
        requireRange(partitionCount, 1, MAX_PARTITIONS, "partitionCount");
        return laneFor(key, partitionCount);
    }

    private int laneFor(K key, int partitionCount) {
        if (partitionSignal != null) {
            return Math.floorMod(partitionSignal.applyAsInt(key), partitionCount);
        }
        int hash = key == null ? 0 : key.hashCode();
        hash ^= hash >>> 16;
        return Math.floorMod(hash, partitionCount);
    }

    private static <T, R> void runLane(BlockingQueue<Work<T>> queue,
                                       BlockingQueue<Completion<R>> completed,
                                       Function<? super T, ? extends R> mapper,
                                       AtomicReference<Throwable> laneFailure) {
        runWorkLane(queue, completed, work -> mapper.apply(work.value()), laneFailure, false);
    }

    private static <T, R> void runWorkLane(BlockingQueue<Work<T>> queue,
            BlockingQueue<Completion<R>> completed,
            Function<? super Work<T>, ? extends R> mapper,
            AtomicReference<Throwable> laneFailure, boolean reportInterruptedWork) {
        Work<T> current = null;
        try {
            for (;;) {
                Work<T> work = queue.take();
                current = work;
                if (work.end()) return;
                try {
                    R value = mapper.apply(work);
                    completed.put(new Completion<>(work.ordinal(), value, null));
                    current = null;
                } catch (RuntimeException | Error failure) {
                    laneFailure.compareAndSet(null, failure);
                    completed.put(new Completion<>(work.ordinal(), null, failure));
                    current = null;
                    // The coordinator may be blocked admitting more work to this bounded lane.
                    // Keep freeing its slots until the posted failure reaches the coordinator.
                    // Global in-flight accounting makes that failure reachable before any
                    // discarded completion is awaited; no cancelled mapper is invoked.
                    while (!queue.take().end()) { }
                    return;
                }
            }
        } catch (InterruptedException interrupted) {
            if (reportInterruptedWork && current != null && !current.end()) {
                laneFailure.compareAndSet(null, new IllegalStateException(
                        "keyed M3 stream worker interrupted", interrupted));
                // An unfinished admitted work item reserves one completion slot:
                // at most maxInFlight-1 other completions can occupy the queue.
                // A callback that sets its own interrupt flag must not leave the
                // coordinator waiting forever for this missing completion.
                completed.offer(new Completion<>(current.ordinal(), null, laneFailure.get()));
            }
            Thread.currentThread().interrupt();
        }
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
    //FLLDCIM: MRL - mergeloss2 leaf 17: fast-admission failure check restored from origin 3538a25a194 (PR #1522), dropped by retain merge 6d0e9f5461e6; recipe qualification/mergeloss2-filtered-views-20261107
        if (queue.offer(value)) {
            checkFailure(laneFailure.get());
            return;
        }
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

    // New terminals join cancellation; the incumbent unmarked map retains its
    // original cancellation timing. Client callbacks must honor interruption.
    private static void joinCancelledWorkers(ExecutorService workers) {
        boolean interrupted = Thread.interrupted();
        try {
            while (!workers.isTerminated()) {
                try {
                    workers.awaitTermination(1, TimeUnit.DAYS);
                } catch (InterruptedException cancellation) {
                    interrupted = true;
                    workers.shutdownNow();
                }
            }
        } finally {
            if (interrupted) Thread.currentThread().interrupt();
        }
    }

    private static void requireExecutionBounds(
            int partitionCount, int maxInFlight, int maxElements) {
        requireRange(partitionCount, 1, MAX_PARTITIONS, "partitionCount");
        requireRange(maxInFlight, 1, MAX_IN_FLIGHT, "maxInFlight");
        if (maxElements < 0) throw new IllegalArgumentException("maxElements must be nonnegative");
    }

    private static void requireGroupBounds(int partitionCount, int maxInFlight,
            int maxElements, int maxKeys) {
        requireExecutionBounds(partitionCount, maxInFlight, maxElements);
        requireRange(maxKeys, 0, MAX_KEYS, "maxKeys");
    }

    private static void requireRange(int value, int minimum, int maximum, String name) {
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException(name + " must be in [" + minimum + "," + maximum + "]");
        }
    }

    private record Work<T>(long ordinal, T value, boolean end, int group) {
        // Preserve the incumbent private seam used by scheduling contract tests.
        private Work(long ordinal, T value, boolean end) { this(ordinal, value, end, -1); }
    }
    private record Completion<R>(long ordinal, R value, Throwable failure) { }

    /**
     * One existing exact ordinal directory and paged payload lanes per traversal.
     * There is no retained node/bucket object per key. Pages are allocated by the
     * coordinator before queue publication, and each state slot has one FIFO
     * worker owner. The outer page tables never resize during execution.
     */
    private static final class KeyStates<K, A> {
        private static final int PAGE_SHIFT = 10;
        private static final int PAGE_SIZE = 1 << PAGE_SHIFT;
        private static final int PAGE_MASK = PAGE_SIZE - 1;
        private final int maxKeys;
        private final ObjectIntHashMap<K> ordinals = new ObjectIntHashMap<>(0);
        private final ArrayList<K> keys = new ArrayList<>();
        private final Object[][] states;
        private final boolean[][] initialized;
        private final int[][] routes;

        private KeyStates(int maxKeys) {
            this.maxKeys = maxKeys;
            int pages = (maxKeys + PAGE_MASK) >>> PAGE_SHIFT;
            states = new Object[pages][];
            initialized = new boolean[pages][];
            routes = new int[pages][];
        }

        private int register(K key, int lane) {
            int ordinal = ordinals.getOrDefault(key, -1);
            if (ordinal >= 0) {
                if (routes[ordinal >>> PAGE_SHIFT][ordinal & PAGE_MASK] != lane) {
                    throw new IllegalArgumentException("equal key changed partition lane");
                }
                return ordinal;
            }
            ordinal = keys.size();
            if (ordinal >= maxKeys) throw new IllegalStateException("parallel key budget exceeded");
            int page = ordinal >>> PAGE_SHIFT;
            if (states[page] == null) {
                int capacity = Math.min(PAGE_SIZE, maxKeys - (page << PAGE_SHIFT));
                states[page] = new Object[capacity];
                initialized[page] = new boolean[capacity];
                routes[page] = new int[capacity];
            }
            ordinals.put(key, ordinal);
            keys.add(key);
            routes[page][ordinal & PAGE_MASK] = lane;
            return ordinal;
        }

        @SuppressWarnings("unchecked")
        private A initialize(int ordinal, Supplier<? extends A> seed, boolean nullable) {
            int page = ordinal >>> PAGE_SHIFT;
            int slot = ordinal & PAGE_MASK;
            if (!initialized[page][slot]) {
                A state = seed.get();
                if (!nullable) Objects.requireNonNull(state, "seed result");
                states[page][slot] = state;
                initialized[page][slot] = true;
            }
            return (A) states[page][slot];
        }

        private void replace(int ordinal, A value) {
            states[ordinal >>> PAGE_SHIFT][ordinal & PAGE_MASK] = value;
        }

        @SuppressWarnings("unchecked")
        private <R> Map<K, R> finish(Function<? super A, ? extends R> finisher) {
            Object[] keyLane = keys.toArray();
            Object[] resultLane = new Object[keyLane.length];
            for (int ordinal = 0; ordinal < keyLane.length; ordinal++) {
                A state = (A) states[ordinal >>> PAGE_SHIFT][ordinal & PAGE_MASK];
                resultLane[ordinal] = finisher.apply(state);
            }
            return FrozenMap.<K, R>copyOfNullableLanes(keyLane, resultLane, keyLane.length).asMap();
        }
    }

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
