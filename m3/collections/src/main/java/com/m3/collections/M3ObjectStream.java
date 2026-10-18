/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Replay-aware object stream plan backed by M3's array/index snapshot owner.
 *
 * <p>This augments rather than replaces {@link Stream}. Collection inputs can be
 * explicitly snapshotted or traversed live; a wrapped JDK stream stays one-shot
 * unless its bounded output is marked. Stateful operators fail closed at their
 * caller-supplied budget. Operations are sequential by default, preserve encounter
 * order, and never assume a reduction is associative.</p>
 *
 * <p>Object references and physical occurrence order remain in
 * {@link M3IndexedListSnapshot}; filtered/sorted projections use M3's integer index
 * lane and byte-mask selector. JNI receives no Java object references, predicates,
 * comparators, or mapped values.</p>
 *
 * @param <T> element type; null values are preserved unless an operator's own contract says otherwise
 */
public final class M3ObjectStream<T> {
    private static final int MAX_STATEFUL_WIDTH = 65_536;
    private final Supplier<? extends Stream<T>> opener;
    private final boolean replayable;

    private M3ObjectStream(Supplier<? extends Stream<T>> opener, boolean replayable) {
        this.opener = Objects.requireNonNull(opener, "opener");
        this.replayable = replayable;
    }

    /** Capture references and occurrence order once in M3's Object[]/int[] owner. */
    public static <T> M3ObjectStream<T> snapshot(Collection<? extends T> source) {
        return fromSnapshot(M3IndexedListSnapshot.copyOf(Objects.requireNonNull(source, "source")));
    }

    /** Snapshot an Iterable such as an Eclipse RichIterable without requiring a Collection API. */
    public static <T> M3ObjectStream<T> snapshot(Iterable<? extends T> source) {
        Objects.requireNonNull(source, "source");
        if (source instanceof Collection<?> collection) {
            @SuppressWarnings("unchecked") Collection<? extends T> typed = (Collection<? extends T>) collection;
            return snapshot(typed);
        }
        ArrayList<T> values = new ArrayList<>();
        for (T value : source) values.add(value);
        return snapshot(values);
    }

    private static <T> M3ObjectStream<T> fromSnapshot(M3IndexedListSnapshot<T> snapshot) {
        return new M3ObjectStream<>(snapshot::stream, true);
    }

    /** Traverse a live collection on every evaluation; the source owns concurrent-modification semantics. */
    public static <T> M3ObjectStream<T> live(Collection<T> source) {
        Objects.requireNonNull(source, "source");
        return new M3ObjectStream<>(() -> source.stream().sequential(), true);
    }

    /** Own exactly one existing JDK stream. It is consumed on first open. */
    public static <T> M3ObjectStream<T> once(Stream<T> source) {
        AtomicReference<Stream<T>> pending = new AtomicReference<>(Objects.requireNonNull(source, "source"));
        return new M3ObjectStream<>(() -> {
            Stream<T> next = pending.getAndSet(null);
            if (next == null) throw new IllegalStateException("one-shot stream already consumed; mark before reuse");
            return next.sequential();
        }, false);
    }

    public boolean replayable() { return replayable; }

    /** A fresh sequential traversal. The caller must close this stream, including after short-circuiting. */
    public Stream<T> stream() {
        return Objects.requireNonNull(opener.get(), "stream factory returned null").sequential();
    }

    private M3ObjectStream<T> stage(Function<Stream<T>, Stream<T>> operation) {
        Objects.requireNonNull(operation, "operation");
        return new M3ObjectStream<>(() -> operation.apply(stream()), replayable);
    }

    public M3ObjectStream<T> filter(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate");
        return stage(s -> s.filter(predicate));
    }

    public <R> M3ObjectStream<R> map(Function<? super T, ? extends R> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        return new M3ObjectStream<>(() -> stream().map(mapper), replayable);
    }

    /** Map one input to a finite Iterable without first allocating an intermediate result collection. */
    public <R> M3ObjectStream<R> flatMapIterable(
            Function<? super T, ? extends Iterable<? extends R>> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        return new M3ObjectStream<>(() -> stream().flatMap(item -> {
            Iterable<? extends R> values = Objects.requireNonNull(mapper.apply(item), "mapped iterable");
            return StreamSupport.stream(values.spliterator(), false);
        }), replayable);
    }

