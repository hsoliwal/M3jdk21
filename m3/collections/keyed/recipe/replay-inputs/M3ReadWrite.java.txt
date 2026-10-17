// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
package com.m3.collections.parallel;

import com.m3.collections.M3LinkedHashMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;

/**
 * Reusable forward dependency graph from COMPLETE client read/write resource sets.
 * Conflicting rows execute in captured encounter order; read/read overlap is permitted.
 * Declared resources/equality must remain stable, callbacks must finish their effects before
 * returning, and all undeclared effects need separate protection. No key inference, lock
 * service across terminal calls, snapshot isolation, retry or transactional rollback is implied.
 */
public final class M3ReadWrite<T> {
    /** Hard admission counts, not estimates of total heap bytes. Duplicates count as touches. */
    public record Budget(int rows, int resourceTouches, int dependencies) {
        public Budget {
            if (rows < 0 || resourceTouches < 0 || dependencies < 0)
                throw new IllegalArgumentException("negative planning budget");
        }
        /** Representation ceiling only; applications should set smaller memory-aware limits. */
        public static Budget representable() {
            return new Budget(Integer.MAX_VALUE - 8, Integer.MAX_VALUE - 8, Integer.MAX_VALUE - 8);
        }
    }
    /** Graph bytes exclude source references, planning scratch, execution scratch and VM headers. */
    public record Statistics(int elements, int resources, int touches, int dependencies, long graphBytes) { }
    private final Object[] items;
    private final int[] heads, targets, links, prerequisites;
    private final Statistics statistics;
    private final ForkJoinPool pool;
    private final BooleanSupplier cancelled;

