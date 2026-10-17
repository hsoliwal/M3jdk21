// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
package com.m3.collections.parallel;

import com.m3.collections.M3LinkedHashMap;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntBinaryOperator;
import java.util.function.LongSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

/**
 * Client-declared key independence; M3-owned index construction and execution.
 * Strict operations execute equal keys serially in captured encounter order within one terminal call.
 * Only explicit reduceByKey permits concurrent same-key callbacks; its functions must be pure
 * and its combiner must have a true identity and be associative.
 * Different keys MAY overlap: clients must protect effects outside the key boundary.
 * This is not a Stream and does not alter any existing collection/stream contract.
 * Sources must not change during capture. Elements are NOT deep-copied: membership
 * is frozen at planning time; key-affecting state must remain stable while using a plan.
 * Two concurrent terminal calls are independent, not a cross-call locking service.
 * CPU/isolated work only: callbacks must not wait for work on their own key or pool.
 */
public final class M3Parallel {
    private M3Parallel() { }
    /** Native is explicit until an end-to-end crossover has been measured. */
    public enum Backend { JAVA, NATIVE }
    /** Index bytes exclude source/key references, result arrays, and VM object headers. */
    public record Statistics(int elements, int keys, int largestPartition, long indexBytes,
                             Backend backend) { }

    public static <T, K> Plan<T, K> by(Collection<? extends T> source,
                                      Function<? super T, ? extends K> key) {
        return by(source, key, Backend.JAVA);
    }
    public static <T, K> Plan<T, K> by(Collection<? extends T> source,
                                      Function<? super T, ? extends K> key, Backend backend) {
        Objects.requireNonNull(source); Objects.requireNonNull(key); Objects.requireNonNull(backend);
        Object[] items = captureOnce(source);
        int[] ids = new int[items.length];
        M3LinkedHashMap<K, Integer> dictionary = new M3LinkedHashMap<>();
        for (int i = 0; i < items.length; i++) {
            K k = key.apply(element(items, i));
            Integer id = dictionary.get(k);
            if (id == null) { id = dictionary.size(); dictionary.put(k, id); }
            ids[i] = id;
        }
        return plan(items, dictionary.keySet().toArray(), ids, backend);
    }
    /**
     * Single-pass capture for lazy, filtered, or size-expensive Collection owners.
     * Only one compact array of references is retained by the resulting plan.
     */
    static <T> Object[] captureOnce(Collection<? extends T> source) {
        return captureOnce(source, Integer.MAX_VALUE - 8);
    }
    /** Checked dynamic-array capture; a bound is checked before admitting another row. */
    static <T> Object[] captureOnce(Collection<? extends T> source, int maxRows) {
        Objects.requireNonNull(source);
        if (maxRows < 0) throw new IllegalArgumentException("negative capture budget");
        Object[] lane = new Object[Math.min(16, maxRows)];
        int used = 0;
        for (T value : source) {
            if (used == maxRows) throw new IllegalArgumentException("row budget exceeded");
            if (used == lane.length) {
                int old = lane.length;
                long next = (long) old + (old >>> 1) + 1L;
                lane = Arrays.copyOf(lane, (int) Math.min(next, maxRows));
            }
            lane[used++] = value;
        }
        return Arrays.copyOf(lane, used);
    }