    public <R> M3ObjectStream<R> flatMap(Function<? super T, ? extends Stream<? extends R>> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        return new M3ObjectStream<>(() -> stream().flatMap(item ->
                Objects.requireNonNull(mapper.apply(item), "mapped stream")), replayable);
    }

    /** Emits zero or more mapped outputs without requiring a temporary collection. */
    public <R> M3ObjectStream<R> mapMulti(
            BiConsumer<? super T, ? super Consumer<R>> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        return new M3ObjectStream<>(() -> stream().<R>mapMulti(mapper), replayable);
    }

    public M3ObjectStream<T> peek(Consumer<? super T> observer) {
        Objects.requireNonNull(observer, "observer");
        return stage(s -> s.peek(observer));
    }

    public M3ObjectStream<T> skip(long count) {
        if (count < 0) throw new IllegalArgumentException("skip must be nonnegative");
        return stage(s -> s.skip(count));
    }

    public M3ObjectStream<T> limit(long count) {
        if (count < 0) throw new IllegalArgumentException("limit must be nonnegative");
        return stage(s -> s.limit(count));
    }

    public M3ObjectStream<T> takeWhile(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate");
        return stage(s -> s.takeWhile(predicate));
    }

    public M3ObjectStream<T> dropWhile(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate");
        return stage(s -> s.dropWhile(predicate));
    }

    /** Stable first-occurrence distinctness with a hard key-state cap. */
    public M3ObjectStream<T> distinct(int maxDistinct) {
        requireNonNegative(maxDistinct, "maxDistinct");
        return stage(s -> {
            Set<T> seen = new HashSet<>();
            return s.filter(value -> {
                if (seen.contains(value)) return false;
                if (seen.size() >= maxDistinct) throw new IllegalStateException("distinct key budget exceeded");
                seen.add(value);
                return true;
            });
        });
    }

    /** Stable bounded sort through M3's payload-array/index-directory owner, not per-element nodes. */
    public M3ObjectStream<T> sorted(Comparator<? super T> comparator, int maxElements) {
        Objects.requireNonNull(comparator, "comparator");
        requireNonNegative(maxElements, "maxElements");
        return new M3ObjectStream<>(() -> {
            List<T> values = toBoundedList(maxElements);
            return M3IndexedListSnapshot.copyOf(values).sorted(comparator).stream();
        }, replayable);
    }

    /** Stable natural-order sort, subject to the elements' Comparable contract. */
    public M3ObjectStream<T> sorted(int maxElements) {
        requireNonNegative(maxElements, "maxElements");
        return new M3ObjectStream<>(() -> {
            List<T> values = toBoundedList(maxElements);
            return M3IndexedListSnapshot.copyOf(values).sorted().stream();
        }, replayable);
    }

    /** Non-overlapping immutable batches. Tail policy is applied only to the final incomplete batch. */
    public M3ObjectStream<List<T>> batch(int size, TailPolicy tailPolicy) {
        requireWidth(size);
        Objects.requireNonNull(tailPolicy, "tailPolicy");
        return new M3ObjectStream<>(() -> {
            Stream<T> upstream = stream();
            Iterator<T> input = upstream.iterator();
            Spliterator<List<T>> batches = new Spliterators.AbstractSpliterator<>(
                    Long.MAX_VALUE, Spliterator.ORDERED | Spliterator.NONNULL) {
                @Override public boolean tryAdvance(Consumer<? super List<T>> action) {
                    Objects.requireNonNull(action, "action");
                    ArrayList<T> values = new ArrayList<>(size);
                    while (values.size() < size && input.hasNext()) values.add(input.next());
                    if (values.isEmpty()) return false;
                    if (values.size() < size && tailPolicy == TailPolicy.DROP) return false;
                    action.accept(Collections.unmodifiableList(values));
                    return true;
                }
            };
            return StreamSupport.stream(batches, false).onClose(upstream::close);
        }, replayable);
    }

    public M3ObjectStream<List<T>> batch(int size) { return batch(size, TailPolicy.INCLUDE); }