    private M3ReadWrite(Object[] items, int[] heads, int[] targets, int[] links, int[] prerequisites,
            Statistics statistics, ForkJoinPool pool, BooleanSupplier cancelled) {
        this.items = items; this.heads = heads; this.targets = targets; this.links = links;
        this.prerequisites = prerequisites; this.statistics = statistics;
        this.pool = Objects.requireNonNull(pool); this.cancelled = Objects.requireNonNull(cancelled);
    }
    static <T, K> M3ReadWrite<T> capture(Collection<? extends T> source,
            Function<? super T, ? extends Collection<? extends K>> reads,
            Function<? super T, ? extends Collection<? extends K>> writes, Budget budget) {
        Objects.requireNonNull(source); Objects.requireNonNull(reads); Objects.requireNonNull(writes);
        Objects.requireNonNull(budget);
        Object[] items = M3Parallel.captureOnce(source, budget.rows());
        if (items.length > budget.rows()) throw new IllegalArgumentException("row budget exceeded");
        Builder<K> b = new Builder<>(items.length, budget);
        for (int row = 0; row < items.length; row++) {
            T item = element(items, row);
            Collection<? extends K> read = Objects.requireNonNull(reads.apply(item), "read resources");
            Collection<? extends K> write = Objects.requireNonNull(writes.apply(item), "write resources");
            // Writes dominate same-row reads. All edges are from a strictly earlier row.
            for (K key : write) b.write(b.resource(key), row);
            for (K key : read) b.read(b.resource(key), row);
        }
        Statistics stats = new Statistics(items.length, b.dictionary.size(), b.touches, b.edgeCount,
                8L * (items.length + (long) b.edgeCount));
        return new M3ReadWrite<>(items, b.heads, Arrays.copyOf(b.to, b.edgeCount),
                Arrays.copyOf(b.next, b.edgeCount), b.indegree, stats, ForkJoinPool.commonPool(), () -> false);
    }
    public M3ReadWrite<T> using(ForkJoinPool executor) {
        return new M3ReadWrite<>(items, heads, targets, links, prerequisites, statistics, executor, cancelled);
    }
    /**
     * Cooperative cancellation. The predicate must be thread-safe and terminate promptly.
     * Started callbacks are drained, never interrupted or rolled back.
     */
    public M3ReadWrite<T> cancelWhen(BooleanSupplier predicate) {
        return new M3ReadWrite<>(items, heads, targets, links, prerequisites, statistics, pool, predicate);
    }
    public Statistics explain() { return statistics; }
    public void forEach(Consumer<? super T> action) {
        Objects.requireNonNull(action); execute(row -> action.accept(element(items, row)));
    }
    /** Result positions, not all side effects, follow captured source order. Nulls are supported. */
    public <R> List<R> mapOrdered(Function<? super T, ? extends R> mapper) {
        Objects.requireNonNull(mapper); Object[] values = new Object[items.length];
        execute(row -> values[row] = mapper.apply(element(items, row)));
        return Collections.unmodifiableList(Arrays.asList(M3ReadWrite.<R[]>cast(values)));
    }
    /** Asynchronous terminal execution; completion means all started callbacks have stopped. */
    public M3Execution<Void> forEachAsync(Executor coordinator, Consumer<? super T> action) {
        Objects.requireNonNull(action);
        return M3Execution.submit(coordinator, stop -> {
            cancelWhen(() -> cancelled.getAsBoolean() || stop.getAsBoolean()).forEach(action); return null;
        });
    }
    private void execute(IntConsumer action) {
        State state = new State(action);
        if (state.stop()) M3Parallel.rethrow(state.failure.get());
        if (items.length == 0) return;
        M3Parallel.workers(pool, Math.min(items.length, Math.max(1, pool.getParallelism())),
                state::work, state.failure);
        M3Parallel.rethrow(state.failure.get());
    }
    private final class State {
        private final IntConsumer action;
        private final int[] pending = prerequisites.clone(), ready = new int[items.length];
        private final AtomicReference<Throwable> failure = new AtomicReference<>();
        private int take, put, completed;
        State(IntConsumer action) {
            this.action = action;
            for (int row = 0; row < pending.length; row++) if (pending[row] == 0) ready[put++] = row;
        }
        private void fail(Throwable error) {
            failure.compareAndSet(null, error);
            synchronized (this) { notifyAll(); }
        }
        private boolean stop() {
            if (failure.get() != null) return true;
            try {
                if (cancelled.getAsBoolean()) fail(new CancellationException("read/write execution cancelled"));
            } catch (Throwable error) { fail(error); }
            return failure.get() != null;
        }
        void work() {
            PollWait waiter = new PollWait();
            try {
                for (;;) {
                    if (stop()) return;
                    int row;
                    synchronized (this) {
                        if (completed == items.length || failure.get() != null) return;
                        row = take < put ? ready[take++] : -1;
                    }
                    if (row < 0) { waiter.reset(); ForkJoinPool.managedBlock(waiter); continue; }
                    if (stop()) return;
                    action.accept(row);
                    synchronized (this) {
                        completed++;
                        for (int edge = heads[row]; edge >= 0; edge = links[edge]) {
                            int target = targets[edge];
                            if (--pending[target] == 0) ready[put++] = target;
                        }
                        notifyAll();
                    }
                }
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                CancellationException cancellation = new CancellationException("read/write worker interrupted");
                cancellation.initCause(error); fail(cancellation);
            } catch (Throwable error) { fail(error); }
        }
        /** One reusable blocker per worker; a poll deadline releases it to test cancellation outside locks. */
        private final class PollWait implements ForkJoinPool.ManagedBlocker {
            private long deadline;
            void reset() { deadline = System.nanoTime() + 10_000_000L; }
            @Override public boolean isReleasable() {
                synchronized (State.this) {
                    return take < put || completed == items.length || failure.get() != null ||
                            System.nanoTime() - deadline >= 0;
                }
            }
            @Override public boolean block() throws InterruptedException {
                synchronized (State.this) {
                    if (!isReleasable()) {
                        long remaining = deadline - System.nanoTime();
                        if (remaining > 0) State.this.wait(remaining / 1_000_000L, (int) (remaining % 1_000_000L));
                    }
                    return isReleasable();
                }
            }
        }
    }
    /** Transient primitive resource/read chains; no per-edge object or retained resource dictionary. */
    private static final class Builder<K> {
        final Budget budget;
        final M3LinkedHashMap<K, Integer> dictionary = new M3LinkedHashMap<>();
        final int[] heads, indegree, seenPredecessor;
        int[] writer = new int[0], readerHead = new int[0], lastRead = new int[0];
        int[] readerRow = new int[0], readerNext = new int[0];
        int[] to = new int[0], next = new int[0];
        int touches, readers, edgeCount;
        Builder(int rows, Budget budget) {
            this.budget = budget; heads = new int[rows]; Arrays.fill(heads, -1);
            indegree = new int[rows]; seenPredecessor = new int[rows]; Arrays.fill(seenPredecessor, -1);
        }
        int resource(K key) {
            if (touches == budget.resourceTouches()) throw new IllegalArgumentException("resource touch budget exceeded");
            touches++;
            Integer old = dictionary.get(key); if (old != null) return old;
            int id = dictionary.size();
            if (id == writer.length) {
                int capacity = capacity(id, budget.resourceTouches());
                writer = Arrays.copyOf(writer, capacity); Arrays.fill(writer, id, capacity, -1);
                readerHead = Arrays.copyOf(readerHead, capacity); Arrays.fill(readerHead, id, capacity, -1);
                lastRead = Arrays.copyOf(lastRead, capacity); Arrays.fill(lastRead, id, capacity, -1);
            }
            dictionary.put(key, id); return id;
        }
        void write(int resource, int row) {
            if (writer[resource] == row) return;
            edge(writer[resource], row);
            for (int r = readerHead[resource]; r >= 0; r = readerNext[r]) edge(readerRow[r], row);
            readerHead[resource] = -1; writer[resource] = row;
        }
        void read(int resource, int row) {
            if (writer[resource] == row || lastRead[resource] == row) return;
            edge(writer[resource], row); lastRead[resource] = row;
            if (readers == readerRow.length) {
                int capacity = capacity(readers, budget.resourceTouches());
                readerRow = Arrays.copyOf(readerRow, capacity); readerNext = Arrays.copyOf(readerNext, capacity);
            }
            readerRow[readers] = row; readerNext[readers] = readerHead[resource]; readerHead[resource] = readers++;
        }
        void edge(int from, int row) {
            if (from < 0 || seenPredecessor[from] == row) return;
            if (from >= row) throw new IllegalStateException("non-forward dependency");
            if (edgeCount == budget.dependencies()) throw new IllegalArgumentException("dependency budget exceeded");
            if (edgeCount == to.length) {
                int capacity = capacity(edgeCount, budget.dependencies());
                to = Arrays.copyOf(to, capacity); next = Arrays.copyOf(next, capacity);
            }
            seenPredecessor[from] = row; to[edgeCount] = row; next[edgeCount] = heads[from];
            heads[from] = edgeCount++; indegree[row]++;
        }
        static int capacity(int old, int limit) {
            long next = Math.max(8L, old + (old >> 1) + 1L);
            int capacity = (int) Math.min(Math.min(Integer.MAX_VALUE - 8L, limit), next);
            if (capacity <= old) throw new IllegalArgumentException("representable planning capacity exceeded");
            return capacity;
        }
    }
    @SuppressWarnings("unchecked") private static <T> T element(Object[] values, int index) { return (T) values[index]; }
    @SuppressWarnings("unchecked") private static <T> T cast(Object value) { return (T) value; }
}
