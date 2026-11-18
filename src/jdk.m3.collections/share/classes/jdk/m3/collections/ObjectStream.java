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
package jdk.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
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
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;
import java.util.stream.Streams;
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
 * {@link IndexedListSnapshot}; filtered/sorted projections use M3's integer index
 * lane and byte-mask selector. JNI receives no Java object references, predicates,
 * comparators, or mapped values.</p>
 *
 * @param <T> element type; null values are preserved unless an operator's own contract says otherwise
 */
public final class ObjectStream<T> {
    private static final int MAX_STATEFUL_WIDTH = 65_536;
    private final Supplier<? extends Stream<T>> opener;
    private final boolean replayable;

    private ObjectStream(Supplier<? extends Stream<T>> opener, boolean replayable) {
        this.opener = Objects.requireNonNull(opener, "opener");
        this.replayable = replayable;
    }

    /** Capture references and occurrence order once in M3's Object[]/int[] owner. */
    public static <T> ObjectStream<T> snapshot(Collection<? extends T> source) {
        return fromSnapshot(IndexedListSnapshot.copyOf(Objects.requireNonNull(source, "source")));
    }

    /** Snapshot an Iterable such as an Eclipse RichIterable without requiring a Collection API. */
    public static <T> ObjectStream<T> snapshot(Iterable<? extends T> source) {
        Objects.requireNonNull(source, "source");
        if (source instanceof Collection<?> collection) {
            @SuppressWarnings("unchecked") Collection<? extends T> typed = (Collection<? extends T>) collection;
            return snapshot(typed);
        }
        ArrayList<T> values = new ArrayList<>();
        for (T value : source) values.add(value);
        return snapshot(values);
    }

    private static <T> ObjectStream<T> fromSnapshot(IndexedListSnapshot<T> snapshot) {
        return new ObjectStream<>(snapshot::stream, true);
    }