    /** Full sliding windows only; ring scratch is O(width) and each emitted window is an immutable copy. */
    public M3ObjectStream<List<T>> window(int width) {
        requireWidth(width);
        return new M3ObjectStream<>(() -> {
            Stream<T> upstream = stream();
            Iterator<T> input = upstream.iterator();
            Spliterator<List<T>> windows = new Spliterators.AbstractSpliterator<>(
                    Long.MAX_VALUE, Spliterator.ORDERED | Spliterator.NONNULL) {
                private final Object[] ring = new Object[width];
                private boolean initialized;
                private boolean exhausted;
                private int head;

                @Override public boolean tryAdvance(Consumer<? super List<T>> action) {
                    Objects.requireNonNull(action, "action");
                    if (exhausted) return false;
                    if (!initialized) {
                        for (int i = 0; i < width; i++) {
                            if (!input.hasNext()) { exhausted = true; return false; }
                            ring[i] = input.next();
                        }
                        initialized = true;
                    } else {
                        if (!input.hasNext()) { exhausted = true; return false; }
                        ring[head] = input.next();
                        head = (head + 1) % width;
                    }
                    ArrayList<T> values = new ArrayList<>(width);
                    for (int i = 0; i < width; i++) {
                        @SuppressWarnings("unchecked") T value = (T) ring[(head + i) % width];
                        values.add(value);
                    }
                    action.accept(Collections.unmodifiableList(values));
                    return true;
                }
            };
            return StreamSupport.stream(windows, false).onClose(upstream::close);
        }, replayable);
    }

    /** Prefix fold; emits one accumulated state per input and emits no seed for an empty input. */
    public <A> M3ObjectStream<A> scan(
            Supplier<? extends A> initializer,
            BiFunction<? super A, ? super T, ? extends A> folder) {
        Objects.requireNonNull(initializer, "initializer");
        Objects.requireNonNull(folder, "folder");
        return new M3ObjectStream<>(() -> {
            Stream<T> upstream = stream();
            Iterator<T> input = upstream.iterator();
            Iterator<A> output = new Iterator<>() {
                private A state;
                private boolean initialized;
                @Override public boolean hasNext() { return input.hasNext(); }
                @Override public A next() {
                    T item = input.next();
                    if (!initialized) { state = initializer.get(); initialized = true; }
                    state = folder.apply(state, item);
                    return state;
                }
            };
            return StreamSupport.stream(Spliterators.spliteratorUnknownSize(output, Spliterator.ORDERED), false)
                    .onClose(upstream::close);
        }, replayable);
    }

    /** Eager bounded capture of the complete current plan output, not a bookmark into the source cursor. */
    public Mark<T> mark(int maxElements) {
        return new Mark<>(M3IndexedListSnapshot.copyOf(toBoundedList(maxElements)));
    }

    public static final class Mark<E> {
        private final M3IndexedListSnapshot<E> captured;
        private Mark(M3IndexedListSnapshot<E> captured) { this.captured = captured; }
        public int size() { return captured.size(); }
        public M3ObjectStream<E> reprocess() { return fromSnapshot(captured); }
        public M3IndexedListSnapshot<E> indexedSnapshot() { return captured; }
    }

    /** Explicit bounded materialization; the (max+1)th value is refused before being appended. */
    public List<T> toBoundedList(int maxElements) {
        requireNonNegative(maxElements, "maxElements");
        try (Stream<T> input = stream()) {
            Iterator<T> iterator = input.iterator();
            ArrayList<T> values = new ArrayList<>(Math.min(maxElements, 1024));
            while (iterator.hasNext()) {
                if (values.size() >= maxElements) throw new IllegalStateException("materialization budget exceeded");
                values.add(iterator.next());
            }
            return Collections.unmodifiableList(values);
        }
    }

    /** Return an independently owned M3 Object[]/int[] snapshot within an explicit source budget. */
    public M3IndexedListSnapshot<T> indexedSnapshot(int maxElements) {
        return M3IndexedListSnapshot.copyOf(toBoundedList(maxElements));
    }

    /** Predicate is evaluated once per selected source occurrence; native mode receives only byte flags. */
    public M3IndexedListSnapshot<T> indexedFiltered(
            Predicate<? super T> predicate, int maxElements, M3IndexMaskCompactor.Backend backend) {
        Objects.requireNonNull(predicate, "predicate");
        Objects.requireNonNull(backend, "backend");
        return indexedSnapshot(maxElements).filtered(predicate, backend);
    }