    /** Dense integer-key fast path. O(keySpace) temporary lanes; boxes only unique result keys. */
    public static <T> Plan<T, Integer> byInt(Collection<? extends T> source,
                                            ToIntFunction<? super T> key, int keySpace) {
        return byInt(source, key, keySpace, Backend.JAVA);
    }
    public static <T> Plan<T, Integer> byInt(Collection<? extends T> source,
                                            ToIntFunction<? super T> key, int keySpace, Backend backend) {
        Objects.requireNonNull(source); Objects.requireNonNull(key); Objects.requireNonNull(backend);
        if (keySpace < 0) throw new IllegalArgumentException("negative key space");
        Object[] items = captureOnce(source);
        int[] dictionary = new int[keySpace], ids = new int[items.length];
        Object[] keys = new Object[Math.min(items.length, keySpace)]; int count = 0;
        for (int i = 0; i < items.length; i++) {
            int k = key.applyAsInt(element(items, i));
            if (k < 0 || k >= keySpace) throw new IllegalArgumentException("key outside declared space: " + k);
            int encoded = dictionary[k];
            if (encoded == 0) { keys[count] = k; encoded = ++count; dictionary[k] = encoded; }
            ids[i] = encoded - 1;
        }
        return plan(items, Arrays.copyOf(keys, count), ids, backend);
    }
    /**
     * Group connected sets of exclusive resources. The selector must list ALL touched resources,
     * including reads that conflict with another callback's writes. Read/read sharing is also
     * conservatively serialized. Each connected component runs in captured source order;
     * transitive conflicts may intentionally reduce parallelism. Empty resource sets are independent.
     * Result keys are the first source position of each component, not a resource or thread ID.
     */
    public static <T, K> Plan<T, Integer> byKeys(Collection<? extends T> source,
            Function<? super T, ? extends Collection<? extends K>> resources) {
        return byKeys(source, resources, Backend.JAVA);
    }
    public static <T, K> Plan<T, Integer> byKeys(Collection<? extends T> source,
            Function<? super T, ? extends Collection<? extends K>> resources, Backend backend) {
        Objects.requireNonNull(source); Objects.requireNonNull(resources); Objects.requireNonNull(backend);
        Object[] items = captureOnce(source);
        int[] parent = new int[items.length]; Arrays.fill(parent, -1);
        M3LinkedHashMap<K, Integer> first = new M3LinkedHashMap<>();
        for (int row = 0; row < items.length; row++) {
            for (K key : Objects.requireNonNull(resources.apply(element(items, row)), "resource set")) {
                Integer earlier = first.putIfAbsent(key, row);
                if (earlier != null) union(parent, row, earlier);
            }
        }
        int[] ids = new int[items.length], dictionary = new int[items.length];
        Object[] keys = new Object[items.length]; int count = 0;
        for (int row = 0; row < items.length; row++) {
            int root = root(parent, row), encoded = dictionary[root];
            if (encoded == 0) { keys[count] = row; encoded = ++count; dictionary[root] = encoded; }
            ids[row] = encoded - 1;
        }
        return plan(items, Arrays.copyOf(keys, count), ids, backend);
    }
    private static int root(int[] parent, int row) {
        int root = row;
        while (parent[root] >= 0) root = parent[root];
        while (row != root) { int next = parent[row]; parent[row] = root; row = next; }
        return root;
    }
    private static void union(int[] parent, int a, int b) {
        a = root(parent, a); b = root(parent, b); if (a == b) return;
        if (parent[a] > parent[b]) { int swap = a; a = b; b = swap; }
        parent[a] += parent[b]; parent[b] = a;
    }
    /**
     * Half-open comparator ranges; equality with a cut belongs to the range on its right.
     * Cuts must be strictly increasing. Nulls work only when supported by the comparator.
     * Ranges preserve source encounter order; this method does NOT sort the source.
     */
    public static <T, K> Plan<T, Integer> byRange(Collection<? extends T> source,
            Function<? super T, ? extends K> key, List<? extends K> cuts, Comparator<? super K> order) {
        Objects.requireNonNull(key); Objects.requireNonNull(cuts); Objects.requireNonNull(order);
        Object[] bounds = cuts.toArray();
        for (int i = 1; i < bounds.length; i++)
            if (order.compare(element(bounds, i - 1), element(bounds, i)) >= 0)
                throw new IllegalArgumentException("range cuts must be strictly increasing");
        int space = Math.addExact(bounds.length, 1);
        return byInt(source, item -> {
            K value = key.apply(item); int low = 0, high = bounds.length;
            while (low < high) {
                int mid = low + ((high - low) >>> 1);
                if (order.compare(value, element(bounds, mid)) < 0) high = mid; else low = mid + 1;
            }
            return low;
        }, space);
    }
    /**
     * Version covers source membership AND all key-affecting values. It must not exhibit ABA.
     * Capture and execution still require external exclusion of concurrent mutation. This guard
     * diagnoses stale reuse; it is not a lock, coherent concurrent snapshot or rollback mechanism.
     */
    public static <T, K> Plan<T, K> byVersioned(Collection<? extends T> source,
            Function<? super T, ? extends K> key, LongSupplier version) {
        Objects.requireNonNull(version); long expected = version.getAsLong();
        Plan<T, K> p = by(source, key);
        if (version.getAsLong() != expected) throw new ConcurrentModificationException("version changed during capture");
        return new Plan<>(p.items, p.keys, p.layout, p.backend, p.pool, p.cancelled, version, expected);
    }
    /** Single-use, bounded-memory source traversal; the caller owns the iterator/lifecycle. */
    public static <T, K> M3Batches<T, K> batches(Iterator<? extends T> source, int batchSize,
            Function<? super T, ? extends K> key) {
        return new M3Batches<>(source, batchSize, key);
    }
    /** Complete read/write resource declarations; conflicts retain source order, readers may overlap. */
    public static <T, K> M3ReadWrite<T> byReadWrite(Collection<? extends T> source,
            Function<? super T, ? extends Collection<? extends K>> reads,
            Function<? super T, ? extends Collection<? extends K>> writes) {
        return byReadWrite(source, reads, writes, M3ReadWrite.Budget.representable());
    }
    public static <T, K> M3ReadWrite<T> byReadWrite(Collection<? extends T> source,
            Function<? super T, ? extends Collection<? extends K>> reads,
            Function<? super T, ? extends Collection<? extends K>> writes, M3ReadWrite.Budget budget) {
        return M3ReadWrite.capture(source, reads, writes, budget);
    }
    private static <T, K> Plan<T, K> plan(Object[] items, Object[] keys, int[] ids, Backend backend) {
        return new Plan<>(items, keys, M3PartitionLayout.build(ids, keys.length, backend), backend,
                ForkJoinPool.commonPool(), () -> false, null, 0L);
    }
    @SuppressWarnings("unchecked") private static <T> T element(Object[] values, int index) {
        return (T) values[index];
    }
    private static <T> List<T> readOnly(Object[] values) {
        return Collections.unmodifiableList(Arrays.asList(M3Parallel.<T[]>cast(values)));
    }
    @SuppressWarnings("unchecked") private static <T> T cast(Object value) { return (T) value; }