    /** Compose two plans in encounter order without materializing either source. */
    public static <T> ObjectStream<T> concat(
            ObjectStream<? extends T> first, ObjectStream<? extends T> second) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        return new ObjectStream<>(
                () -> Stream.concat(first.stream(), second.stream()),
                first.replayable && second.replayable);
    }

    /**
     * A replayable, live window over one caller-owned Object[] array. Logical
     * offsets are checked once; no slice copy or per-element wrapper is created.
     * Array edits remain visible on future traversals. The caller owns mutation.
     */
    public static <T> ObjectStream<T> arrayWindow(T[] values, int offset, int length) {
        Objects.requireNonNull(values, "values");
        Objects.checkFromIndexSize(offset, length, values.length);
        int end = offset + length;
        return new ObjectStream<>(
                () -> StreamSupport.stream(Arrays.spliterator(values, offset, end), false), true);
    }

    /** Traverse a live collection on every evaluation; the source owns concurrent-modification semantics. */
    public static <T> ObjectStream<T> live(Collection<T> source) {
        Objects.requireNonNull(source, "source");
        return new ObjectStream<>(() -> source.stream().sequential(), true);
    }

    /** Own exactly one existing JDK stream. It is consumed on first open. */
    public static <T> ObjectStream<T> once(Stream<T> source) {
        AtomicReference<Stream<T>> pending = new AtomicReference<>(Objects.requireNonNull(source, "source"));
        return new ObjectStream<>(() -> {
            Stream<T> next = pending.getAndSet(null);
            if (next == null) throw new IllegalStateException("one-shot stream already consumed; mark before reuse");
            return next.sequential();
        }, false);
    }

    public boolean replayable() { return replayable; }

    /** Indexed mapping without a boxed Long or retained per-entry index object. */
    @FunctionalInterface
    public interface IndexedMapper<E, R> {
        R apply(E element, long index);
    }

    /**
     * Lazy one-pass zip, stopping at the shorter input. No Tuple/Entry object
     * is allocated by the operator. The result closes both sources, including
     * on terminal short-circuit; a failed second open closes the first.
     */
    public static <A, B, R> ObjectStream<R> zip(
            ObjectStream<? extends A> left, ObjectStream<? extends B> right,
            BiFunction<? super A, ? super B, ? extends R> zipper) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        Objects.requireNonNull(zipper, "zipper");
        return new ObjectStream<>(() -> {
            Stream<? extends A> sourceA = left.stream();
            try {
                Stream<? extends B> sourceB = right.stream();
                try {
                    Iterator<? extends A> a = sourceA.iterator();
                    Iterator<? extends B> b = sourceB.iterator();
                    Spliterator<R> pairs = new Spliterators.AbstractSpliterator<>(
                            Long.MAX_VALUE, Spliterator.ORDERED) {
                        @Override public boolean tryAdvance(Consumer<? super R> action) {
                            Objects.requireNonNull(action, "action");
                            if (!a.hasNext() || !b.hasNext()) return false;
                            action.accept(zipper.apply(a.next(), b.next()));
                            return true;
                        }
                    };
                    return StreamSupport.stream(pairs, false).onClose(() -> {
                        try (sourceA; sourceB) { /* release both sources */ }
                    });
                } catch (RuntimeException | Error failure) {
                    try { sourceB.close(); }
                    catch (RuntimeException | Error closing) { failure.addSuppressed(closing); }
                    throw failure;
                }
            } catch (RuntimeException | Error failure) {
                try { sourceA.close(); }
                catch (RuntimeException | Error closing) { failure.addSuppressed(closing); }
                throw failure;
            }
        }, left.replayable && right.replayable);
    }

    /** A fresh sequential traversal. The caller must close this stream, including after short-circuiting. */
    public Stream<T> stream() {
        return Objects.requireNonNull(opener.get(), "stream factory returned null").sequential();
    }

    private ObjectStream<T> stage(Function<Stream<T>, Stream<T>> operation) {
        Objects.requireNonNull(operation, "operation");
        return new ObjectStream<>(() -> operation.apply(stream()), replayable);
    }

    public ObjectStream<T> filter(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate");
        return stage(s -> s.filter(predicate));
    }

    public <R> ObjectStream<R> map(Function<? super T, ? extends R> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        return new ObjectStream<>(() -> stream().map(mapper), replayable);
    }

    /**
     * Per-traversal primitive encounter index, resetting at each replay.
     * Unlike tuple-based zipWithIndex, the index is delivered as primitive long.
     */
    public <R> ObjectStream<R> mapIndexed(IndexedMapper<? super T, ? extends R> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        return new ObjectStream<>(() -> {
            long[] index = new long[1];
            return stream().map(value -> {
                long ordinal = index[0];
                index[0] = Math.addExact(ordinal, 1L);
                return mapper.apply(value, ordinal);
            });
        }, replayable);
    }

    /** Map one input to a finite Iterable without first allocating an intermediate result collection. */
    public <R> ObjectStream<R> flatMapIterable(
            Function<? super T, ? extends Iterable<? extends R>> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        return new ObjectStream<>(() -> stream().flatMap(item -> {
            Iterable<? extends R> values = Objects.requireNonNull(mapper.apply(item), "mapped iterable");
            return StreamSupport.stream(values.spliterator(), false);
        }), replayable);
    }

    public <R> ObjectStream<R> flatMap(Function<? super T, ? extends Stream<? extends R>> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        return new ObjectStream<>(() -> stream().flatMap(item ->
                Objects.requireNonNull(mapper.apply(item), "mapped stream")), replayable);
    }

    /** Emits zero or more mapped outputs without requiring a temporary collection. */
    public <R> ObjectStream<R> mapMulti(
            BiConsumer<? super T, ? super Consumer<R>> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        return new ObjectStream<>(() -> stream().<R>mapMulti(mapper), replayable);
    }

    /**
     * Explicit ordinal-first mapping without tuple or Long boxing.
     * The earlier mapIndexed(value, ordinal) API retains its published order.
     */
    @FunctionalInterface
    public interface OrdinalFirstMapper<I, O> {
        O apply(long index, I value);
    }

    /** Lazy zero-based ordinal-first mapping; index overflow is rejected. */
    public <R> ObjectStream<R> mapIndexedOrdinalFirst(
            OrdinalFirstMapper<? super T, ? extends R> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        return new ObjectStream<>(
                () -> ObjectStreamLanguageKernels.mapIndexedOrdinalFirst(stream(), mapper),
                replayable);
    }

    /** Kotlin zipWithNext / Python pairwise shape; no intermediate pair objects. */
    public <R> ObjectStream<R> zipWithNext(
            BiFunction<? super T, ? super T, ? extends R> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        return new ObjectStream<>(() -> ObjectStreamLanguageKernels.zipWithNext(stream(), mapper),
                replayable);
    }

    /** Prefix reduction seeded by the first element, unlike scan(initializer,folder). */
    public ObjectStream<T> runningReduce(BinaryOperator<T> reducer) {
        Objects.requireNonNull(reducer, "reducer");
        return new ObjectStream<>(() -> ObjectStreamLanguageKernels.runningReduce(stream(), reducer),
                replayable);
    }

    /** Contiguous, immutable key runs with explicit per-run retention cap. */
    public <K> ObjectStream<List<T>> chunkBy(
            Function<? super T, ? extends K> keyExtractor, int maxRunLength) {
        Objects.requireNonNull(keyExtractor, "keyExtractor");
        requireWidth(maxRunLength);
        return new ObjectStream<>(
                () -> ObjectStreamLanguageKernels.chunkBy(stream(), keyExtractor, maxRunLength),
                replayable);
    }

    /** Suppress only adjacent duplicate keys using constant additional state. */
    public <K> ObjectStream<T> distinctUntilChanged(
            Function<? super T, ? extends K> keyExtractor) {
        Objects.requireNonNull(keyExtractor, "keyExtractor");
        return new ObjectStream<>(
                () -> ObjectStreamLanguageKernels.distinctUntilChanged(stream(), keyExtractor),
                replayable);
    }

    /** Insert a separator between values, never before the first or after the last. */
    public ObjectStream<T> intersperse(T separator) {
        return new ObjectStream<>(
                () -> ObjectStreamLanguageKernels.intersperse(stream(), separator), replayable);
    }

    /** Lazy two-input zip; truncates at the shorter side and closes both on evaluation. */
    public <U, R> ObjectStream<R> zip(
            ObjectStream<? extends U> other,
            BiFunction<? super T, ? super U, ? extends R> mapper) {
        Objects.requireNonNull(other, "other");
        Objects.requireNonNull(mapper, "mapper");
        return new ObjectStream<>(() -> {
            Stream<T> first = stream();
            Stream<? extends U> second = null;
            try {
                second = other.stream();
                return ObjectStreamLanguageKernels.zip(first, second, mapper);
            } catch (RuntimeException | Error failure) {
                try { first.close(); }
                catch (RuntimeException | Error closing) { failure.addSuppressed(closing); }
                if (second != null) {
                    try { second.close(); }
                    catch (RuntimeException | Error closing) { failure.addSuppressed(closing); }
                }
                throw failure;
            }
        }, replayable && other.replayable());
    }

    /**
     * Lazily map overlapping adjacent pairs using the java.base stream owner.
     * Empty and singleton inputs produce no results. Each replay opens the
     * original source afresh; the mapped traversal owns source closure.
     */
    public <R> ObjectStream<R> pairwise(
            BiFunction<? super T, ? super T, ? extends R> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        return delegateStream(input -> Streams.pairwise(input, mapper));
    }

    /**
     * Lazily map until the first empty optional, consuming that terminating
     * input but requesting no later elements. A null mapped optional is
     * rejected. Each evaluation has independent termination state.
     */
    public <R> ObjectStream<R> mapWhile(
            Function<? super T, ? extends Optional<? extends R>> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        return delegateStream(input -> Streams.mapWhile(input, mapper));
    }

    /** Transfer this plan's opened traversal to a java.base stream factory. */
    private <R> ObjectStream<R> delegateStream(Function<Stream<T>, Stream<R>> factory) {
        return new ObjectStream<>(() -> {
            Stream<T> input = stream();
            try {
                return factory.apply(input);
            } catch (RuntimeException | Error failure) {
                // A failed factory leaves ownership with its caller, which is this plan.
                try {
                    input.close();
                } catch (RuntimeException | Error closeFailure) {
                    if (closeFailure != failure) failure.addSuppressed(closeFailure);
                }
                throw failure;
            }
        }, replayable);
    }

    /**
     * Distinguish a genuinely missing zip-longest side from a present null value,
     * without allocating Optional or tuple wrappers for every occurrence.
     */
    @FunctionalInterface
    public interface ZipLongestMapper<L, R, O> {
        O apply(boolean leftPresent, L left, boolean rightPresent, R right);
    }

    /** Kotlin mapNotNull / Rust filter_map: map once, retain non-null outputs. */
    public <R> ObjectStream<R> mapNotNull(Function<? super T, ? extends R> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        return new ObjectStream<>(
                () -> stream().<R>mapMulti((value, emit) -> {
                    R mapped = mapper.apply(value);
                    if (mapped != null) emit.accept(mapped);
                }), replayable);
    }

    /** Map into an Optional and emit present results without building a temporary List. */
    public <R> ObjectStream<R> filterMap(
            Function<? super T, ? extends Optional<? extends R>> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        return new ObjectStream<>(() -> stream().<R>mapMulti((item, emit) -> {
            Optional<? extends R> result = Objects.requireNonNull(
                    mapper.apply(item), "mapper returned null Optional");
            result.ifPresent(emit);
        }), replayable);
    }

    /** Python zip_longest / Rust itertools zip_longest, with explicit presence bits. */
    public <U, R> ObjectStream<R> zipLongest(ObjectStream<? extends U> other,
            ZipLongestMapper<? super T, ? super U, ? extends R> mapper) {
        Objects.requireNonNull(other, "other");
        Objects.requireNonNull(mapper, "mapper");
        return new ObjectStream<>(() -> {
            Stream<T> first = stream();
            Stream<? extends U> second = null;
            try {
                second = other.stream();
                return ObjectStreamAdvancedKernels.zipLongest(first, second, mapper);
            } catch (RuntimeException | Error failure) {
                try { first.close(); }
                catch (RuntimeException | Error closing) { failure.addSuppressed(closing); }
                if (second != null) {
                    try { second.close(); }
                    catch (RuntimeException | Error closing) { failure.addSuppressed(closing); }
                }
                throw failure;
            }
        }, replayable && other.replayable());
    }

    /** Rust itertools interleave: alternate while possible, then drain the longer side. */
    public ObjectStream<T> interleave(ObjectStream<? extends T> other) {
        Objects.requireNonNull(other, "other");
        return new ObjectStream<>(() -> {
            Stream<T> first = stream();
            Stream<? extends T> second = null;
            try {
                second = other.stream();
                return ObjectStreamAdvancedKernels.interleave(first, second);
            } catch (RuntimeException | Error failure) {
                try { first.close(); }
                catch (RuntimeException | Error closing) { failure.addSuppressed(closing); }
                if (second != null) {
                    try { second.close(); }
                    catch (RuntimeException | Error closing) { failure.addSuppressed(closing); }
                }
                throw failure;
            }
        }, replayable && other.replayable());
    }

    /**
     * Java 24 Gatherers.windowSliding-compatible short-source handling on Java 21:
     * an input shorter than width yields one immutable partial window.
     * Unlike window(width), normal inputs still emit full sliding windows only.
     */
    public ObjectStream<List<T>> windowSlidingPartial(int width) {
        requireWidth(width);
        return new ObjectStream<>(
                () -> ObjectStreamAdvancedKernels.windowSlidingPartial(stream(), width), replayable);
    }

    /** Inclusive take-until: emit the first boundary match, then stop traversal. */
    public ObjectStream<T> takeUntilInclusive(Predicate<? super T> boundary) {
        Objects.requireNonNull(boundary, "boundary");
        return new ObjectStream<>(
                () -> ObjectStreamAdvancedKernels.takeUntilInclusive(stream(), boundary),
                replayable);
    }

    /**
     * Select the greatest k values in descending comparator order, preserving
     * source order among equal values. Uses bounded object/ordinal heap arrays.
     */
    public ObjectStream<T> topKBy(int k, Comparator<? super T> comparator) {
        requireNonNegative(k, "k");
        if (k > MAX_STATEFUL_WIDTH) {
            throw new IllegalArgumentException("topK state exceeds " + MAX_STATEFUL_WIDTH);
        }
        Objects.requireNonNull(comparator, "comparator");
        return new ObjectStream<>(
                () -> ObjectStreamAdvancedKernels.topK(stream(), k, comparator), replayable);
    }

    /** Indexed running fold with seed emitted first, including for an empty source. */
    @FunctionalInterface
    public interface IndexedFolder<A, I> {
        A apply(long index, A state, I value);
    }

    /**
     * Complete partition result with one read-only output list for each branch.
     * Each element is transformed once for only its selected branch.
     */
    public record Partition<L, R>(List<L> left, List<R> right) { }

    /** Null-sentinel alternative to the existing Optional-based mapWhile API. */
    public <R> ObjectStream<R> mapWhileNullable(Function<? super T, ? extends R> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        return new ObjectStream<>(
                () -> ObjectStreamLoopKernels.mapWhile(stream(), mapper), replayable);
    }

    /** Rust step_by / Python islice step: select indices 0, stride, 2*stride, ... */
    public ObjectStream<T> stepBy(int stride) {
        if (stride < 1) throw new IllegalArgumentException("stride must be positive");
        return new ObjectStream<>(() -> ObjectStreamLoopKernels.stepBy(stream(), stride), replayable);
    }

    /**
     * Python heapq.merge style: merge two already-sorted inputs lazily.
     * On comparator ties, occurrences from this source precede the other source.
     */
    public ObjectStream<T> mergeSorted(ObjectStream<? extends T> other,
            Comparator<? super T> comparator) {
        Objects.requireNonNull(other, "other");
        Objects.requireNonNull(comparator, "comparator");
        return new ObjectStream<>(() -> {
            Stream<T> first = stream();
            Stream<? extends T> second = null;
            try {
                second = other.stream();
                return ObjectStreamLoopKernels.mergeSorted(first, second, comparator);
            } catch (RuntimeException | Error failure) {
                try { first.close(); }
                catch (RuntimeException | Error closing) { failure.addSuppressed(closing); }
                if (second != null) {
                    try { second.close(); }
                    catch (RuntimeException | Error closing) { failure.addSuppressed(closing); }
                }
                throw failure;
            }
        }, replayable && other.replayable());
    }

    /** Drop the final count values using a bounded lookahead ring, including nulls. */
    public ObjectStream<T> dropLast(int count) {
        requireNonNegative(count, "count");
        if (count > MAX_STATEFUL_WIDTH) {
            throw new IllegalArgumentException("dropLast count exceeds " + MAX_STATEFUL_WIDTH);
        }
        return new ObjectStream<>(() -> ObjectStreamLoopKernels.dropLast(stream(), count),
                replayable);
    }

    /** Drain finite input and emit only its bounded trailing count values. */
    public ObjectStream<T> takeLast(int count) {
        requireNonNegative(count, "count");
        if (count > MAX_STATEFUL_WIDTH) {
            throw new IllegalArgumentException("takeLast count exceeds " + MAX_STATEFUL_WIDTH);
        }
        return new ObjectStream<>(() -> ObjectStreamLoopKernels.takeLast(stream(), count),
                replayable);
    }

    /** Kotlin runningFoldIndexed: seed is the first emitted value, even on empty input. */
    public <A> ObjectStream<A> runningFoldIndexed(Supplier<? extends A> initializer,
            IndexedFolder<A, T> folder) {
        Objects.requireNonNull(initializer, "initializer");
        Objects.requireNonNull(folder, "folder");
        return new ObjectStream<>(() -> ObjectStreamLoopKernels.runningFoldIndexed(
                stream(), initializer, folder), replayable);
    }

    /**
     * Rust itertools coalesce shape: merge compatible neighboring values in place.
     * Compatibility checks the current accumulator against the next source value.
     */
    public ObjectStream<T> coalesceAdjacent(BiPredicate<? super T, ? super T> compatible,
            BinaryOperator<T> merger) {
        Objects.requireNonNull(compatible, "compatible");
        Objects.requireNonNull(merger, "merger");
        return new ObjectStream<>(() -> ObjectStreamLoopKernels.coalesceAdjacent(
                stream(), compatible, merger), replayable);
    }

    /**
     * Rust partition_map idiom without one Either object per occurrence.
     * The predicate chooses the left result on true and right on false.
     */
    public <L, R> Partition<L, R> partitionMap(
            Predicate<? super T> toLeft, Function<? super T, ? extends L> leftMapper,
            Function<? super T, ? extends R> rightMapper, int maxElements) {
        Objects.requireNonNull(toLeft, "toLeft");
        Objects.requireNonNull(leftMapper, "leftMapper");
        Objects.requireNonNull(rightMapper, "rightMapper");
        requireNonNegative(maxElements, "maxElements");
        ArrayList<L> left = new ArrayList<>();
        ArrayList<R> right = new ArrayList<>();
        try (Stream<T> input = stream()) {
            Iterator<T> source = input.iterator();
            int admitted = 0;
            while (source.hasNext()) {
                if (admitted >= maxElements) {
                    throw new IllegalStateException("partitionMap element budget exceeded");
                }
                T item = source.next();
                admitted++;
                if (toLeft.test(item)) left.add(leftMapper.apply(item));
                else right.add(rightMapper.apply(item));
            }
        }
        return new Partition<>(Collections.unmodifiableList(left),
                Collections.unmodifiableList(right));
    }

    public ObjectStream<T> peek(Consumer<? super T> observer) {
        Objects.requireNonNull(observer, "observer");
        return stage(s -> s.peek(observer));
    }

    public ObjectStream<T> skip(long count) {
        if (count < 0) throw new IllegalArgumentException("skip must be nonnegative");
        return stage(s -> s.skip(count));
    }

    public ObjectStream<T> limit(long count) {
        if (count < 0) throw new IllegalArgumentException("limit must be nonnegative");
        return stage(s -> s.limit(count));
    }

    public ObjectStream<T> takeWhile(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate");
        return stage(s -> s.takeWhile(predicate));
    }

    public ObjectStream<T> dropWhile(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate");
        return stage(s -> s.dropWhile(predicate));
    }

    /**
     * Equivalent to a streaming zipWithNext mapper, but reads the source once.
     * Stores one previous element and never allocates an intermediate window.
     */
    public <R> ObjectStream<R> mapAdjacent(
            BiFunction<? super T, ? super T, ? extends R> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        return new ObjectStream<>(() -> {
            Stream<T> upstream = stream();
            Iterator<T> input = upstream.iterator();
            Spliterator<R> pairs = new Spliterators.AbstractSpliterator<>(
                    Long.MAX_VALUE, Spliterator.ORDERED) {
                private boolean initialized;
                private T previous;
                @Override public boolean tryAdvance(Consumer<? super R> action) {
                    Objects.requireNonNull(action, "action");
                    if (!initialized) {
                        if (!input.hasNext()) return false;
                        previous = input.next();
                        initialized = true;
                    }
                    if (!input.hasNext()) return false;
                    T next = input.next();
                    R mapped = mapper.apply(previous, next);
                    previous = next;
                    action.accept(mapped);
                    return true;
                }
            };
            return StreamSupport.stream(pairs, false).onClose(upstream::close);
        }, replayable);
    }

    /**
     * Drop only adjacent equal occurrences; unlike distinct(maxDistinct),
     * this needs O(1) key state rather than a growing hash table.
     */
    public ObjectStream<T> distinctAdjacent() {
        return new ObjectStream<>(() -> {
            Object[] last = new Object[1];
            boolean[] seen = new boolean[1];
            return stream().filter(value -> {
                if (seen[0] && Objects.equals(last[0], value)) return false;
                last[0] = value;
                seen[0] = true;
                return true;
            });
        }, replayable);
    }

    /** Stable first-occurrence distinctness with a hard key-state cap. */
    public ObjectStream<T> distinct(int maxDistinct) {
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

    /** Stable first-key distinctness with a hard bound on unique extracted keys. */
    public <K> ObjectStream<T> distinctBy(
            Function<? super T, ? extends K> keyExtractor, int maxDistinct) {
        Objects.requireNonNull(keyExtractor, "keyExtractor");
        requireNonNegative(maxDistinct, "maxDistinct");
        return stage(s -> {
            Set<K> seen = new HashSet<>();
            return s.filter(value -> {
                K key = keyExtractor.apply(value);
                if (seen.contains(key)) return false;
                if (seen.size() >= maxDistinct) {
                    throw new IllegalStateException("distinct key budget exceeded");
                }
                seen.add(key);
                return true;
            });
        });
    }

    /** Stable bounded sort through M3's payload-array/index-directory owner, not per-element nodes. */
    public ObjectStream<T> sorted(Comparator<? super T> comparator, int maxElements) {
        Objects.requireNonNull(comparator, "comparator");
        requireNonNegative(maxElements, "maxElements");
        return new ObjectStream<>(() -> {
            List<T> values = toBoundedList(maxElements);
            return IndexedListSnapshot.copyOf(values).sorted(comparator).stream();
        }, replayable);
    }

    /** Stable natural-order sort, subject to the elements' Comparable contract. */
    public ObjectStream<T> sorted(int maxElements) {
        requireNonNegative(maxElements, "maxElements");
        return new ObjectStream<>(() -> {
            List<T> values = toBoundedList(maxElements);
            return IndexedListSnapshot.copyOf(values).sorted().stream();
        }, replayable);
    }

    /** Non-overlapping immutable batches. Tail policy is applied only to the final incomplete batch. */
    public ObjectStream<List<T>> batch(int size, TailPolicy tailPolicy) {
        requireWidth(size);
        Objects.requireNonNull(tailPolicy, "tailPolicy");
        return new ObjectStream<>(() -> {
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

    public ObjectStream<List<T>> batch(int size) { return batch(size, TailPolicy.INCLUDE); }

    /** Full sliding windows only; ring scratch is O(width) and each emitted window is an immutable copy. */
    public ObjectStream<List<T>> window(int width) {
        requireWidth(width);
        return new ObjectStream<>(() -> {
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
    public <A> ObjectStream<A> scan(
            Supplier<? extends A> initializer,
            BiFunction<? super A, ? super T, ? extends A> folder) {
        Objects.requireNonNull(initializer, "initializer");
        Objects.requireNonNull(folder, "folder");
        return new ObjectStream<>(() -> {
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
        return new Mark<>(IndexedListSnapshot.copyOf(toBoundedList(maxElements)));
    }

    public static final class Mark<E> {
        private final IndexedListSnapshot<E> captured;
        private Mark(IndexedListSnapshot<E> captured) { this.captured = captured; }
        public int size() { return captured.size(); }
        public ObjectStream<E> reprocess() { return fromSnapshot(captured); }
        public IndexedListSnapshot<E> indexedSnapshot() { return captured; }
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
    public IndexedListSnapshot<T> indexedSnapshot(int maxElements) {
        return IndexedListSnapshot.copyOf(toBoundedList(maxElements));
    }

    /** Predicate is evaluated once per selected source occurrence; native mode receives only byte flags. */
    public IndexedListSnapshot<T> indexedFiltered(
            Predicate<? super T> predicate, int maxElements, IndexMaskCompactor.Backend backend) {
        Objects.requireNonNull(predicate, "predicate");
        Objects.requireNonNull(backend, "backend");
        return indexedSnapshot(maxElements).filtered(predicate, backend);
    }

    public IndexedListSnapshot<T> indexedFiltered(Predicate<? super T> predicate, int maxElements) {
        return indexedFiltered(predicate, maxElements, IndexMaskCompactor.Backend.JAVA);
    }

    /** Read-only Collection facade; its traversal reopens a replayable plan. */
    public ObjectCollection<T> virtualCollection() {
        if (!replayable) throw new IllegalStateException("one-shot plan cannot be a virtual Collection; mark first");
        return new ObjectCollection<>(this);
    }

    /** Opt-in bounded key-affinity scheduler; equal keys share a sequential lane. */
    public <K> KeyedParallelismPlan<T, K> parallelByKey(Function<? super T, ? extends K> keyExtractor) {
        Objects.requireNonNull(keyExtractor, "keyExtractor");
        if (!replayable) throw new IllegalStateException("keyed replay plan requires a replayable source; mark first");
        return new KeyedParallelismPlan<>(this, keyExtractor);
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

    /** One-pass stable partition with a hard bound on the total number of retained elements. */
    public Map<Boolean, List<T>> partitionBy(
            Predicate<? super T> predicate, int maxElements) {
        Objects.requireNonNull(predicate, "predicate");
        requireNonNegative(maxElements, "maxElements");
        List<T> rejected = new ArrayList<>();
        List<T> accepted = new ArrayList<>();
        int seen = 0;
        try (Stream<T> input = stream()) {
            Iterator<T> iterator = input.iterator();
            while (iterator.hasNext()) {
                if (seen >= maxElements) {
                    throw new IllegalStateException("partition element budget exceeded");
                }
                T value = iterator.next();
                seen++;
                if (predicate.test(value)) accepted.add(value);
                else rejected.add(value);
            }
        }
        LinkedHashMap<Boolean, List<T>> result = new LinkedHashMap<>();
        result.put(Boolean.FALSE, Collections.unmodifiableList(rejected));
        result.put(Boolean.TRUE, Collections.unmodifiableList(accepted));
        return Collections.unmodifiableMap(result);
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

    /**
     * Sequentially replace one accumulator state per key in first-key encounter
     * order. The initializer runs once for each admitted key, including a null
     * key. Null initial and replacement states are valid and never reseeded.
     * The key extractor and folder run once per successfully processed element;
     * no parallel associativity or mutable-state holder is required.
     *
     * <p>A new key beyond maxKeys is refused before its initializer or folder
     * runs. The returned map is unmodifiable and is published only after the
     * input closes successfully. Keys and state objects retain their original
     * references; the result does not deep-copy client accumulator states.</p>
     */
    public <K, A> Map<K, A> foldByKey(
            Function<? super T, ? extends K> keyExtractor, int maxKeys,
            Supplier<? extends A> initializer,
            BiFunction<? super A, ? super T, ? extends A> folder) {
        Objects.requireNonNull(keyExtractor, "keyExtractor");
        Objects.requireNonNull(initializer, "initializer");
        Objects.requireNonNull(folder, "folder");
        requireNonNegative(maxKeys, "maxKeys");
        LinkedHashMap<K, A> states = new LinkedHashMap<>();
        try (Stream<T> input = stream()) {
            Iterator<T> iterator = input.iterator();
            while (iterator.hasNext()) {
                T value = iterator.next();
                K key = keyExtractor.apply(value);
                if (!states.containsKey(key)) {
                    if (states.size() >= maxKeys) {
                        throw new IllegalStateException("keyed state budget exceeded");
                    }
                    states.put(key, initializer.get());
                }
                states.put(key, folder.apply(states.get(key), value));
            }
        }
        return Collections.unmodifiableMap(states);
    }

    /**
     * Scala-style groupMapReduce, with first-key iteration order and a hard
     * distinct-key cap. Unlike groupBy-to-list, retains only one value per key.
     * Null keys and nullable accumulator values remain distinguishable.
     */
    public <K, V> Map<K, V> groupMapReduce(
            Function<? super T, ? extends K> keyExtractor, int maxKeys,
            Function<? super T, ? extends V> valueMapper, BinaryOperator<V> reducer) {
        Objects.requireNonNull(keyExtractor, "keyExtractor");
        Objects.requireNonNull(valueMapper, "valueMapper");
        Objects.requireNonNull(reducer, "reducer");
        requireNonNegative(maxKeys, "maxKeys");
        LinkedHashMap<K, V> states = new LinkedHashMap<>();
        try (Stream<T> input = stream()) {
            Iterator<T> source = input.iterator();
            while (source.hasNext()) {
                T item = source.next();
                K key = keyExtractor.apply(item);
                if (!states.containsKey(key)) {
                    if (states.size() >= maxKeys) {
                        throw new IllegalStateException("groupMapReduce key budget exceeded");
                    }
                    states.put(key, valueMapper.apply(item));
                } else {
                    states.put(key, reducer.apply(states.get(key), valueMapper.apply(item)));
                }
            }
        }
        return Collections.unmodifiableMap(states);
    }

    public <K> Map<K, Long> countByKey(Function<? super T, ? extends K> keyExtractor, int maxKeys) {
        return groupBy(keyExtractor, maxKeys, () -> new long[1],
                (state, ignored) -> state[0] = Math.addExact(state[0], 1L), state -> state[0]);
    }

    /**
     * Groovy 6 groupByMany shape, with M3's explicit source, key and membership
     * budgets. A value can occur under multiple keys; a repeated key in the
     * iterable contributes a repeated membership, preserving its side effects.
     * Null key iterables skip that input, as in Groovy's GDK implementation.
     *
     * <p>Group directories use M3's array-backed ordered map, and members use
     * M3's Object[]-backed list. There is no per-membership Entry/Pair graph.
     * First occurrence determines key order; each group's members preserve
     * source and key-iteration order. The result owns mutable groups and is
     * independent of the source. This is not a Groovy MetaClass/GDK patch.
     *
     * <p>The value mapper runs once per admitted <em>membership</em>, not once
     * per source element, following the GDK two-function form. Both callbacks
     * execute sequentially on the caller's thread. Limits are checked before
     * mutating the output. Any failure closes an opened source stream.
     */
    public <K, V> Map<K, List<V>> groupByMany(
            Function<? super T, ? extends V> valueMapper,
            Function<? super T, ? extends Iterable<? extends K>> keyMapper,
            int maxElements, int maxKeys, long maxMemberships) {
        Objects.requireNonNull(valueMapper, "valueMapper");
        Objects.requireNonNull(keyMapper, "keyMapper");
        requireNonNegative(maxElements, "maxElements");
        requireNonNegative(maxKeys, "maxKeys");
        if (maxMemberships < 0L) throw new IllegalArgumentException("negative maxMemberships");
        Map<K, List<V>> groups = new M3LinkedHashMap<>();
        int consumed = 0;
        long memberships = 0L;
        try (Stream<T> source = stream()) {
            Iterator<T> iterator = source.iterator();
            while (iterator.hasNext()) {
                if (consumed >= maxElements) {
                    throw new IllegalStateException("groupByMany source budget exceeded");
                }
                T element = iterator.next();
                consumed++;
                Iterable<? extends K> keys = keyMapper.apply(element);
                if (keys == null) continue;
                for (K key : keys) {
                    if (memberships >= maxMemberships) {
                        throw new IllegalStateException("groupByMany membership budget exceeded");
                    }
                    List<V> members = groups.get(key);
                    if (members == null) {
                        if (groups.size() >= maxKeys) {
                            throw new IllegalStateException("groupByMany key budget exceeded");
                        }
                        members = new M3ArrayList<>();
                        groups.put(key, members);
                    }
                    members.add(valueMapper.apply(element));
                    memberships++;
                }
            }
        }
        return groups;
    }

    /** Identity-preserving one-to-many grouping with no per-element tuple. */
    public <K> Map<K, List<T>> groupByMany(
            Function<? super T, ? extends Iterable<? extends K>> keyMapper,
            int maxElements, int maxKeys, long maxMemberships) {
        return groupByMany(Function.identity(), keyMapper, maxElements, maxKeys, maxMemberships);
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