    public M3IndexedListSnapshot<T> indexedFiltered(Predicate<? super T> predicate, int maxElements) {
        return indexedFiltered(predicate, maxElements, M3IndexMaskCompactor.Backend.JAVA);
    }

    /** Read-only Collection facade; its traversal reopens a replayable plan. */
    public M3ObjectCollection<T> virtualCollection() {
        if (!replayable) throw new IllegalStateException("one-shot plan cannot be a virtual Collection; mark first");
        return new M3ObjectCollection<>(this);
    }

    /** Opt-in bounded key-affinity scheduler; equal keys share a sequential lane. */
    public <K> M3KeyedParallelismPlan<T, K> parallelByKey(Function<? super T, ? extends K> keyExtractor) {
        Objects.requireNonNull(keyExtractor, "keyExtractor");
        if (!replayable) throw new IllegalStateException("keyed replay plan requires a replayable source; mark first");
        return new M3KeyedParallelismPlan<>(this, keyExtractor);
    }

    /** Ordered reduction. It intentionally makes no parallel-associativity assumption. */
    public <A> A fold(Supplier<? extends A> initializer,
                      BiFunction<? super A, ? super T, ? extends A> folder) {
        Objects.requireNonNull(initializer, "initializer");
        Objects.requireNonNull(folder, "folder");
        try (Stream<T> input = stream()) {
            Iterator<T> iterator = input.iterator();
            A state = initializer.get();
            while (iterator.hasNext()) state = folder.apply(state, iterator.next());
            return state;
        }
    }

    public T reduce(T identity, BinaryOperator<T> reducer) {
        Objects.requireNonNull(reducer, "reducer");
        try (Stream<T> input = stream()) {
            Iterator<T> iterator = input.iterator();
            T state = identity;
            while (iterator.hasNext()) state = reducer.apply(state, iterator.next());
            return state;
        }
    }

    /** Collect with a fresh state for every evaluation; null states and results are allowed. */
    public <A, R> R collect(Supplier<? extends A> initializer,
                           BiConsumer<? super A, ? super T> accumulator,
                           Function<? super A, ? extends R> finisher) {
        Objects.requireNonNull(initializer, "initializer");
        Objects.requireNonNull(accumulator, "accumulator");
        Objects.requireNonNull(finisher, "finisher");
        try (Stream<T> input = stream()) {
            Iterator<T> iterator = input.iterator();
            A state = initializer.get();
            while (iterator.hasNext()) accumulator.accept(state, iterator.next());
            return finisher.apply(state);
        }
    }

    /** Single traversal with a client JDK Collector; no hidden parallel collector or reassociation. */
    public <A, R> R collect(Collector<? super T, A, R> collector) {
        Objects.requireNonNull(collector, "collector");
        A state = collector.supplier().get();
        BiConsumer<A, ? super T> accumulator = collector.accumulator();
        try (Stream<T> input = stream()) {
            Iterator<T> iterator = input.iterator();
            while (iterator.hasNext()) accumulator.accept(state, iterator.next());
        }
        return collector.finisher().apply(state);
    }

    /** Count by explicit traversal so peek/map side effects are not elided by a sized-stream optimization. */
    public long count() {
        try (Stream<T> input = stream()) {
            Iterator<T> iterator = input.iterator();
            long count = 0;
            while (iterator.hasNext()) { iterator.next(); count = Math.addExact(count, 1L); }
            return count;
        }
    }

    /** JDK-compatible first element terminal; a null first element is rejected by Optional semantics. */
    public Optional<T> findFirst() {
        try (Stream<T> input = stream()) {
            return input.findFirst();
        }
    }

    /** Sequential find-any terminal. The chosen element is intentionally not a stable API promise. */
    public Optional<T> findAny() {
        try (Stream<T> input = stream()) {
            return input.findAny();
        }
    }

    public boolean anyMatch(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate");
        try (Stream<T> input = stream()) {
            return input.anyMatch(predicate);
        }
    }

    public boolean allMatch(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate");
        try (Stream<T> input = stream()) {
            return input.allMatch(predicate);
        }
    }

    public boolean noneMatch(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate");
        try (Stream<T> input = stream()) {
            return input.noneMatch(predicate);
        }
    }