    /** Immutable reusable membership/index plan. Configuration methods return a new view. */
    public static final class Plan<T, K> {
        private final Object[] items, keys;
        private final M3PartitionLayout layout;
        private final Backend backend;
        private final ForkJoinPool pool;
        private final BooleanSupplier cancelled;
        private final LongSupplier version;
        private final long expectedVersion;
        private Plan(Object[] items, Object[] keys, M3PartitionLayout layout, Backend backend,
                     ForkJoinPool pool, BooleanSupplier cancelled, LongSupplier version, long expectedVersion) {
            this.items = items; this.keys = keys; this.layout = layout;
            this.backend = backend; this.pool = pool; this.cancelled = cancelled;
            this.version = version; this.expectedVersion = expectedVersion;
        }
        /** The caller owns the pool; never shut it down until this call has returned. */
        public Plan<T, K> using(ForkJoinPool executor) {
            return new Plan<>(items, keys, layout, backend, Objects.requireNonNull(executor), cancelled, version, expectedVersion);
        }
        /** Cooperative, thread-safe predicate. Never interrupts or rolls back a started callback. */
        public Plan<T, K> cancelWhen(BooleanSupplier predicate) {
            return new Plan<>(items, keys, layout, backend, pool, Objects.requireNonNull(predicate), version, expectedVersion);
        }
        public Statistics explain() {
            checkVersion();
            int largest = 0;
            for (int g = 0; g < keys.length; g++) largest = Math.max(largest, layout.end(g) - layout.start(g));
            return new Statistics(items.length, keys.length, largest, layout.bytes(), backend);
        }
        public List<K> keys() { checkVersion(); return readOnly(keys); }
        public void forEach(Consumer<? super T> action) {
            Objects.requireNonNull(action);
            rows(i -> action.accept(element(items, i)));
        }
        /** Borrowed coordinator; completion means all started callbacks are quiescent. */
        public M3Execution<Void> forEachAsync(Executor coordinator, Consumer<? super T> action) {
            Objects.requireNonNull(action);
            return M3Execution.submit(coordinator, stop -> {
                cancelWhen(() -> cancelled.getAsBoolean() || stop.getAsBoolean()).forEach(action); return null;
            });
        }
        public <R> M3Execution<List<R>> mapOrderedAsync(Executor coordinator,
                Function<? super T, ? extends R> mapper) {
            Objects.requireNonNull(mapper);
            return M3Execution.submit(coordinator, stop ->
                    cancelWhen(() -> cancelled.getAsBoolean() || stop.getAsBoolean()).mapOrdered(mapper));
        }
        /** Restores captured result positions, not cross-key side-effect order; null results supported. */
        public <R> List<R> mapOrdered(Function<? super T, ? extends R> mapper) {
            Objects.requireNonNull(mapper); Object[] result = new Object[items.length];
            rows(i -> result[i] = mapper.apply(element(items, i)));
            return readOnly(result);
        }
        /** Stable selection mask followed by serial compaction; predicates obey the same key contract. */
        public List<T> filter(Predicate<? super T> predicate) {
            Objects.requireNonNull(predicate);
            // Partition workers can visit different source rows in the same mask
            // word. Atomic bitwise OR preserves both writes; no per-row wrapper.
            M3SelectionBits selected = new M3SelectionBits(items.length);
            rows(i -> { if (predicate.test(element(items, i))) selected.mark(i); });
            return readOnly(selected.compact(items));
        }
        /**
         * Ordered per-key left fold: no associativity/commutativity assumption or combiner.
         * Each initial state must be independent. Keys/results may be null; first-key order is retained.
         */
        public <A> Map<K, A> foldByKey(Supplier<? extends A> initial,
                                      BiFunction<? super A, ? super T, ? extends A> fold) {
            Objects.requireNonNull(initial); Objects.requireNonNull(fold);
            Object[] values = new Object[keys.length];
            groups((group, failure) -> {
                A value = initial.get();
                for (int p = layout.start(group); p < layout.end(group); p++) {
                    if (stop(failure)) return;
                    value = fold.apply(value, element(items, layout.row(p)));
                }
                values[group] = value;
            });
            M3LinkedHashMap<K, A> result = new M3LinkedHashMap<>();
            for (int g = 0; g < keys.length; g++) result.put(element(keys, g), element(values, g));
            return Collections.unmodifiableMap(result);
        }
        public <A> Map<K, A> collectByKey(Supplier<? extends A> initial, BiConsumer<? super A, ? super T> add) {
            Objects.requireNonNull(add);
            return foldByKey(initial, (a, item) -> { add.accept(a, item); return a; });
        }
        /** First source occurrence per key; stable distinct when the key selector is identity. */
        public List<T> firstByKey() {
            Object[] values = new Object[keys.length];
            groups((g, failure) -> values[g] = items[layout.row(layout.start(g))]);
            return readOnly(values);
        }
        /**
         * Best k elements per key according to a pure best-first comparator (negative means better).
         * Ties retain original encounter order. k=0 yields an empty list for every key.
         * Primitive row-index max heaps retain no per-candidate Entry, Node or boxed row object.
         */
        public Map<K, List<T>> topByKey(int k, Comparator<? super T> bestFirst) {
            if (k < 0) throw new IllegalArgumentException("negative k");
            Objects.requireNonNull(bestFirst); Object[] values = new Object[keys.length];
            IntBinaryOperator compare = (a, b) -> {
                int c = bestFirst.compare(element(items, a), element(items, b));
                return c == 0 ? Integer.compare(a, b) : c;
            };
            groups((g, failure) -> {
                int size = Math.min(k, layout.end(g) - layout.start(g));
                int[] heap = new int[size]; int used = 0;
                if (size > 0) for (int p = layout.start(g); p < layout.end(g); p++) {
                    if (stop(failure)) return;
                    int row = layout.row(p);
                    if (used < size) {
                        int at = used++;
                        while (at > 0) {
                            int parent = (at - 1) >>> 1;
                            if (compare.applyAsInt(heap[parent], row) >= 0) break;
                            heap[at] = heap[parent]; at = parent;
                        }
                        heap[at] = row;
                    } else if (compare.applyAsInt(row, heap[0]) < 0) {
                        heap[0] = row; sift(heap, size, compare);
                    }
                }
                for (int remaining = size - 1; remaining > 0; remaining--) {
                    if (stop(failure)) return;
                    int swap = heap[0]; heap[0] = heap[remaining]; heap[remaining] = swap;
                    sift(heap, remaining, compare);
                }
                Object[] selected = new Object[size];
                for (int i = 0; i < size; i++) selected[i] = items[heap[i]];
                values[g] = readOnly(selected);
            });
            return resultMap(values);
        }
        private static void sift(int[] heap, int size, IntBinaryOperator compare) {
            int at = 0, value = heap[0];
            while (at < size / 2) {
                int child = 2 * at + 1;
                if (child + 1 < size && compare.applyAsInt(heap[child + 1], heap[child]) > 0) child++;
                if (compare.applyAsInt(value, heap[child]) >= 0) break;
                heap[at] = heap[child]; at = child;
            }
            heap[at] = value;
        }
        /**
         * Inclusive-center, per-key neighborhoods truncated at that key's boundaries.
         * Output positions match the source; window views are read-only, not element deep copies.
         * The mapper must not mutate data read through another window or escape async work.
         */
        public <R> List<R> mapWindows(int preceding, int following,
                BiFunction<? super T, ? super List<T>, ? extends R> mapper) {
            if (preceding < 0 || following < 0) throw new IllegalArgumentException("negative window radius");
            Objects.requireNonNull(mapper); Object[] values = new Object[items.length];
            groups((g, failure) -> {
                int start = layout.start(g), end = layout.end(g);
                for (int p = start; p < end; p++) {
                    if (stop(failure)) return;
                    int from = (int) Math.max(start, (long) p - preceding);
                    int to = (int) Math.min(end, (long) p + following + 1L);
                    int row = layout.row(p);
                    List<T> window = Collections.unmodifiableList(new PartitionView(from, to));
                    values[row] = mapper.apply(element(items, row), window);
                }
            });
            return readOnly(values);
        }
        /** Default 1024-row chunks; not a measured crossover or automatic optimization of foldByKey. */
        public <A> Map<K, A> reduceByKey(Supplier<? extends A> identity,
                Function<? super T, ? extends A> lift, BinaryOperator<A> combine) {
            return reduceByKey(1024, identity, lift, combine);
        }
        /**
         * Explicit opt-in to splitting hot keys: identity, lift and combine must be pure and
         * independent; combine must be associative with identity on both sides. Non-commutative
         * associative operations are supported: contiguous chunks merge in encounter order.
         * Same-key callbacks MAY overlap here. Floating point addition is not generally associative.
         * Strict foldByKey remains unchanged and is the correct operation without these laws.
         */
        public <A> Map<K, A> reduceByKey(int chunkSize, Supplier<? extends A> identity,
                Function<? super T, ? extends A> lift, BinaryOperator<A> combine) {
            if (chunkSize <= 0) throw new IllegalArgumentException("nonpositive chunk size");
            Objects.requireNonNull(identity); Objects.requireNonNull(lift); Objects.requireNonNull(combine);
            int[] offsets = new int[keys.length + 1];
            for (int g = 0; g < keys.length; g++) {
                int n = layout.end(g) - layout.start(g);
                offsets[g + 1] = Math.addExact(offsets[g], 1 + (n - 1) / chunkSize);
            }
            Object[] partial = new Object[offsets[keys.length]];
            jobs(partial.length, (chunk, failure) -> {
                int found = Arrays.binarySearch(offsets, chunk), g = found >= 0 ? found : -found - 2;
                int from = layout.start(g) + (chunk - offsets[g]) * chunkSize;
                int to = (int) Math.min(layout.end(g), (long) from + chunkSize);
                A acc = identity.get();
                for (int p = from; p < to; p++) {
                    if (stop(failure)) return;
                    acc = combine.apply(acc, lift.apply(element(items, layout.row(p))));
                }
                partial[chunk] = acc;
            });
            Object[] values = new Object[keys.length];
            groups((g, failure) -> {
                A acc = element(partial, offsets[g]);
                for (int c = offsets[g] + 1; c < offsets[g + 1]; c++) {
                    if (stop(failure)) return;
                    acc = combine.apply(acc, element(partial, c));
                }
                values[g] = acc;
            });
            return resultMap(values);
        }
        private <A> Map<K, A> resultMap(Object[] values) {
            M3LinkedHashMap<K, A> result = new M3LinkedHashMap<>();
            for (int g = 0; g < keys.length; g++) result.put(element(keys, g), element(values, g));
            checkVersion(); return Collections.unmodifiableMap(result);
        }
        /** One read-only indexed view per key, no copied element lists. Retaining a view retains the snapshot. */
        public void forEachPartition(BiConsumer<? super K, ? super List<T>> action) {
            Objects.requireNonNull(action);
            groups((group, failure) -> action.accept(element(keys, group),
                    Collections.unmodifiableList(new PartitionView(group))));
        }
        private final class PartitionView extends AbstractList<T> implements RandomAccess {
            private final int from, to;
            PartitionView(int group) { this(layout.start(group), layout.end(group)); }
            PartitionView(int from, int to) { this.from = from; this.to = to; }
            @Override public int size() { return to - from; }
            @Override public T get(int index) {
                Objects.checkIndex(index, size()); return element(items, layout.row(from + index));
            }
        }
        private void rows(IntConsumer action) {
            groups((group, failure) -> {
                for (int p = layout.start(group); p < layout.end(group); p++) {
                    if (stop(failure)) return;
                    action.accept(layout.row(p));
                }
            });
        }
        private void checkVersion() {
            if (version != null && version.getAsLong() != expectedVersion)
                throw new ConcurrentModificationException("keyed plan version changed");
        }
        private boolean stop(AtomicReference<Throwable> failure) {
            if (failure.get() != null) return true;
            checkVersion();
            if (!cancelled.getAsBoolean()) return false;
            failure.compareAndSet(null, new CancellationException("keyed operation cancelled")); return true;
        }
        private void groups(GroupAction action) { jobs(keys.length, action); }
        private void jobs(int count, GroupAction action) {
            AtomicReference<Throwable> failure = new AtomicReference<>();
            if (stop(failure)) rethrow(failure.get());
            if (count == 0) return;
            // At most one task per worker; partition count does not create an unbounded queue.
            int workers = Math.min(count, Math.max(1, pool.getParallelism()));
            AtomicInteger next = new AtomicInteger();
            workers(pool, workers, () -> {
                try {
                    while (!stop(failure)) {
                        int g = claim(next, count);
                        if (g >= count) return;
                        if (!stop(failure)) action.run(g, failure);
                    }
                } catch (Throwable error) { failure.compareAndSet(null, error); }
            }, failure);
            rethrow(failure.get()); checkVersion();
        }
    }
    /** Shared bounded worker lifetime: drain all forked tasks even after failure. */
    static void workers(ForkJoinPool pool, int count, Runnable body, AtomicReference<Throwable> failure) {
        Worker[] tasks = new Worker[count];
        for (int w = 0; w < count; w++) tasks[w] = new Worker(body);
        Worker root = new Worker(() -> {
            int forked = 0;
            try {
                for (; forked < tasks.length - 1; forked++) tasks[forked].fork();
                tasks[tasks.length - 1].invoke();
            } catch (Throwable error) { failure.compareAndSet(null, error); }
            finally {
                // Do not cancel futures: completion must mean every started callback is quiescent.
                for (int i = forked - 1; i >= 0; i--) {
                    try { tasks[i].join(); }
                    catch (Throwable error) { failure.compareAndSet(null, error); }
                }
            }
        });
        if (ForkJoinTask.getPool() == pool) root.invoke(); else pool.invoke(root);
    }
    /** Saturating claim: no overflow or negative task ID even at Integer.MAX_VALUE. */
    static int claim(AtomicInteger next, int limit) {
        for (;;) {
            int value = next.get();
            if (value >= limit) return limit;
            if (next.compareAndSet(value, value + 1)) return value;
        }
    }
    @FunctionalInterface private interface GroupAction {
        void run(int group, AtomicReference<Throwable> failure);
    }
    private static final class Worker extends RecursiveAction {
        private static final long serialVersionUID = 1L;
        private final transient Runnable work;
        Worker(Runnable work) { this.work = work; }
        @Override protected void compute() { work.run(); }
    }
    static void rethrow(Throwable error) {
        if (error == null) return;
        if (error instanceof RuntimeException runtime) throw runtime;
        if (error instanceof Error fatal) throw fatal;
        throw new IllegalStateException("callback threw an undeclared checked exception", error);
    }
}