    public Optional<T> min(Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator, "comparator");
        try (Stream<T> input = stream()) {
            return input.min(comparator);
        }
    }

    public Optional<T> max(Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator, "comparator");
        try (Stream<T> input = stream()) {
            return input.max(comparator);
        }
    }

    public void forEach(Consumer<? super T> action) {
        Objects.requireNonNull(action, "action");
        try (Stream<T> input = stream()) {
            input.forEach(action);
        }
    }

    public void forEachOrdered(Consumer<? super T> action) {
        Objects.requireNonNull(action, "action");
        try (Stream<T> input = stream()) {
            input.forEachOrdered(action);
        }
    }

    /** Bounded array materialization; refuses when more than maxElements values are present. */
    public Object[] toArray(int maxElements) {
        return toBoundedList(maxElements).toArray();
    }

    /** Bounded typed array materialization using the JDK stream array-generator convention. */
    public <A> A[] toArray(IntFunction<A[]> generator, int maxElements) {
        Objects.requireNonNull(generator, "generator");
        List<T> values = toBoundedList(maxElements);
        return values.toArray(Objects.requireNonNull(
                generator.apply(values.size()), "array generator returned null"));
    }

    /** Encounter-order keyed aggregation with first-key order, null support and a hard distinct-key cap. */
    public <K, A, R> Map<K, R> groupBy(
            Function<? super T, ? extends K> keyExtractor, int maxKeys,
            Supplier<? extends A> initializer,
            BiConsumer<? super A, ? super T> accumulator,
            Function<? super A, ? extends R> finisher) {
        Objects.requireNonNull(keyExtractor, "keyExtractor");
        Objects.requireNonNull(initializer, "initializer");
        Objects.requireNonNull(accumulator, "accumulator");
        Objects.requireNonNull(finisher, "finisher");
        requireNonNegative(maxKeys, "maxKeys");
        LinkedHashMap<K, A> states = new LinkedHashMap<>();
        try (Stream<T> input = stream()) {
            Iterator<T> iterator = input.iterator();
            while (iterator.hasNext()) {
                T value = iterator.next();
                K key = keyExtractor.apply(value);
                if (!states.containsKey(key)) {
                    if (states.size() >= maxKeys) throw new IllegalStateException("group key budget exceeded");
                    states.put(key, initializer.get());
                }
                accumulator.accept(states.get(key), value);
            }
        }
        LinkedHashMap<K, R> output = new LinkedHashMap<>();
        states.forEach((key, state) -> output.put(key, finisher.apply(state)));
        return Collections.unmodifiableMap(output);
    }

    public <K> Map<K, Long> countByKey(Function<? super T, ? extends K> keyExtractor, int maxKeys) {
        return groupBy(keyExtractor, maxKeys, () -> new long[1],
                (state, ignored) -> state[0] = Math.addExact(state[0], 1L), state -> state[0]);
    }

    /** Compute two collectors in one ordered traversal; output is merged only after successful input closure. */
    public <A, B, R1, R2, R> R tee(
            Collector<? super T, A, R1> first,
            Collector<? super T, B, R2> second,
            BiFunction<? super R1, ? super R2, ? extends R> merger) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        Objects.requireNonNull(merger, "merger");
        A a = first.supplier().get();
        B b = second.supplier().get();
        BiConsumer<A, ? super T> accumulateA = first.accumulator();
        BiConsumer<B, ? super T> accumulateB = second.accumulator();
        try (Stream<T> input = stream()) {
            Iterator<T> iterator = input.iterator();
            while (iterator.hasNext()) {
                T value = iterator.next();
                accumulateA.accept(a, value);
                accumulateB.accept(b, value);
            }
        }
        R1 left = first.finisher().apply(a);
        R2 right = second.finisher().apply(b);
        return merger.apply(left, right);
    }

    public enum TailPolicy { INCLUDE, DROP }

    private static void requireWidth(int width) {
        if (width < 1 || width > MAX_STATEFUL_WIDTH) {
            throw new IllegalArgumentException("width must be in [1," + MAX_STATEFUL_WIDTH + "]");
        }
    }

    private static void requireNonNegative(int value, String name) {
        if (value < 0) throw new IllegalArgumentException(name + " must be nonnegative");
    }
}
