/*
 * Copyright (c) 2012, 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package java.util.stream;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.PrimitiveIterator;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.Function;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.LongBinaryOperator;
import java.util.function.LongConsumer;
import java.util.function.LongFunction;
import java.util.function.ext.DoubleLongToDoubleFunction;
import java.util.function.ext.IntLongToIntFunction;
import java.util.function.ext.ObjLongFunction;
import jdk.internal.vm.annotation.IntrinsicCandidate;

/**
 * Utility methods for operating on and creating streams.
 *
 * <p>Unless otherwise stated, streams are created as sequential streams.  A
 * sequential stream can be transformed into a parallel stream by calling the
 * {@code parallel()} method on the created stream.
 *
 * <p>This public utility family belongs to the same java.base stream
 * implementation as Stream and StreamSupport. Existing stream builders,
 * range spliterators, and close-composition helpers keep their internal roles.
 *
 * @since 21
 */
public final class Streams {

    private Streams() {
        throw new Error("no instances");
    }

    /**
     * Creates a sequential stream from an iterable's own spliterator.
     * The spliterator is obtained immediately; its binding and interference
     * policies, encounter order, and characteristics are preserved. Closing
     * the result does not close the iterable, even if it is closeable.
     *
     * @param source the iterable
     * @param <T> the element type
     * @return a sequential stream
     * @throws NullPointerException if {@code source} is null
     * @since 21
     */
    public static <T> Stream<T> stream(Iterable<T> source) {
        return stream(source, false);
    }

    /**
     * Creates a stream from the iterable's own spliterator and requested
     * execution mode. Splitting, late binding, fail-fast behavior, and
     * metadata come from that spliterator. Closing the result does not
     * close the iterable.
     *
     * @param source the iterable
     * @param parallel whether the stream is initially parallel
     * @param <T> the element type
     * @return a stream over the iterable
     * @throws NullPointerException if {@code source} is null
     * @since 21
     */
    public static <T> Stream<T> stream(Iterable<T> source, boolean parallel) {
        Objects.requireNonNull(source, "source");
        return StreamSupport.stream(source.spliterator(), parallel);
    }

    /**
     * Creates a sequential, ordered stream over the remaining elements of
     * an iterator. Encounter order follows iterator traversal. No element
     * is traversed during construction. Closing the result does not close
     * the iterator, even if it is closeable.
     *
     * @param source the iterator
     * @param <T> the element type
     * @return a sequential stream
     * @throws NullPointerException if {@code source} is null
     * @since 21
     */
    public static <T> Stream<T> stream(Iterator<? extends T> source) {
        return stream(source, false);
    }

    /**
     * Creates an ordered stream over the remaining iterator elements with
     * the requested execution mode. The iterator has an unknown size;
     * parallel traversal uses the batching policy of
     * {@link Spliterators#spliteratorUnknownSize(Iterator, int)}. Do not
     * use the iterator separately after creating the stream. Closing the
     * result does not close the iterator.
     *
     * @param source the iterator
     * @param parallel whether the stream is initially parallel
     * @param <T> the element type
     * @return a stream over the remaining iterator elements
     * @throws NullPointerException if {@code source} is null
     * @since 21
     */
    public static <T> Stream<T> stream(Iterator<? extends T> source, boolean parallel) {
        Objects.requireNonNull(source, "source");
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(source, Spliterator.ORDERED), parallel);
    }

    /**
     * Creates a sequential, ordered stream over the remaining elements of
     * an enumeration. Uses {@link Enumeration#asIterator()} and has the
     * same ownership and traversal rules as {@link #stream(Iterator)}.
     *
     * @param source the enumeration
     * @param <T> the element type
     * @return a sequential stream
     * @throws NullPointerException if {@code source} is null
     * @since 21
     */
    public static <T> Stream<T> stream(Enumeration<? extends T> source) {
        return stream(Objects.requireNonNull(source, "source").asIterator());
    }

    /**
     * Combines corresponding elements of two streams, stopping when either
     * input is exhausted. The result is initially sequential and its source
     * does not split, including after {@code parallel()} is called. It is
     * ordered only when both inputs are ordered. Size metadata is retained
     * only when both input sizes are known; sortedness, distinctness, and
     * non-null characteristics are not inferred for mapped results.
     *
     * <p>The input spliterators are acquired immediately, but elements are
     * pulled lazily. Testing whether a pair exists can consume one unpaired
     * element from the longer input. The supplied streams must be distinct,
     * open, and unused, and must not be used separately afterward.
     *
     * <p>Closing the returned stream closes the first input and then the
     * second, even if the first close fails. If both close operations fail,
     * the second exception is suppressed on the first. Exhaustion alone
     * does not close either input; use try-with-resources when appropriate.
     * If construction fails, resource ownership remains with the caller.
     * Exceptions from traversal or {@code zipper} are propagated unchanged.
     * As with other stream operations, traversal callbacks may be elided
     * when the result can be obtained from size metadata.
     *
     * @param first the first input stream
     * @param second the second input stream
     * @param zipper the non-interfering, stateless pair-combining function
     * @param <A> the first input element type
     * @param <B> the second input element type
     * @param <R> the result element type
     * @return the stream of paired results
     * @throws NullPointerException if an argument is null
     * @throws IllegalArgumentException if the two input stream references are identical
     * @throws IllegalStateException if an input stream has been operated on or closed
     * @since 21
     */
    public static <A, B, R> Stream<R> zip(Stream<? extends A> first,
                                         Stream<? extends B> second,
                                         BiFunction<? super A, ? super B, ? extends R> zipper) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        Objects.requireNonNull(zipper, "zipper");
        if (first == second) throw new IllegalArgumentException("distinct streams required");
        Spliterator<R> paired = new ZipSpliterator<>(
                first.spliterator(), second.spliterator(), zipper);
        return StreamSupport.stream(paired, false).onClose(composedClose(first, second));
    }

    /**
     * Maps overlapping adjacent pairs in source encounter order. A source with
     * fewer than two elements produces no result. The mapper receives the
     * earlier element first; elements and mapped results may be null.
     *
     * <p>The source is switched to sequential mode and its spliterator is
     * acquired immediately without traversing it.
     * Traversal retains only the previous element and does not allocate pair
     * containers. The result is initially sequential and its source does not
     * split, even after {@code parallel()}. It is ordered if the input is
     * ordered and does not advertise an exact size. Closing the result closes
     * the input; exhaustion alone does not close it. The input must not be used
     * separately after construction. Callback failures propagate unchanged.
     *
     * @param source the input stream
     * @param mapper the adjacent-element mapping function
     * @param <T> the input element type
     * @param <R> the result element type
     * @return a lazy stream of mapped adjacent pairs
     * @throws NullPointerException if an argument is null
     * @since 21
     */
    public static <T, R> Stream<R> pairwise(Stream<? extends T> source,
            BiFunction<? super T, ? super T, ? extends R> mapper) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(mapper, "mapper");
        Spliterator<? extends T> input = source.sequential().spliterator();
        return combinatorStream(source, new PairwiseSpliterator<>(input, mapper));
    }

    /**
     * Maps each element with its zero-based encounter index at this pipeline
     * stage. The index is a primitive {@code long}; no boxed index or indexed
     * tuple is required. Elements and mapped results may be null.
     *
     * <p>Construction switches the source to sequential mode and acquires its
     * spliterator without traversing it.
     * Traversal uses one counter and is unsplittable, including after
     * {@code parallel()}. The result is initially sequential, is ordered if the
     * input is ordered, and does not advertise an exact size. For an unordered
     * input, indices follow the order in which this traversal receives values.
     * Requesting an element beyond index {@link Long#MAX_VALUE} throws
     * {@code ArithmeticException} instead of wrapping. An existence check may
     * read ahead to that element, but its mapper is not invoked.
     *
     * <p>Closing the result closes the input; exhaustion alone does not. The
     * input must not be used separately afterward. Callback failures propagate
     * unchanged.
     *
     * @param source the input stream
     * @param mapper the function receiving an element and its primitive index
     * @param <T> the input element type
     * @param <R> the result element type
     * @return a lazy stream of indexed mapping results
     * @throws NullPointerException if an argument is null
     * @since 21
     */
    public static <T, R> Stream<R> mapIndexed(Stream<? extends T> source,
            ObjLongFunction<? super T, ? extends R> mapper) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(mapper, "mapper");
        Spliterator<? extends T> input = source.sequential().spliterator();
        return combinatorStream(source, new IndexedSpliterator<>(input, mapper));
    }

    /**
     * Inserts the supplied separator strictly between consecutive input
     * elements. Empty and singleton inputs are unchanged. Input elements and
     * the separator may be null, and the separator reference is reused.
     *
     * <p>Construction switches the source to sequential mode and acquires its
     * spliterator without traversing it.
     * Emitting a separator reads and retains at most one following element so
     * that no trailing separator is produced. Traversal is unsplittable, even
     * after {@code parallel()}. The result is initially sequential, is ordered
     * if the input is ordered, and does not advertise an exact size. Closing
     * the result closes the input; exhaustion alone does not. The input must
     * not be used separately afterward.
     *
     * @param source the input stream
     * @param separator the value to insert between elements
     * @param <T> the element type
     * @return a lazy interspersed stream
     * @throws NullPointerException if {@code source} is null
     * @since 21
     */
    public static <T> Stream<T> intersperse(Stream<? extends T> source, T separator) {
        Objects.requireNonNull(source, "source");
        Spliterator<? extends T> input = source.sequential().spliterator();
        return combinatorStream(source, new InterspersedSpliterator<>(input, separator));
    }

    /**
     * Maps each examined input element and emits only non-null mapping results.
     * A null input element is passed to the mapper normally; null is tested
     * only on the result. Non-null results such as false, zero, and empty
     * strings are retained. This is a lazy intermediate operation with the
     * same encounter-order and parallel-mode rules as the source stream.
     *
     * <p>This operation uses {@link Stream#mapMulti(java.util.function.BiConsumer)}
     * with zero or one output per input. Construction does not traverse the
     * source. The mapper is invoked once for each examined input, including
     * inputs whose result is discarded. Short-circuiting may leave inputs
     * unexamined; parallel traversal may examine additional inputs. Mapper
     * invocation order follows the normal stream execution rules.
     *
     * <p>The supplied source must be open and unused and must not be used
     * separately afterward. Closing the returned stream closes the source;
     * exhaustion alone does not close it. If argument validation fails,
     * ownership remains with the caller. Mapper and traversal exceptions follow
     * the source stream's normal exception propagation rules; this operation
     * does not catch or translate them. The mapper must be non-interfering
     * and stateless.
     *
     * @param source the input stream
     * @param mapper the function mapping each examined element
     * @param <T> the input element type
     * @param <R> the result element type
     * @return a stream of the non-null mapping results
     * @throws NullPointerException if an argument is null
     * @throws IllegalStateException if the source has been operated on or closed
     * @since 21
     */
    public static <T, R> Stream<R> mapNotNull(Stream<? extends T> source,
            Function<? super T, ? extends R> mapper) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(mapper, "mapper");
        return source.<R>mapMulti((value, downstream) -> {
            R result = mapper.apply(value);
            if (result != null) downstream.accept(result);
        });
    }

    /**
     * Maps input elements until the mapper first returns an empty optional.
     * The element producing that empty optional is consumed. Later elements
     * and mapper calls are not requested, including after repeated exhaustion
     * checks. A null optional is rejected; successful outputs are non-null.
     *
     * <p>Construction switches the source to sequential mode and acquires its
     * spliterator without traversing it.
     * Traversal retains constant state and is unsplittable, even after
     * {@code parallel()}. The result is initially sequential, is ordered if
     * the input is ordered, and does not advertise an exact size. Closing the
     * result closes the input; termination alone does not. The input must not
     * be used separately afterward. Callback failures propagate unchanged.
     *
     * @param source the input stream
     * @param mapper the mapping and termination function
     * @param <T> the input element type
     * @param <R> the result element type
     * @return a lazy stream of mapped values before the first empty optional
     * @throws NullPointerException if an argument or a mapped optional is null
     * @since 21
     */
    public static <T, R> Stream<R> mapWhile(Stream<? extends T> source,
            Function<? super T, ? extends Optional<? extends R>> mapper) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(mapper, "mapper");
        Spliterator<? extends T> input = source.sequential().spliterator();
        return combinatorStream(source, new MappingWhileSpliterator<>(input, mapper));
    }

    /**
     * Retains the first encountered element for each extracted key, keeping at
     * most {@code maxDistinct} distinct keys. Keys use the usual
     * {@link Object#equals(Object)} and {@link Object#hashCode()} contracts;
     * elements and keys may be null. A new key beyond the budget is rejected
     * before its element is emitted. Duplicate keys at the budget still pass
     * through selection without increasing retained state.
     *
     * <p>Construction validates arguments, switches the source to sequential
     * mode and acquires its spliterator without traversing it. The key
     * extractor is invoked once for every
     * examined element. Keys must remain stable while retained. State is
     * proportional to the number of distinct keys, not the input length.
     * An infinite repeated-key suffix may never produce another result.
     *
     * <p>The result is initially sequential and its source is unsplittable,
     * even after {@code parallel()}. It preserves input encounter order when
     * present, and does not claim exact size or element distinctness from key
     * distinctness. Closing the result closes the input; exhaustion alone
     * does not. The input must not be used separately afterward. Callback
     * failures propagate unchanged.
     *
     * @param source the input stream
     * @param keyExtractor the stable key function
     * @param maxDistinct the maximum retained key count, possibly zero
     * @param <T> the element type
     * @param <K> the key type
     * @return a lazy bounded distinct-by-key stream
     * @throws NullPointerException if {@code source} or {@code keyExtractor} is null
     * @throws IllegalArgumentException if {@code maxDistinct} is negative
     * @throws IllegalStateException if traversal exceeds the distinct-key budget
     * @since 21
     */
    public static <T, K> Stream<T> distinctBy(Stream<? extends T> source,
            Function<? super T, ? extends K> keyExtractor, int maxDistinct) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(keyExtractor, "keyExtractor");
        if (maxDistinct < 0) {
            throw new IllegalArgumentException("maxDistinct must be nonnegative");
        }
        Spliterator<? extends T> input = source.sequential().spliterator();
        return combinatorStream(source,
                new DistinctKeySpliterator<>(input, keyExtractor, maxDistinct));
    }

    /**
     * Lazily merges two streams already sorted in their traversal order under
     * the supplied comparator. Duplicate elements are retained. Equal values
     * from the first input precede equal values from the second; order within
     * each input is preserved. Null elements are permitted if the comparator
     * supports them. Sortedness is a caller precondition and is not checked.
     *
     * <p>Both inputs are switched to sequential mode and their spliterators
     * are acquired immediately without traversing them. Traversal holds at
     * most one element from each input and performs
     * linear comparison work in the number of emitted elements. The result is
     * initially sequential and its source does not split, even after
     * {@code parallel()}. Its spliterator reports ordered and sorted, returns
     * the supplied comparator, and does not advertise an exact size.
     *
     * <p>The inputs must be distinct, open and unused, and must not be used
     * separately afterward. Closing the result closes the first input and
     * then the second, even if the first close fails; a second failure is
     * suppressed on the first. Exhaustion alone does not close either input.
     * If construction fails, ownership remains with the caller. Comparator
     * and traversal failures propagate unchanged.
     *
     * @param first the first sorted input stream
     * @param second the second sorted input stream
     * @param comparator the ordering shared by both inputs
     * @param <T> the element type
     * @return a stable lazy merge
     * @throws NullPointerException if an argument is null
     * @throws IllegalArgumentException if both input references are identical
     * @since 21
     */
    public static <T> Stream<T> mergeSorted(Stream<? extends T> first,
            Stream<? extends T> second, Comparator<? super T> comparator) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        Objects.requireNonNull(comparator, "comparator");
        if (first == second) throw new IllegalArgumentException("distinct streams required");
        Spliterator<T> merged = new SortedMergeSpliterator<>(
                first.sequential().spliterator(), second.sequential().spliterator(), comparator);
        return StreamSupport.stream(merged, false).onClose(composedClose(first, second));
    }


    /**
     * Lazily zip two equally long streams. Unlike {@link #zip(Stream, Stream, BiFunction)},
     * a full traversal throws {@code IllegalArgumentException} if either input has
     * unmatched elements. A short-circuited traversal checks only the prefix it
     * demands and need not detect a length mismatch beyond that prefix.
     *
     * <p>Both input spliterators are acquired at construction but no elements
     * are traversed until requested. Each is forced to sequential mode; the
     * result cannot split even after a later {@code parallel()} request. No
     * element or mapper result is rejected merely for being null. Since
     * traversal must validate input lengths, the result does not claim SIZED,
     * even when both input sources claim an exact size.
     *
     * <p>Closing the returned stream closes both inputs, including when the
     * first close fails (the second failure is then suppressed). Exhaustion
     * alone does not close them. Inputs must be distinct, open and unused and
     * must not be operated on separately after this call. Input/callback
     * exceptions propagate unchanged.
     *
     * @param first the left input
     * @param second the right input
     * @param zipper the pair mapping function
     * @param <A> the left element type
     * @param <B> the right element type
     * @param <R> the result element type
     * @return a lazy strictly paired stream
     * @throws NullPointerException if an argument is null
     * @throws IllegalArgumentException if the streams are identical or traversal finds unequal lengths
     * @since 21
     */
    public static <A, B, R> Stream<R> zipStrict(Stream<? extends A> first,
            Stream<? extends B> second, BiFunction<? super A, ? super B, ? extends R> zipper) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        Objects.requireNonNull(zipper, "zipper");
        if (first == second) throw new IllegalArgumentException("distinct streams required");
        Spliterator<? extends A> left = first.sequential().spliterator();
        Spliterator<? extends B> right = second.sequential().spliterator();
        Spliterator<R> pairs = new Spliterator<>() {
            private final Iterator<? extends A> a = Spliterators.iterator(left);
            private final Iterator<? extends B> b = Spliterators.iterator(right);
            private boolean finished;

            @Override public boolean tryAdvance(Consumer<? super R> action) {
                Objects.requireNonNull(action, "action");
                if (finished) return false;
                boolean hasLeft = a.hasNext();
                boolean hasRight = b.hasNext();
                if (hasLeft != hasRight) {
                    finished = true;
                    throw new IllegalArgumentException("zipStrict input lengths differ");
                }
                if (!hasLeft) { finished = true; return false; }
                action.accept(zipper.apply(a.next(), b.next()));
                return true;
            }

            @Override public Spliterator<R> trySplit() { return null; }
            @Override public long estimateSize() { return finished ? 0 : Long.MAX_VALUE; }
            @Override public int characteristics() {
                return left.characteristics() & right.characteristics() & Spliterator.ORDERED;
            }
        };
        return StreamSupport.stream(pairs, false).onClose(composedClose(first, second));
    }

    /**
     * Group consecutive elements having equal extracted keys, not all
     * elements with the same key across the entire stream. Each emitted run
     * is an independent unmodifiable List, including null occurrences. Keys
     * are compared by {@link Objects#equals(Object, Object)}, and null keys
     * are permitted.
     *
     * <p>The operation is lazy, sequential and unsplittable, retaining at most
     * {@code maxRunLength} values plus one element of lookahead. A new group
     * may be discovered by reading one element beyond the emitted group;
     * its key extractor is not called again. A run exceeding its explicit
     * budget fails before the extra element is stored. The accepted width
     * is 1 to 65,536 inclusive, consistent with M3 bounded grouping.
     *
     * <p>The result is ordered only if its source has encounter order. Closing
     * the result closes its input; exhaustion alone does not. The source must
     * not be used separately after this call. Neither group identities nor
     * keys are retained in a global map.
     *
     * @param source the input stream
     * @param keyExtractor maps each value to its grouping key
     * @param maxRunLength maximum retained elements in one run
     * @param <T> the element type
     * @param <K> the key type
     * @return a lazy stream of adjacent immutable groups
     * @throws NullPointerException if an argument is null
     * @throws IllegalArgumentException if maxRunLength is outside 1..65,536
     * @throws IllegalStateException if an adjacent group exceeds maxRunLength
     * @since 21
     */
    public static <T, K> Stream<List<T>> groupAdjacent(Stream<? extends T> source,
            Function<? super T, ? extends K> keyExtractor, int maxRunLength) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(keyExtractor, "keyExtractor");
        if (maxRunLength < 1 || maxRunLength > 65_536) {
            throw new IllegalArgumentException("maxRunLength must be in [1,65536]");
        }
        Spliterator<? extends T> input = source.sequential().spliterator();
        Spliterator<List<T>> runs = new Spliterator<>() {
            private final Iterator<? extends T> values = Spliterators.iterator(input);
            private boolean finished;
            private boolean pending;
            private T pendingValue;
            private K pendingKey;

            @Override public boolean tryAdvance(Consumer<? super List<T>> action) {
                Objects.requireNonNull(action, "action");
                if (finished) return false;
                if (!pending && !values.hasNext()) { finished = true; return false; }
                T first;
                K key;
                if (pending) {
                    first = pendingValue;
                    key = pendingKey;
                    pending = false;
                    pendingValue = null;
                    pendingKey = null;
                } else {
                    first = values.next();
                    key = keyExtractor.apply(first);
                }
                ArrayList<T> run = new ArrayList<>();
                run.add(first);
                while (values.hasNext()) {
                    T next = values.next();
                    K nextKey = keyExtractor.apply(next);
                    if (!Objects.equals(key, nextKey)) {
                        pendingValue = next;
                        pendingKey = nextKey;
                        pending = true;
                        break;
                    }
                    if (run.size() >= maxRunLength) {
                        finished = true;
                        pendingValue = null;
                        pendingKey = null;
                        throw new IllegalStateException("adjacent run budget exceeded");
                    }
                    run.add(next);
                }
                action.accept(Collections.unmodifiableList(run));
                return true;
            }

            @Override public Spliterator<List<T>> trySplit() { return null; }
            @Override public long estimateSize() { return finished ? 0 : Long.MAX_VALUE; }
            @Override public int characteristics() {
                return (input.characteristics() & Spliterator.ORDERED) | Spliterator.NONNULL;
            }
        };
        return StreamSupport.stream(runs, false).onClose(source::close);
    }


    /**
     * Maps each int value with its zero-based primitive {@code long} encounter
     * index at this pipeline stage. The value is the first callback argument.
     * For unordered input, indices follow this traversal's received order.
     * Index {@link Long#MAX_VALUE} is valid; demanding a further element throws
     * {@code ArithmeticException} before its mapper is invoked. An existence
     * check may prefetch that additional element.

     * <p>Construction switches the source to sequential mode and acquires its
     * spliterator without requesting elements or invoking callbacks. The result
     * is initially sequential and its primitive source cannot split, even after
     * {@code parallel()}. It preserves encounter order when present, retains
     * constant state, and does not advertise an exact size. Primitive values are
     * traversed without boxing. Closing the result closes the input; exhaustion
     * alone does not. The input must not be used separately afterward.
     * Callback and traversal failures propagate unchanged.
     * @param source the input stream
     * @param mapper the function receiving the primitive value and long index
     * @return the indexed primitive mapping
     * @throws NullPointerException if an argument is null
     * @throws ArithmeticException if traversal demands an index beyond Long.MAX_VALUE
     * @since 21
     */
    public static IntStream mapIndexedInt(IntStream source, IntLongToIntFunction mapper) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(mapper, "mapper");
        Spliterator.OfInt input = source.sequential().spliterator();
        return StreamSupport.intStream(new IndexedIntSpliterator(input, mapper), false)
                .onClose(source::close);
    }

    /**
     * Maps each long value with its zero-based primitive {@code long} encounter
     * index at this pipeline stage. The value is the first callback argument.
     * For unordered input, indices follow this traversal's received order.
     * Index {@link Long#MAX_VALUE} is valid; demanding a further element throws
     * {@code ArithmeticException} before its mapper is invoked. An existence
     * check may prefetch that additional element.

     * <p>Construction switches the source to sequential mode and acquires its
     * spliterator without requesting elements or invoking callbacks. The result
     * is initially sequential and its primitive source cannot split, even after
     * {@code parallel()}. It preserves encounter order when present, retains
     * constant state, and does not advertise an exact size. Primitive values are
     * traversed without boxing. Closing the result closes the input; exhaustion
     * alone does not. The input must not be used separately afterward.
     * Callback and traversal failures propagate unchanged.
     * @param source the input stream
     * @param mapper the function receiving the primitive value and long index
     * @return the indexed primitive mapping
     * @throws NullPointerException if an argument is null
     * @throws ArithmeticException if traversal demands an index beyond Long.MAX_VALUE
     * @since 21
     */
    public static LongStream mapIndexedLong(LongStream source, LongBinaryOperator mapper) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(mapper, "mapper");
        Spliterator.OfLong input = source.sequential().spliterator();
        return StreamSupport.longStream(new IndexedLongSpliterator(input, mapper), false)
                .onClose(source::close);
    }

    /**
     * Maps each double value with its zero-based primitive {@code long} encounter
     * index at this pipeline stage. The value is the first callback argument.
     * For unordered input, indices follow this traversal's received order.
     * Index {@link Long#MAX_VALUE} is valid; demanding a further element throws
     * {@code ArithmeticException} before its mapper is invoked. An existence
     * check may prefetch that additional element.

     * <p>Construction switches the source to sequential mode and acquires its
     * spliterator without requesting elements or invoking callbacks. The result
     * is initially sequential and its primitive source cannot split, even after
     * {@code parallel()}. It preserves encounter order when present, retains
     * constant state, and does not advertise an exact size. Primitive values are
     * traversed without boxing. Closing the result closes the input; exhaustion
     * alone does not. The input must not be used separately afterward.
     * Callback and traversal failures propagate unchanged.
     * @param source the input stream
     * @param mapper the function receiving the primitive value and long index
     * @return the indexed primitive mapping
     * @throws NullPointerException if an argument is null
     * @throws ArithmeticException if traversal demands an index beyond Long.MAX_VALUE
     * @since 21
     */
    public static DoubleStream mapIndexedDouble(DoubleStream source, DoubleLongToDoubleFunction mapper) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(mapper, "mapper");
        Spliterator.OfDouble input = source.sequential().spliterator();
        return StreamSupport.doubleStream(new IndexedDoubleSpliterator(input, mapper), false)
                .onClose(source::close);
    }

    /**
     * Maps overlapping adjacent int values, passing the earlier value first.
     * Empty and singleton inputs produce no result; no pair containers are used.

     * <p>Construction switches the source to sequential mode and acquires its
     * spliterator without requesting elements or invoking callbacks. The result
     * is initially sequential and its primitive source cannot split, even after
     * {@code parallel()}. It preserves encounter order when present, retains
     * constant state, and does not advertise an exact size. Primitive values are
     * traversed without boxing. Closing the result closes the input; exhaustion
     * alone does not. The input must not be used separately afterward.
     * Callback and traversal failures propagate unchanged.
     * @param source the input stream
     * @param mapper the adjacent-value mapping function
     * @return the primitive adjacent-pair mapping
     * @throws NullPointerException if an argument is null
     * @since 21
     */
    public static IntStream pairwiseInt(IntStream source, IntBinaryOperator mapper) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(mapper, "mapper");
        Spliterator.OfInt input = source.sequential().spliterator();
        return StreamSupport.intStream(new PairwiseIntSpliterator(input, mapper), false)
                .onClose(source::close);
    }

    /**
     * Maps overlapping adjacent long values, passing the earlier value first.
     * Empty and singleton inputs produce no result; no pair containers are used.

     * <p>Construction switches the source to sequential mode and acquires its
     * spliterator without requesting elements or invoking callbacks. The result
     * is initially sequential and its primitive source cannot split, even after
     * {@code parallel()}. It preserves encounter order when present, retains
     * constant state, and does not advertise an exact size. Primitive values are
     * traversed without boxing. Closing the result closes the input; exhaustion
     * alone does not. The input must not be used separately afterward.
     * Callback and traversal failures propagate unchanged.
     * @param source the input stream
     * @param mapper the adjacent-value mapping function
     * @return the primitive adjacent-pair mapping
     * @throws NullPointerException if an argument is null
     * @since 21
     */
    public static LongStream pairwiseLong(LongStream source, LongBinaryOperator mapper) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(mapper, "mapper");
        Spliterator.OfLong input = source.sequential().spliterator();
        return StreamSupport.longStream(new PairwiseLongSpliterator(input, mapper), false)
                .onClose(source::close);
    }

    /**
     * Maps overlapping adjacent double values, passing the earlier value first.
     * Empty and singleton inputs produce no result; no pair containers are used.

     * <p>Construction switches the source to sequential mode and acquires its
     * spliterator without requesting elements or invoking callbacks. The result
     * is initially sequential and its primitive source cannot split, even after
     * {@code parallel()}. It preserves encounter order when present, retains
     * constant state, and does not advertise an exact size. Primitive values are
     * traversed without boxing. Closing the result closes the input; exhaustion
     * alone does not. The input must not be used separately afterward.
     * Callback and traversal failures propagate unchanged.
     * @param source the input stream
     * @param mapper the adjacent-value mapping function
     * @return the primitive adjacent-pair mapping
     * @throws NullPointerException if an argument is null
     * @since 21
     */
    public static DoubleStream pairwiseDouble(DoubleStream source, DoubleBinaryOperator mapper) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(mapper, "mapper");
        Spliterator.OfDouble input = source.sequential().spliterator();
        return StreamSupport.doubleStream(new PairwiseDoubleSpliterator(input, mapper), false)
                .onClose(source::close);
    }

    /**
     * Combines corresponding int values until either input is exhausted.
     * Testing for a pair may consume one unpaired value from the longer input.
     * Encounter order is reported only when both input streams are ordered.

     * <p>Both inputs are switched to sequential mode and their spliterators are
     * acquired without requesting elements or invoking callbacks. The result is
     * initially sequential, its primitive source cannot split even after
     * {@code parallel()}, and it does not advertise an exact size. Traversal
     * retains constant state and does not box primitive values.
     *
     * <p>The inputs must be distinct, open and unused, and must not be used
     * separately afterward. Closing the result closes the first input and then
     * the second, even if the first close fails; a second failure is suppressed
     * on the first. Exhaustion alone does not close either input. If construction
     * fails, close responsibility remains with the caller. Callback and
     * traversal failures propagate unchanged.
     * @param first the first input stream
     * @param second the second input stream
     * @param zipper the primitive pair-combining function
     * @return the primitive paired results
     * @throws NullPointerException if an argument is null
     * @throws IllegalArgumentException if both input references are identical
     * @since 21
     */
    public static IntStream zipInt(IntStream first, IntStream second,
            IntBinaryOperator zipper) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        Objects.requireNonNull(zipper, "zipper");
        if (first == second) throw new IllegalArgumentException("distinct streams required");
        Spliterator.OfInt paired = new ZipIntSpliterator(
                first.sequential().spliterator(), second.sequential().spliterator(), zipper);
        return StreamSupport.intStream(paired, false).onClose(composedClose(first, second));
    }

    /**
     * Combines corresponding long values until either input is exhausted.
     * Testing for a pair may consume one unpaired value from the longer input.
     * Encounter order is reported only when both input streams are ordered.

     * <p>Both inputs are switched to sequential mode and their spliterators are
     * acquired without requesting elements or invoking callbacks. The result is
     * initially sequential, its primitive source cannot split even after
     * {@code parallel()}, and it does not advertise an exact size. Traversal
     * retains constant state and does not box primitive values.
     *
     * <p>The inputs must be distinct, open and unused, and must not be used
     * separately afterward. Closing the result closes the first input and then
     * the second, even if the first close fails; a second failure is suppressed
     * on the first. Exhaustion alone does not close either input. If construction
     * fails, close responsibility remains with the caller. Callback and
     * traversal failures propagate unchanged.
     * @param first the first input stream
     * @param second the second input stream
     * @param zipper the primitive pair-combining function
     * @return the primitive paired results
     * @throws NullPointerException if an argument is null
     * @throws IllegalArgumentException if both input references are identical
     * @since 21
     */
    public static LongStream zipLong(LongStream first, LongStream second,
            LongBinaryOperator zipper) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        Objects.requireNonNull(zipper, "zipper");
        if (first == second) throw new IllegalArgumentException("distinct streams required");
        Spliterator.OfLong paired = new ZipLongSpliterator(
                first.sequential().spliterator(), second.sequential().spliterator(), zipper);
        return StreamSupport.longStream(paired, false).onClose(composedClose(first, second));
    }

    /**
     * Combines corresponding double values until either input is exhausted.
     * Testing for a pair may consume one unpaired value from the longer input.
     * Encounter order is reported only when both input streams are ordered.

     * <p>Both inputs are switched to sequential mode and their spliterators are
     * acquired without requesting elements or invoking callbacks. The result is
     * initially sequential, its primitive source cannot split even after
     * {@code parallel()}, and it does not advertise an exact size. Traversal
     * retains constant state and does not box primitive values.
     *
     * <p>The inputs must be distinct, open and unused, and must not be used
     * separately afterward. Closing the result closes the first input and then
     * the second, even if the first close fails; a second failure is suppressed
     * on the first. Exhaustion alone does not close either input. If construction
     * fails, close responsibility remains with the caller. Callback and
     * traversal failures propagate unchanged.
     * @param first the first input stream
     * @param second the second input stream
     * @param zipper the primitive pair-combining function
     * @return the primitive paired results
     * @throws NullPointerException if an argument is null
     * @throws IllegalArgumentException if both input references are identical
     * @since 21
     */
    public static DoubleStream zipDouble(DoubleStream first, DoubleStream second,
            DoubleBinaryOperator zipper) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        Objects.requireNonNull(zipper, "zipper");
        if (first == second) throw new IllegalArgumentException("distinct streams required");
        Spliterator.OfDouble paired = new ZipDoubleSpliterator(
                first.sequential().spliterator(), second.sequential().spliterator(), zipper);
        return StreamSupport.doubleStream(paired, false).onClose(composedClose(first, second));
    }

    /**
     * Inserts a primitive separator strictly between consecutive int values.
     * Empty and singleton inputs are unchanged. Emitting a separator may read
     * and retain one following value so that no trailing separator is emitted.

     * <p>Construction switches the source to sequential mode and acquires its
     * spliterator without requesting elements or invoking callbacks. The result
     * is initially sequential and its primitive source cannot split, even after
     * {@code parallel()}. It preserves encounter order when present, retains
     * constant state, and does not advertise an exact size. Primitive values are
     * traversed without boxing. Closing the result closes the input; exhaustion
     * alone does not. The input must not be used separately afterward.
     * Callback and traversal failures propagate unchanged.
     * @param source the input stream
     * @param separator the value inserted between input values
     * @return the interspersed primitive stream
     * @throws NullPointerException if source is null
     * @since 21
     */
    public static IntStream intersperseInt(IntStream source, int separator) {
        Objects.requireNonNull(source, "source");
        Spliterator.OfInt input = source.sequential().spliterator();
        return StreamSupport.intStream(
                new InterspersedIntSpliterator(input, separator), false).onClose(source::close);
    }

    /**
     * Inserts a primitive separator strictly between consecutive long values.
     * Empty and singleton inputs are unchanged. Emitting a separator may read
     * and retain one following value so that no trailing separator is emitted.

     * <p>Construction switches the source to sequential mode and acquires its
     * spliterator without requesting elements or invoking callbacks. The result
     * is initially sequential and its primitive source cannot split, even after
     * {@code parallel()}. It preserves encounter order when present, retains
     * constant state, and does not advertise an exact size. Primitive values are
     * traversed without boxing. Closing the result closes the input; exhaustion
     * alone does not. The input must not be used separately afterward.
     * Callback and traversal failures propagate unchanged.
     * @param source the input stream
     * @param separator the value inserted between input values
     * @return the interspersed primitive stream
     * @throws NullPointerException if source is null
     * @since 21
     */
    public static LongStream intersperseLong(LongStream source, long separator) {
        Objects.requireNonNull(source, "source");
        Spliterator.OfLong input = source.sequential().spliterator();
        return StreamSupport.longStream(
                new InterspersedLongSpliterator(input, separator), false).onClose(source::close);
    }

    /**
     * Inserts a primitive separator strictly between consecutive double values.
     * Empty and singleton inputs are unchanged. Emitting a separator may read
     * and retain one following value so that no trailing separator is emitted.

     * <p>Construction switches the source to sequential mode and acquires its
     * spliterator without requesting elements or invoking callbacks. The result
     * is initially sequential and its primitive source cannot split, even after
     * {@code parallel()}. It preserves encounter order when present, retains
     * constant state, and does not advertise an exact size. Primitive values are
     * traversed without boxing. Closing the result closes the input; exhaustion
     * alone does not. The input must not be used separately afterward.
     * Callback and traversal failures propagate unchanged.
     * @param source the input stream
     * @param separator the value inserted between input values
     * @return the interspersed primitive stream
     * @throws NullPointerException if source is null
     * @since 21
     */
    public static DoubleStream intersperseDouble(DoubleStream source, double separator) {
        Objects.requireNonNull(source, "source");
        Spliterator.OfDouble input = source.sequential().spliterator();
        return StreamSupport.doubleStream(
                new InterspersedDoubleSpliterator(input, separator), false).onClose(source::close);
    }

    /**
     * Maps int values until the first empty primitive optional. The rejecting
     * input value is consumed, but no later value or mapper call is requested,
     * including after repeated exhaustion checks. A null optional is rejected.

     * <p>Construction switches the source to sequential mode and acquires its
     * spliterator without requesting elements or invoking callbacks. The result
     * is initially sequential and its primitive source cannot split, even after
     * {@code parallel()}. It preserves encounter order when present, retains
     * constant state, and does not advertise an exact size. Primitive values are
     * traversed without boxing. Closing the result closes the input; exhaustion
     * alone does not. The input must not be used separately afterward.
     * Callback and traversal failures propagate unchanged.
     * @param source the input stream
     * @param mapper the primitive-input mapping and termination function
     * @return the present optional values preceding the first empty optional
     * @throws NullPointerException if an argument or mapped optional is null
     * @since 21
     */
    public static IntStream mapWhileInt(IntStream source,
            IntFunction<OptionalInt> mapper) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(mapper, "mapper");
        Spliterator.OfInt input = source.sequential().spliterator();
        return StreamSupport.intStream(
                new MappingWhileIntSpliterator(input, mapper), false).onClose(source::close);
    }

    /**
     * Maps long values until the first empty primitive optional. The rejecting
     * input value is consumed, but no later value or mapper call is requested,
     * including after repeated exhaustion checks. A null optional is rejected.

     * <p>Construction switches the source to sequential mode and acquires its
     * spliterator without requesting elements or invoking callbacks. The result
     * is initially sequential and its primitive source cannot split, even after
     * {@code parallel()}. It preserves encounter order when present, retains
     * constant state, and does not advertise an exact size. Primitive values are
     * traversed without boxing. Closing the result closes the input; exhaustion
     * alone does not. The input must not be used separately afterward.
     * Callback and traversal failures propagate unchanged.
     * @param source the input stream
     * @param mapper the primitive-input mapping and termination function
     * @return the present optional values preceding the first empty optional
     * @throws NullPointerException if an argument or mapped optional is null
     * @since 21
     */
    public static LongStream mapWhileLong(LongStream source,
            LongFunction<OptionalLong> mapper) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(mapper, "mapper");
        Spliterator.OfLong input = source.sequential().spliterator();
        return StreamSupport.longStream(
                new MappingWhileLongSpliterator(input, mapper), false).onClose(source::close);
    }

    /**
     * Maps double values until the first empty primitive optional. The rejecting
     * input value is consumed, but no later value or mapper call is requested,
     * including after repeated exhaustion checks. A null optional is rejected.

     * <p>Construction switches the source to sequential mode and acquires its
     * spliterator without requesting elements or invoking callbacks. The result
     * is initially sequential and its primitive source cannot split, even after
     * {@code parallel()}. It preserves encounter order when present, retains
     * constant state, and does not advertise an exact size. Primitive values are
     * traversed without boxing. Closing the result closes the input; exhaustion
     * alone does not. The input must not be used separately afterward.
     * Callback and traversal failures propagate unchanged.
     * @param source the input stream
     * @param mapper the primitive-input mapping and termination function
     * @return the present optional values preceding the first empty optional
     * @throws NullPointerException if an argument or mapped optional is null
     * @since 21
     */
    public static DoubleStream mapWhileDouble(DoubleStream source,
            DoubleFunction<OptionalDouble> mapper) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(mapper, "mapper");
        Spliterator.OfDouble input = source.sequential().spliterator();
        return StreamSupport.doubleStream(
                new MappingWhileDoubleSpliterator(input, mapper), false).onClose(source::close);
    }

    /**
     * Lazily merges two int streams already sorted in natural ascending order.
     * Duplicates are retained; equal values from the first input precede those
     * from the second, preserving order within each input. Sortedness is a
     * caller precondition and is not checked. The spliterator reports ORDERED
     * and SORTED, with a null comparator for natural ordering.


     * <p>Both inputs are switched to sequential mode and their spliterators are
     * acquired without requesting elements or invoking callbacks. The result is
     * initially sequential, its primitive source cannot split even after
     * {@code parallel()}, and it does not advertise an exact size. Traversal
     * retains constant state and does not box primitive values.
     *
     * <p>The inputs must be distinct, open and unused, and must not be used
     * separately afterward. Closing the result closes the first input and then
     * the second, even if the first close fails; a second failure is suppressed
     * on the first. Exhaustion alone does not close either input. If construction
     * fails, close responsibility remains with the caller. Callback and
     * traversal failures propagate unchanged.
     * @param first the first naturally sorted input
     * @param second the second naturally sorted input
     * @return the stable primitive merge
     * @throws NullPointerException if an argument is null
     * @throws IllegalArgumentException if both input references are identical
     * @since 21
     */
    public static IntStream mergeSortedInt(IntStream first, IntStream second) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        if (first == second) throw new IllegalArgumentException("distinct streams required");
        Spliterator.OfInt merged = new SortedMergeIntSpliterator(
                first.sequential().spliterator(), second.sequential().spliterator());
        return StreamSupport.intStream(merged, false).onClose(composedClose(first, second));
    }

    /**
     * Lazily merges two long streams already sorted in natural ascending order.
     * Duplicates are retained; equal values from the first input precede those
     * from the second, preserving order within each input. Sortedness is a
     * caller precondition and is not checked. The spliterator reports ORDERED
     * and SORTED, with a null comparator for natural ordering.


     * <p>Both inputs are switched to sequential mode and their spliterators are
     * acquired without requesting elements or invoking callbacks. The result is
     * initially sequential, its primitive source cannot split even after
     * {@code parallel()}, and it does not advertise an exact size. Traversal
     * retains constant state and does not box primitive values.
     *
     * <p>The inputs must be distinct, open and unused, and must not be used
     * separately afterward. Closing the result closes the first input and then
     * the second, even if the first close fails; a second failure is suppressed
     * on the first. Exhaustion alone does not close either input. If construction
     * fails, close responsibility remains with the caller. Callback and
     * traversal failures propagate unchanged.
     * @param first the first naturally sorted input
     * @param second the second naturally sorted input
     * @return the stable primitive merge
     * @throws NullPointerException if an argument is null
     * @throws IllegalArgumentException if both input references are identical
     * @since 21
     */
    public static LongStream mergeSortedLong(LongStream first, LongStream second) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        if (first == second) throw new IllegalArgumentException("distinct streams required");
        Spliterator.OfLong merged = new SortedMergeLongSpliterator(
                first.sequential().spliterator(), second.sequential().spliterator());
        return StreamSupport.longStream(merged, false).onClose(composedClose(first, second));
    }

    /**
     * Lazily merges two double streams already sorted in natural ascending order.
     * Duplicates are retained; equal values from the first input precede those
     * from the second, preserving order within each input. Sortedness is a
     * caller precondition and is not checked. The spliterator reports ORDERED
     * and SORTED, with a null comparator for natural ordering.
     * Natural double order is defined by {@link Double#compare(double, double)},
     * including negative zero before positive zero and NaNs after numbers.

     * <p>Both inputs are switched to sequential mode and their spliterators are
     * acquired without requesting elements or invoking callbacks. The result is
     * initially sequential, its primitive source cannot split even after
     * {@code parallel()}, and it does not advertise an exact size. Traversal
     * retains constant state and does not box primitive values.
     *
     * <p>The inputs must be distinct, open and unused, and must not be used
     * separately afterward. Closing the result closes the first input and then
     * the second, even if the first close fails; a second failure is suppressed
     * on the first. Exhaustion alone does not close either input. If construction
     * fails, close responsibility remains with the caller. Callback and
     * traversal failures propagate unchanged.
     * @param first the first naturally sorted input
     * @param second the second naturally sorted input
     * @return the stable primitive merge
     * @throws NullPointerException if an argument is null
     * @throws IllegalArgumentException if both input references are identical
     * @since 21
     */
    public static DoubleStream mergeSortedDouble(DoubleStream first, DoubleStream second) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        if (first == second) throw new IllegalArgumentException("distinct streams required");
        Spliterator.OfDouble merged = new SortedMergeDoubleSpliterator(
                first.sequential().spliterator(), second.sequential().spliterator());
        return StreamSupport.doubleStream(merged, false).onClose(composedClose(first, second));
    }

    /** Shared conservative metadata; each typed subclass keeps primitive traversal. */
    private abstract static class PrimitiveCombinatorSpliterator {
        private final int characteristics;
        boolean finished;

        PrimitiveCombinatorSpliterator(int characteristics) {
            this.characteristics = characteristics | Spliterator.NONNULL;
        }

        public final long estimateSize() { return finished ? 0 : Long.MAX_VALUE; }

        public final int characteristics() { return characteristics; }
    }

    private abstract static class IntCombinatorSpliterator
            extends PrimitiveCombinatorSpliterator implements Spliterator.OfInt {
        IntCombinatorSpliterator(int characteristics) { super(characteristics); }

        @Override
        public final Spliterator.OfInt trySplit() { return null; }
    }

    private abstract static class LongCombinatorSpliterator
            extends PrimitiveCombinatorSpliterator implements Spliterator.OfLong {
        LongCombinatorSpliterator(int characteristics) { super(characteristics); }

        @Override
        public final Spliterator.OfLong trySplit() { return null; }
    }

    private abstract static class DoubleCombinatorSpliterator
            extends PrimitiveCombinatorSpliterator implements Spliterator.OfDouble {
        DoubleCombinatorSpliterator(int characteristics) { super(characteristics); }

        @Override
        public final Spliterator.OfDouble trySplit() { return null; }
    }

    private static final class IndexedIntSpliterator extends IntCombinatorSpliterator {
        private final PrimitiveIterator.OfInt input;
        private final IntLongToIntFunction mapper;
        private long index;
        private boolean overflow;

        IndexedIntSpliterator(Spliterator.OfInt input, IntLongToIntFunction mapper) {
            super(input.characteristics() & Spliterator.ORDERED);
            this.input = Spliterators.iterator(input);
            this.mapper = mapper;
        }

        @Override
        public boolean tryAdvance(IntConsumer action) {
            Objects.requireNonNull(action, "action");
            if (finished) return false;
            if (!input.hasNext()) { finished = true; return false; }
            if (overflow) throw new ArithmeticException("stream index overflow");
            int value = input.nextInt();
            long current = index;
            if (index == Long.MAX_VALUE) overflow = true;
            else index++;
            action.accept(mapper.applyAsInt(value, current));
            return true;
        }
    }

    private static final class IndexedLongSpliterator extends LongCombinatorSpliterator {
        private final PrimitiveIterator.OfLong input;
        private final LongBinaryOperator mapper;
        private long index;
        private boolean overflow;

        IndexedLongSpliterator(Spliterator.OfLong input, LongBinaryOperator mapper) {
            super(input.characteristics() & Spliterator.ORDERED);
            this.input = Spliterators.iterator(input);
            this.mapper = mapper;
        }

        @Override
        public boolean tryAdvance(LongConsumer action) {
            Objects.requireNonNull(action, "action");
            if (finished) return false;
            if (!input.hasNext()) { finished = true; return false; }
            if (overflow) throw new ArithmeticException("stream index overflow");
            long value = input.nextLong();
            long current = index;
            if (index == Long.MAX_VALUE) overflow = true;
            else index++;
            action.accept(mapper.applyAsLong(value, current));
            return true;
        }
    }

    private static final class IndexedDoubleSpliterator extends DoubleCombinatorSpliterator {
        private final PrimitiveIterator.OfDouble input;
        private final DoubleLongToDoubleFunction mapper;
        private long index;
        private boolean overflow;

        IndexedDoubleSpliterator(Spliterator.OfDouble input, DoubleLongToDoubleFunction mapper) {
            super(input.characteristics() & Spliterator.ORDERED);
            this.input = Spliterators.iterator(input);
            this.mapper = mapper;
        }

        @Override
        public boolean tryAdvance(DoubleConsumer action) {
            Objects.requireNonNull(action, "action");
            if (finished) return false;
            if (!input.hasNext()) { finished = true; return false; }
            if (overflow) throw new ArithmeticException("stream index overflow");
            double value = input.nextDouble();
            long current = index;
            if (index == Long.MAX_VALUE) overflow = true;
            else index++;
            action.accept(mapper.applyAsDouble(value, current));
            return true;
        }
    }

    private static final class PairwiseIntSpliterator extends IntCombinatorSpliterator {
        private final PrimitiveIterator.OfInt input;
        private final IntBinaryOperator mapper;
        private boolean hasPrevious;
        private int previous;

        PairwiseIntSpliterator(Spliterator.OfInt input, IntBinaryOperator mapper) {
            super(input.characteristics() & Spliterator.ORDERED);
            this.input = Spliterators.iterator(input);
            this.mapper = mapper;
        }

        @Override
        public boolean tryAdvance(IntConsumer action) {
            Objects.requireNonNull(action, "action");
            if (finished) return false;
            if (!hasPrevious) {
                if (!input.hasNext()) { finished = true; return false; }
                previous = input.nextInt();
                hasPrevious = true;
            }
            if (!input.hasNext()) { finished = true; return false; }
            int first = previous;
            int second = input.nextInt();
            previous = second;
            action.accept(mapper.applyAsInt(first, second));
            return true;
        }
    }

    private static final class PairwiseLongSpliterator extends LongCombinatorSpliterator {
        private final PrimitiveIterator.OfLong input;
        private final LongBinaryOperator mapper;
        private boolean hasPrevious;
        private long previous;

        PairwiseLongSpliterator(Spliterator.OfLong input, LongBinaryOperator mapper) {
            super(input.characteristics() & Spliterator.ORDERED);
            this.input = Spliterators.iterator(input);
            this.mapper = mapper;
        }

        @Override
        public boolean tryAdvance(LongConsumer action) {
            Objects.requireNonNull(action, "action");
            if (finished) return false;
            if (!hasPrevious) {
                if (!input.hasNext()) { finished = true; return false; }
                previous = input.nextLong();
                hasPrevious = true;
            }
            if (!input.hasNext()) { finished = true; return false; }
            long first = previous;
            long second = input.nextLong();
            previous = second;
            action.accept(mapper.applyAsLong(first, second));
            return true;
        }
    }

    private static final class PairwiseDoubleSpliterator extends DoubleCombinatorSpliterator {
        private final PrimitiveIterator.OfDouble input;
        private final DoubleBinaryOperator mapper;
        private boolean hasPrevious;
        private double previous;

        PairwiseDoubleSpliterator(Spliterator.OfDouble input, DoubleBinaryOperator mapper) {
            super(input.characteristics() & Spliterator.ORDERED);
            this.input = Spliterators.iterator(input);
            this.mapper = mapper;
        }

        @Override
        public boolean tryAdvance(DoubleConsumer action) {
            Objects.requireNonNull(action, "action");
            if (finished) return false;
            if (!hasPrevious) {
                if (!input.hasNext()) { finished = true; return false; }
                previous = input.nextDouble();
                hasPrevious = true;
            }
            if (!input.hasNext()) { finished = true; return false; }
            double first = previous;
            double second = input.nextDouble();
            previous = second;
            action.accept(mapper.applyAsDouble(first, second));
            return true;
        }
    }

    private static final class ZipIntSpliterator extends IntCombinatorSpliterator {
        private final PrimitiveIterator.OfInt first;
        private final PrimitiveIterator.OfInt second;
        private final IntBinaryOperator zipper;

        ZipIntSpliterator(Spliterator.OfInt first, Spliterator.OfInt second,
                IntBinaryOperator zipper) {
            super(first.characteristics() & second.characteristics() & Spliterator.ORDERED);
            this.first = Spliterators.iterator(first);
            this.second = Spliterators.iterator(second);
            this.zipper = zipper;
        }

        @Override
        public boolean tryAdvance(IntConsumer action) {
            Objects.requireNonNull(action, "action");
            if (finished) return false;
            if (!first.hasNext() || !second.hasNext()) { finished = true; return false; }
            int left = first.nextInt();
            int right = second.nextInt();
            action.accept(zipper.applyAsInt(left, right));
            return true;
        }
    }

    private static final class ZipLongSpliterator extends LongCombinatorSpliterator {
        private final PrimitiveIterator.OfLong first;
        private final PrimitiveIterator.OfLong second;
        private final LongBinaryOperator zipper;

        ZipLongSpliterator(Spliterator.OfLong first, Spliterator.OfLong second,
                LongBinaryOperator zipper) {
            super(first.characteristics() & second.characteristics() & Spliterator.ORDERED);
            this.first = Spliterators.iterator(first);
            this.second = Spliterators.iterator(second);
            this.zipper = zipper;
        }

        @Override
        public boolean tryAdvance(LongConsumer action) {
            Objects.requireNonNull(action, "action");
            if (finished) return false;
            if (!first.hasNext() || !second.hasNext()) { finished = true; return false; }
            long left = first.nextLong();
            long right = second.nextLong();
            action.accept(zipper.applyAsLong(left, right));
            return true;
        }
    }

    private static final class ZipDoubleSpliterator extends DoubleCombinatorSpliterator {
        private final PrimitiveIterator.OfDouble first;
        private final PrimitiveIterator.OfDouble second;
        private final DoubleBinaryOperator zipper;

        ZipDoubleSpliterator(Spliterator.OfDouble first, Spliterator.OfDouble second,
                DoubleBinaryOperator zipper) {
            super(first.characteristics() & second.characteristics() & Spliterator.ORDERED);
            this.first = Spliterators.iterator(first);
            this.second = Spliterators.iterator(second);
            this.zipper = zipper;
        }

        @Override
        public boolean tryAdvance(DoubleConsumer action) {
            Objects.requireNonNull(action, "action");
            if (finished) return false;
            if (!first.hasNext() || !second.hasNext()) { finished = true; return false; }
            double left = first.nextDouble();
            double right = second.nextDouble();
            action.accept(zipper.applyAsDouble(left, right));
            return true;
        }
    }

    private static final class InterspersedIntSpliterator extends IntCombinatorSpliterator {
        private final PrimitiveIterator.OfInt input;
        private final int separator;
        private boolean first = true;
        private boolean hasPending;
        private int pending;

        InterspersedIntSpliterator(Spliterator.OfInt input, int separator) {
            super(input.characteristics() & Spliterator.ORDERED);
            this.input = Spliterators.iterator(input);
            this.separator = separator;
        }

        @Override
        public boolean tryAdvance(IntConsumer action) {
            Objects.requireNonNull(action, "action");
            if (finished) return false;
            if (hasPending) {
                hasPending = false;
                action.accept(pending);
                return true;
            }
            if (!input.hasNext()) { finished = true; return false; }
            int value = input.nextInt();
            if (first) {
                first = false;
                action.accept(value);
            } else {
                pending = value;
                hasPending = true;
                action.accept(separator);
            }
            return true;
        }
    }

    private static final class InterspersedLongSpliterator extends LongCombinatorSpliterator {
        private final PrimitiveIterator.OfLong input;
        private final long separator;
        private boolean first = true;
        private boolean hasPending;
        private long pending;

        InterspersedLongSpliterator(Spliterator.OfLong input, long separator) {
            super(input.characteristics() & Spliterator.ORDERED);
            this.input = Spliterators.iterator(input);
            this.separator = separator;
        }

        @Override
        public boolean tryAdvance(LongConsumer action) {
            Objects.requireNonNull(action, "action");
            if (finished) return false;
            if (hasPending) {
                hasPending = false;
                action.accept(pending);
                return true;
            }
            if (!input.hasNext()) { finished = true; return false; }
            long value = input.nextLong();
            if (first) {
                first = false;
                action.accept(value);
            } else {
                pending = value;
                hasPending = true;
                action.accept(separator);
            }
            return true;
        }
    }

    private static final class InterspersedDoubleSpliterator extends DoubleCombinatorSpliterator {
        private final PrimitiveIterator.OfDouble input;
        private final double separator;
        private boolean first = true;
        private boolean hasPending;
        private double pending;

        InterspersedDoubleSpliterator(Spliterator.OfDouble input, double separator) {
            super(input.characteristics() & Spliterator.ORDERED);
            this.input = Spliterators.iterator(input);
            this.separator = separator;
        }

        @Override
        public boolean tryAdvance(DoubleConsumer action) {
            Objects.requireNonNull(action, "action");
            if (finished) return false;
            if (hasPending) {
                hasPending = false;
                action.accept(pending);
                return true;
            }
            if (!input.hasNext()) { finished = true; return false; }
            double value = input.nextDouble();
            if (first) {
                first = false;
                action.accept(value);
            } else {
                pending = value;
                hasPending = true;
                action.accept(separator);
            }
            return true;
        }
    }

    private static final class MappingWhileIntSpliterator extends IntCombinatorSpliterator {
        private final PrimitiveIterator.OfInt input;
        private final IntFunction<OptionalInt> mapper;

        MappingWhileIntSpliterator(Spliterator.OfInt input, IntFunction<OptionalInt> mapper) {
            super(input.characteristics() & Spliterator.ORDERED);
            this.input = Spliterators.iterator(input);
            this.mapper = mapper;
        }

        @Override
        public boolean tryAdvance(IntConsumer action) {
            Objects.requireNonNull(action, "action");
            if (finished) return false;
            if (!input.hasNext()) { finished = true; return false; }
            OptionalInt result = Objects.requireNonNull(
                    mapper.apply(input.nextInt()), "mapped optional");
            if (result.isEmpty()) { finished = true; return false; }
            action.accept(result.getAsInt());
            return true;
        }
    }

    private static final class MappingWhileLongSpliterator extends LongCombinatorSpliterator {
        private final PrimitiveIterator.OfLong input;
        private final LongFunction<OptionalLong> mapper;

        MappingWhileLongSpliterator(Spliterator.OfLong input, LongFunction<OptionalLong> mapper) {
            super(input.characteristics() & Spliterator.ORDERED);
            this.input = Spliterators.iterator(input);
            this.mapper = mapper;
        }

        @Override
        public boolean tryAdvance(LongConsumer action) {
            Objects.requireNonNull(action, "action");
            if (finished) return false;
            if (!input.hasNext()) { finished = true; return false; }
            OptionalLong result = Objects.requireNonNull(
                    mapper.apply(input.nextLong()), "mapped optional");
            if (result.isEmpty()) { finished = true; return false; }
            action.accept(result.getAsLong());
            return true;
        }
    }

    private static final class MappingWhileDoubleSpliterator extends DoubleCombinatorSpliterator {
        private final PrimitiveIterator.OfDouble input;
        private final DoubleFunction<OptionalDouble> mapper;

        MappingWhileDoubleSpliterator(Spliterator.OfDouble input, DoubleFunction<OptionalDouble> mapper) {
            super(input.characteristics() & Spliterator.ORDERED);
            this.input = Spliterators.iterator(input);
            this.mapper = mapper;
        }

        @Override
        public boolean tryAdvance(DoubleConsumer action) {
            Objects.requireNonNull(action, "action");
            if (finished) return false;
            if (!input.hasNext()) { finished = true; return false; }
            OptionalDouble result = Objects.requireNonNull(
                    mapper.apply(input.nextDouble()), "mapped optional");
            if (result.isEmpty()) { finished = true; return false; }
            action.accept(result.getAsDouble());
            return true;
        }
    }

    private static final class SortedMergeIntSpliterator extends IntCombinatorSpliterator {
        private final PrimitiveIterator.OfInt first;
        private final PrimitiveIterator.OfInt second;
        private boolean firstDone;
        private boolean secondDone;
        private boolean hasFirst;
        private boolean hasSecond;
        private int firstValue;
        private int secondValue;

        SortedMergeIntSpliterator(Spliterator.OfInt first, Spliterator.OfInt second) {
            super(Spliterator.ORDERED | Spliterator.SORTED);
            this.first = Spliterators.iterator(first);
            this.second = Spliterators.iterator(second);
        }

        @Override
        public Comparator<? super Integer> getComparator() { return null; }

        @Override
        public boolean tryAdvance(IntConsumer action) {
            Objects.requireNonNull(action, "action");
            if (finished) return false;
            if (!hasFirst && !firstDone) {
                if (first.hasNext()) { firstValue = first.nextInt(); hasFirst = true; }
                else firstDone = true;
            }
            if (!hasSecond && !secondDone) {
                if (second.hasNext()) { secondValue = second.nextInt(); hasSecond = true; }
                else secondDone = true;
            }
            if (!hasFirst && !hasSecond) { finished = true; return false; }
            int value;
            if (!hasSecond || (hasFirst && Integer.compare(firstValue, secondValue) <= 0)) {
                value = firstValue;
                hasFirst = false;
            } else {
                value = secondValue;
                hasSecond = false;
            }
            action.accept(value);
            return true;
        }
    }

    private static final class SortedMergeLongSpliterator extends LongCombinatorSpliterator {
        private final PrimitiveIterator.OfLong first;
        private final PrimitiveIterator.OfLong second;
        private boolean firstDone;
        private boolean secondDone;
        private boolean hasFirst;
        private boolean hasSecond;
        private long firstValue;
        private long secondValue;

        SortedMergeLongSpliterator(Spliterator.OfLong first, Spliterator.OfLong second) {
            super(Spliterator.ORDERED | Spliterator.SORTED);
            this.first = Spliterators.iterator(first);
            this.second = Spliterators.iterator(second);
        }

        @Override
        public Comparator<? super Long> getComparator() { return null; }

        @Override
        public boolean tryAdvance(LongConsumer action) {
            Objects.requireNonNull(action, "action");
            if (finished) return false;
            if (!hasFirst && !firstDone) {
                if (first.hasNext()) { firstValue = first.nextLong(); hasFirst = true; }
                else firstDone = true;
            }
            if (!hasSecond && !secondDone) {
                if (second.hasNext()) { secondValue = second.nextLong(); hasSecond = true; }
                else secondDone = true;
            }
            if (!hasFirst && !hasSecond) { finished = true; return false; }
            long value;
            if (!hasSecond || (hasFirst && Long.compare(firstValue, secondValue) <= 0)) {
                value = firstValue;
                hasFirst = false;
            } else {
                value = secondValue;
                hasSecond = false;
            }
            action.accept(value);
            return true;
        }
    }

    private static final class SortedMergeDoubleSpliterator extends DoubleCombinatorSpliterator {
        private final PrimitiveIterator.OfDouble first;
        private final PrimitiveIterator.OfDouble second;
        private boolean firstDone;
        private boolean secondDone;
        private boolean hasFirst;
        private boolean hasSecond;
        private double firstValue;
        private double secondValue;

        SortedMergeDoubleSpliterator(Spliterator.OfDouble first, Spliterator.OfDouble second) {
            super(Spliterator.ORDERED | Spliterator.SORTED);
            this.first = Spliterators.iterator(first);
            this.second = Spliterators.iterator(second);
        }

        @Override
        public Comparator<? super Double> getComparator() { return null; }

        @Override
        public boolean tryAdvance(DoubleConsumer action) {
            Objects.requireNonNull(action, "action");
            if (finished) return false;
            if (!hasFirst && !firstDone) {
                if (first.hasNext()) { firstValue = first.nextDouble(); hasFirst = true; }
                else firstDone = true;
            }
            if (!hasSecond && !secondDone) {
                if (second.hasNext()) { secondValue = second.nextDouble(); hasSecond = true; }
                else secondDone = true;
            }
            if (!hasFirst && !hasSecond) { finished = true; return false; }
            double value;
            if (!hasSecond || (hasFirst && Double.compare(firstValue, secondValue) <= 0)) {
                value = firstValue;
                hasFirst = false;
            } else {
                value = secondValue;
                hasSecond = false;
            }
            action.accept(value);
            return true;
        }
    }

    private static <T> Stream<T> combinatorStream(BaseStream<?, ?> source,
            Spliterator<T> traversal) {
        return StreamSupport.stream(traversal, false).onClose(source::close);
    }

    /** One serial state owner; AbstractSpliterator's batching split is deliberately absent. */
    private abstract static class CombinatorSpliterator<T> implements Spliterator<T> {
        private final int characteristics;
        boolean finished;

        CombinatorSpliterator(int characteristics) {
            this.characteristics = characteristics;
        }

        @Override
        public final Spliterator<T> trySplit() { return null; }

        @Override
        public final long estimateSize() { return finished ? 0 : Long.MAX_VALUE; }

        @Override
        public final int characteristics() { return characteristics; }
    }

    private static final class PairwiseSpliterator<T, R> extends CombinatorSpliterator<R> {
        private final Iterator<? extends T> input;
        private final BiFunction<? super T, ? super T, ? extends R> mapper;
        private boolean hasPrevious;
        private T previous;

        PairwiseSpliterator(Spliterator<? extends T> input,
                BiFunction<? super T, ? super T, ? extends R> mapper) {
            super(input.characteristics() & Spliterator.ORDERED);
            this.input = Spliterators.iterator(input);
            this.mapper = mapper;
        }

        @Override
        public boolean tryAdvance(Consumer<? super R> action) {
            Objects.requireNonNull(action, "action");
            if (finished) return false;
            if (!hasPrevious) {
                if (!input.hasNext()) { finished = true; return false; }
                previous = input.next();
                hasPrevious = true;
            }
            if (!input.hasNext()) {
                previous = null;
                finished = true;
                return false;
            }
            T first = previous;
            T second = input.next();
            previous = second;
            action.accept(mapper.apply(first, second));
            return true;
        }
    }

    private static final class IndexedSpliterator<T, R> extends CombinatorSpliterator<R> {
        private final Iterator<? extends T> input;
        private final ObjLongFunction<? super T, ? extends R> mapper;
        private long index;
        private boolean overflow;

        IndexedSpliterator(Spliterator<? extends T> input,
                ObjLongFunction<? super T, ? extends R> mapper) {
            super(input.characteristics() & Spliterator.ORDERED);
            this.input = Spliterators.iterator(input);
            this.mapper = mapper;
        }

        @Override
        public boolean tryAdvance(Consumer<? super R> action) {
            Objects.requireNonNull(action, "action");
            if (finished) return false;
            if (!input.hasNext()) { finished = true; return false; }
            if (overflow) throw new ArithmeticException("stream index overflow");
            T value = input.next();
            long current = index;
            if (index == Long.MAX_VALUE) overflow = true;
            else index++;
            action.accept(mapper.apply(value, current));
            return true;
        }
    }

    private static final class InterspersedSpliterator<T> extends CombinatorSpliterator<T> {
        private final Iterator<? extends T> input;
        private final T separator;
        private boolean first = true;
        private boolean hasPending;
        private T pending;

        InterspersedSpliterator(Spliterator<? extends T> input, T separator) {
            super(input.characteristics() & Spliterator.ORDERED);
            this.input = Spliterators.iterator(input);
            this.separator = separator;
        }

        @Override
        public boolean tryAdvance(Consumer<? super T> action) {
            Objects.requireNonNull(action, "action");
            if (finished) return false;
            if (hasPending) {
                T value = pending;
                pending = null;
                hasPending = false;
                action.accept(value);
                return true;
            }
            if (!input.hasNext()) { finished = true; return false; }
            T value = input.next();
            if (first) {
                first = false;
                action.accept(value);
            } else {
                pending = value;
                hasPending = true;
                action.accept(separator);
            }
            return true;
        }
    }

    private static final class MappingWhileSpliterator<T, R> extends CombinatorSpliterator<R> {
        private final Iterator<? extends T> input;
        private final Function<? super T, ? extends Optional<? extends R>> mapper;

        MappingWhileSpliterator(Spliterator<? extends T> input,
                Function<? super T, ? extends Optional<? extends R>> mapper) {
            super((input.characteristics() & Spliterator.ORDERED) | Spliterator.NONNULL);
            this.input = Spliterators.iterator(input);
            this.mapper = mapper;
        }

        @Override
        public boolean tryAdvance(Consumer<? super R> action) {
            Objects.requireNonNull(action, "action");
            if (finished) return false;
            if (!input.hasNext()) { finished = true; return false; }
            Optional<? extends R> result = Objects.requireNonNull(
                    mapper.apply(input.next()), "mapped optional");
            if (result.isEmpty()) { finished = true; return false; }
            action.accept(result.get());
            return true;
        }
    }

    private static final class DistinctKeySpliterator<T, K> extends CombinatorSpliterator<T> {
        private final Iterator<? extends T> input;
        private final Function<? super T, ? extends K> keyExtractor;
        private final int maxDistinct;
        private final Set<K> seen = new HashSet<>();

        DistinctKeySpliterator(Spliterator<? extends T> input,
                Function<? super T, ? extends K> keyExtractor, int maxDistinct) {
            super(input.characteristics() & Spliterator.ORDERED);
            this.input = Spliterators.iterator(input);
            this.keyExtractor = keyExtractor;
            this.maxDistinct = maxDistinct;
        }

        @Override
        public boolean tryAdvance(Consumer<? super T> action) {
            Objects.requireNonNull(action, "action");
            if (finished) return false;
            while (input.hasNext()) {
                T value = input.next();
                K key = keyExtractor.apply(value);
                if (seen.contains(key)) continue;
                if (seen.size() >= maxDistinct) {
                    throw new IllegalStateException("distinct key budget exceeded");
                }
                seen.add(key);
                action.accept(value);
                return true;
            }
            finished = true;
            return false;
        }
    }

    private static final class SortedMergeSpliterator<T> extends CombinatorSpliterator<T> {
        private final Iterator<? extends T> first;
        private final Iterator<? extends T> second;
        private final Comparator<? super T> comparator;
        private boolean firstDone;
        private boolean secondDone;
        private boolean hasFirst;
        private boolean hasSecond;
        private T firstValue;
        private T secondValue;

        SortedMergeSpliterator(Spliterator<? extends T> first,
                Spliterator<? extends T> second, Comparator<? super T> comparator) {
            super(Spliterator.ORDERED | Spliterator.SORTED);
            this.first = Spliterators.iterator(first);
            this.second = Spliterators.iterator(second);
            this.comparator = comparator;
        }

        @Override
        public Comparator<? super T> getComparator() { return comparator; }

        @Override
        public boolean tryAdvance(Consumer<? super T> action) {
            Objects.requireNonNull(action, "action");
            if (finished) return false;
            if (!hasFirst && !firstDone) {
                if (first.hasNext()) { firstValue = first.next(); hasFirst = true; }
                else firstDone = true;
            }
            if (!hasSecond && !secondDone) {
                if (second.hasNext()) { secondValue = second.next(); hasSecond = true; }
                else secondDone = true;
            }
            if (!hasFirst && !hasSecond) { finished = true; return false; }
            T value;
            if (!hasSecond || (hasFirst && comparator.compare(firstValue, secondValue) <= 0)) {
                value = firstValue;
                firstValue = null;
                hasFirst = false;
            } else {
                value = secondValue;
                secondValue = null;
                hasSecond = false;
            }
            action.accept(value);
            return true;
        }
    }

    // A pair is one replaceable behavioral atom. Unsplittable traversal keeps
    // one-shot input iterators and unequal-length handling serial and bounded.
    private static final class ZipSpliterator<A, B, R> implements Spliterator<R> {
        private final Iterator<? extends A> first;
        private final Iterator<? extends B> second;
        private final BiFunction<? super A, ? super B, ? extends R> zipper;
        private final int characteristics;
        private long estimate;
        private boolean finished;

        ZipSpliterator(Spliterator<? extends A> first, Spliterator<? extends B> second,
                       BiFunction<? super A, ? super B, ? extends R> zipper) {
            this.first = Spliterators.iterator(first);
            this.second = Spliterators.iterator(second);
            this.zipper = zipper;
            this.estimate = Math.min(first.estimateSize(), second.estimateSize());
            this.characteristics = first.characteristics() & second.characteristics()
                    & (Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED);
        }

        @Override
        public boolean tryAdvance(Consumer<? super R> action) {
            Objects.requireNonNull(action, "action");
            if (finished) return false;
            if (!first.hasNext() || !second.hasNext()) {
                finished = true;
                estimate = 0;
                return false;
            }
            A a = first.next();
            B b = second.next();
            if (estimate > 0 && (estimate != Long.MAX_VALUE
                    || (characteristics & Spliterator.SIZED) != 0)) estimate--;
            action.accept(zipper.apply(a, b));
            return true;
        }

        @Override
        public Spliterator<R> trySplit() {
            return null;
        }

        @Override
        public long estimateSize() {
            return estimate;
        }

        @Override
        public int characteristics() {
            return characteristics;
        }
    }

    /**
     * An {@code int} range spliterator.
     */
    static final class RangeIntSpliterator implements Spliterator.OfInt {
        // Can never be greater that upTo, this avoids overflow if upper bound
        // is Integer.MAX_VALUE
        // All elements are traversed if from == upTo & last == 0
        private int from;
        private final int upTo;
        // 1 if the range is closed and the last element has not been traversed
        // Otherwise, 0 if the range is open, or is a closed range and all
        // elements have been traversed
        private int last;

        RangeIntSpliterator(int from, int upTo, boolean closed) {
            this(from, upTo, closed ? 1 : 0);
        }

        private RangeIntSpliterator(int from, int upTo, int last) {
            this.from = from;
            this.upTo = upTo;
            this.last = last;
        }

        @Override
        public boolean tryAdvance(IntConsumer consumer) {
            Objects.requireNonNull(consumer);

            final int i = from;
            if (i < upTo) {
                from++;
                consumer.accept(i);
                return true;
            }
            else if (last > 0) {
                last = 0;
                consumer.accept(i);
                return true;
            }
            return false;
        }

        @Override
        @IntrinsicCandidate
        public void forEachRemaining(IntConsumer consumer) {
            Objects.requireNonNull(consumer);

            int i = from;
            final int hUpTo = upTo;
            int hLast = last;
            from = upTo;
            last = 0;
            while (i < hUpTo) {
                consumer.accept(i++);
            }
            if (hLast > 0) {
                // Last element of closed range
                consumer.accept(i);
            }
        }

        @Override
        public long estimateSize() {
            // Ensure ranges of size > Integer.MAX_VALUE report the correct size
            return ((long) upTo) - from + last;
        }

        @Override
        public int characteristics() {
            return Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED |
                   Spliterator.IMMUTABLE | Spliterator.NONNULL |
                   Spliterator.DISTINCT | Spliterator.SORTED;
        }

        @Override
        public Comparator<? super Integer> getComparator() {
            return null;
        }

        @Override
        public Spliterator.OfInt trySplit() {
            long size = estimateSize();
            return size <= 1
                   ? null
                   // Left split always has a half-open range
                   : new RangeIntSpliterator(from, from = from + splitPoint(size), 0);
        }

        /**
         * The spliterator size below which the spliterator will be split
         * at the mid-point to produce balanced splits. Above this size the
         * spliterator will be split at a ratio of
         * 1:(RIGHT_BALANCED_SPLIT_RATIO - 1)
         * to produce right-balanced splits.
         *
         * <p>Such splitting ensures that for very large ranges that the left
         * side of the range will more likely be processed at a lower-depth
         * than a balanced tree at the expense of a higher-depth for the right
         * side of the range.
         *
         * <p>This is optimized for cases such as IntStream.range(0, Integer.MAX_VALUE)
         * that is likely to be augmented with a limit operation that limits the
         * number of elements to a count lower than this threshold.
         */
        private static final int BALANCED_SPLIT_THRESHOLD = 1 << 24;

        /**
         * The split ratio of the left and right split when the spliterator
         * size is above BALANCED_SPLIT_THRESHOLD.
         */
        private static final int RIGHT_BALANCED_SPLIT_RATIO = 1 << 3;

        private int splitPoint(long size) {
            int d = (size < BALANCED_SPLIT_THRESHOLD) ? 2 : RIGHT_BALANCED_SPLIT_RATIO;
            // Cast to int is safe since:
            //   2 <= size < 2^32
            //   2 <= d <= 8
            return (int) (size / d);
        }
    }

    /**
     * A {@code long} range spliterator.
     *
     * This implementation cannot be used for ranges whose size is greater
     * than Long.MAX_VALUE
     */
    static final class RangeLongSpliterator implements Spliterator.OfLong {
        // Can never be greater that upTo, this avoids overflow if upper bound
        // is Long.MAX_VALUE
        // All elements are traversed if from == upTo & last == 0
        private long from;
        private final long upTo;
        // 1 if the range is closed and the last element has not been traversed
        // Otherwise, 0 if the range is open, or is a closed range and all
        // elements have been traversed
        private int last;

        RangeLongSpliterator(long from, long upTo, boolean closed) {
            this(from, upTo, closed ? 1 : 0);
        }

        private RangeLongSpliterator(long from, long upTo, int last) {
            assert upTo - from + last > 0;
            this.from = from;
            this.upTo = upTo;
            this.last = last;
        }

        @Override
        public boolean tryAdvance(LongConsumer consumer) {
            Objects.requireNonNull(consumer);

            final long i = from;
            if (i < upTo) {
                from++;
                consumer.accept(i);
                return true;
            }
            else if (last > 0) {
                last = 0;
                consumer.accept(i);
                return true;
            }
            return false;
        }

        @Override
        public void forEachRemaining(LongConsumer consumer) {
            Objects.requireNonNull(consumer);

            long i = from;
            final long hUpTo = upTo;
            int hLast = last;
            from = upTo;
            last = 0;
            while (i < hUpTo) {
                consumer.accept(i++);
            }
            if (hLast > 0) {
                // Last element of closed range
                consumer.accept(i);
            }
        }

        @Override
        public long estimateSize() {
            return upTo - from + last;
        }

        @Override
        public int characteristics() {
            return Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED |
                   Spliterator.IMMUTABLE | Spliterator.NONNULL |
                   Spliterator.DISTINCT | Spliterator.SORTED;
        }

        @Override
        public Comparator<? super Long> getComparator() {
            return null;
        }

        @Override
        public Spliterator.OfLong trySplit() {
            long size = estimateSize();
            return size <= 1
                   ? null
                   // Left split always has a half-open range
                   : new RangeLongSpliterator(from, from = from + splitPoint(size), 0);
        }

        /**
         * The spliterator size below which the spliterator will be split
         * at the mid-point to produce balanced splits. Above this size the
         * spliterator will be split at a ratio of
         * 1:(RIGHT_BALANCED_SPLIT_RATIO - 1)
         * to produce right-balanced splits.
         *
         * <p>Such splitting ensures that for very large ranges that the left
         * side of the range will more likely be processed at a lower-depth
         * than a balanced tree at the expense of a higher-depth for the right
         * side of the range.
         *
         * <p>This is optimized for cases such as LongStream.range(0, Long.MAX_VALUE)
         * that is likely to be augmented with a limit operation that limits the
         * number of elements to a count lower than this threshold.
         */
        private static final long BALANCED_SPLIT_THRESHOLD = 1 << 24;

        /**
         * The split ratio of the left and right split when the spliterator
         * size is above BALANCED_SPLIT_THRESHOLD.
         */
        private static final long RIGHT_BALANCED_SPLIT_RATIO = 1 << 3;

        private long splitPoint(long size) {
            long d = (size < BALANCED_SPLIT_THRESHOLD) ? 2 : RIGHT_BALANCED_SPLIT_RATIO;
            // 2 <= size <= Long.MAX_VALUE
            return size / d;
        }
    }

    private abstract static class AbstractStreamBuilderImpl<T, S extends Spliterator<T>> implements Spliterator<T> {
        // >= 0 when building, < 0 when built
        // -1 == no elements
        // -2 == one element, held by first
        // -3 == two or more elements, held by buffer
        int count;

        // Spliterator implementation for 0 or 1 element
        // count == -1 for no elements
        // count == -2 for one element held by first

        @Override
        public S trySplit() {
            return null;
        }

        @Override
        public long estimateSize() {
            return -count - 1;
        }

        @Override
        public int characteristics() {
            return Spliterator.SIZED | Spliterator.SUBSIZED |
                   Spliterator.ORDERED | Spliterator.IMMUTABLE;
        }
    }

    static final class StreamBuilderImpl<T>
            extends AbstractStreamBuilderImpl<T, Spliterator<T>>
            implements Stream.Builder<T> {
        // The first element in the stream
        // valid if count == 1
        T first;

        // The first and subsequent elements in the stream
        // non-null if count == 2
        SpinedBuffer<T> buffer;

        /**
         * Constructor for building a stream of 0 or more elements.
         */
        StreamBuilderImpl() { }

        /**
         * Constructor for a singleton stream.
         *
         * @param t the single element
         */
        StreamBuilderImpl(T t) {
            first = t;
            count = -2;
        }

        // StreamBuilder implementation

        @Override
        public void accept(T t) {
            if (count == 0) {
                first = t;
                count++;
            }
            else if (count > 0) {
                if (buffer == null) {
                    buffer = new SpinedBuffer<>();
                    buffer.accept(first);
                    count++;
                }

                buffer.accept(t);
            }
            else {
                throw new IllegalStateException();
            }
        }

        public Stream.Builder<T> add(T t) {
            accept(t);
            return this;
        }

        @Override
        public Stream<T> build() {
            int c = count;
            if (c >= 0) {
                // Switch count to negative value signalling the builder is built
                count = -count - 1;
                // Use this spliterator if 0 or 1 elements, otherwise use
                // the spliterator of the spined buffer
                return (c < 2) ? StreamSupport.stream(this, false) : StreamSupport.stream(buffer.spliterator(), false);
            }

            throw new IllegalStateException();
        }

        // Spliterator implementation for 0 or 1 element
        // count == -1 for no elements
        // count == -2 for one element held by first

        @Override
        public boolean tryAdvance(Consumer<? super T> action) {
            Objects.requireNonNull(action);

            if (count == -2) {
                action.accept(first);
                count = -1;
                return true;
            }
            else {
                return false;
            }
        }

        @Override
        public void forEachRemaining(Consumer<? super T> action) {
            Objects.requireNonNull(action);

            if (count == -2) {
                action.accept(first);
                count = -1;
            }
        }
    }

    static final class IntStreamBuilderImpl
            extends AbstractStreamBuilderImpl<Integer, Spliterator.OfInt>
            implements IntStream.Builder, Spliterator.OfInt {
        // The first element in the stream
        // valid if count == 1
        int first;

        // The first and subsequent elements in the stream
        // non-null if count == 2
        SpinedBuffer.OfInt buffer;

        /**
         * Constructor for building a stream of 0 or more elements.
         */
        IntStreamBuilderImpl() { }

        /**
         * Constructor for a singleton stream.
         *
         * @param t the single element
         */
        IntStreamBuilderImpl(int t) {
            first = t;
            count = -2;
        }

        // StreamBuilder implementation

        @Override
        public void accept(int t) {
            if (count == 0) {
                first = t;
                count++;
            }
            else if (count > 0) {
                if (buffer == null) {
                    buffer = new SpinedBuffer.OfInt();
                    buffer.accept(first);
                    count++;
                }

                buffer.accept(t);
            }
            else {
                throw new IllegalStateException();
            }
        }

        @Override
        public IntStream build() {
            int c = count;
            if (c >= 0) {
                // Switch count to negative value signalling the builder is built
                count = -count - 1;
                // Use this spliterator if 0 or 1 elements, otherwise use
                // the spliterator of the spined buffer
                return (c < 2) ? StreamSupport.intStream(this, false) : StreamSupport.intStream(buffer.spliterator(), false);
            }

            throw new IllegalStateException();
        }

        // Spliterator implementation for 0 or 1 element
        // count == -1 for no elements
        // count == -2 for one element held by first

        @Override
        public boolean tryAdvance(IntConsumer action) {
            Objects.requireNonNull(action);

            if (count == -2) {
                action.accept(first);
                count = -1;
                return true;
            }
            else {
                return false;
            }
        }

        @Override
        public void forEachRemaining(IntConsumer action) {
            Objects.requireNonNull(action);

            if (count == -2) {
                action.accept(first);
                count = -1;
            }
        }
    }

    static final class LongStreamBuilderImpl
            extends AbstractStreamBuilderImpl<Long, Spliterator.OfLong>
            implements LongStream.Builder, Spliterator.OfLong {
        // The first element in the stream
        // valid if count == 1
        long first;

        // The first and subsequent elements in the stream
        // non-null if count == 2
        SpinedBuffer.OfLong buffer;

        /**
         * Constructor for building a stream of 0 or more elements.
         */
        LongStreamBuilderImpl() { }

        /**
         * Constructor for a singleton stream.
         *
         * @param t the single element
         */
        LongStreamBuilderImpl(long t) {
            first = t;
            count = -2;
        }

        // StreamBuilder implementation

        @Override
        public void accept(long t) {
            if (count == 0) {
                first = t;
                count++;
            }
            else if (count > 0) {
                if (buffer == null) {
                    buffer = new SpinedBuffer.OfLong();
                    buffer.accept(first);
                    count++;
                }

                buffer.accept(t);
            }
            else {
                throw new IllegalStateException();
            }
        }

        @Override
        public LongStream build() {
            int c = count;
            if (c >= 0) {
                // Switch count to negative value signalling the builder is built
                count = -count - 1;
                // Use this spliterator if 0 or 1 elements, otherwise use
                // the spliterator of the spined buffer
                return (c < 2) ? StreamSupport.longStream(this, false) : StreamSupport.longStream(buffer.spliterator(), false);
            }

            throw new IllegalStateException();
        }

        // Spliterator implementation for 0 or 1 element
        // count == -1 for no elements
        // count == -2 for one element held by first

        @Override
        public boolean tryAdvance(LongConsumer action) {
            Objects.requireNonNull(action);

            if (count == -2) {
                action.accept(first);
                count = -1;
                return true;
            }
            else {
                return false;
            }
        }

        @Override
        public void forEachRemaining(LongConsumer action) {
            Objects.requireNonNull(action);

            if (count == -2) {
                action.accept(first);
                count = -1;
            }
        }
    }

    static final class DoubleStreamBuilderImpl
            extends AbstractStreamBuilderImpl<Double, Spliterator.OfDouble>
            implements DoubleStream.Builder, Spliterator.OfDouble {
        // The first element in the stream
        // valid if count == 1
        double first;

        // The first and subsequent elements in the stream
        // non-null if count == 2
        SpinedBuffer.OfDouble buffer;

        /**
         * Constructor for building a stream of 0 or more elements.
         */
        DoubleStreamBuilderImpl() { }

        /**
         * Constructor for a singleton stream.
         *
         * @param t the single element
         */
        DoubleStreamBuilderImpl(double t) {
            first = t;
            count = -2;
        }

        // StreamBuilder implementation

        @Override
        public void accept(double t) {
            if (count == 0) {
                first = t;
                count++;
            }
            else if (count > 0) {
                if (buffer == null) {
                    buffer = new SpinedBuffer.OfDouble();
                    buffer.accept(first);
                    count++;
                }

                buffer.accept(t);
            }
            else {
                throw new IllegalStateException();
            }
        }

        @Override
        public DoubleStream build() {
            int c = count;
            if (c >= 0) {
                // Switch count to negative value signalling the builder is built
                count = -count - 1;
                // Use this spliterator if 0 or 1 elements, otherwise use
                // the spliterator of the spined buffer
                return (c < 2) ? StreamSupport.doubleStream(this, false) : StreamSupport.doubleStream(buffer.spliterator(), false);
            }

            throw new IllegalStateException();
        }

        // Spliterator implementation for 0 or 1 element
        // count == -1 for no elements
        // count == -2 for one element held by first

        @Override
        public boolean tryAdvance(DoubleConsumer action) {
            Objects.requireNonNull(action);

            if (count == -2) {
                action.accept(first);
                count = -1;
                return true;
            }
            else {
                return false;
            }
        }

        @Override
        public void forEachRemaining(DoubleConsumer action) {
            Objects.requireNonNull(action);

            if (count == -2) {
                action.accept(first);
                count = -1;
            }
        }
    }

    abstract static class ConcatSpliterator<T, T_SPLITR extends Spliterator<T>>
            implements Spliterator<T> {
        protected final T_SPLITR aSpliterator;
        protected final T_SPLITR bSpliterator;
        // True when no split has occurred, otherwise false
        boolean beforeSplit;
        // Never read after splitting
        final boolean unsized;

        public ConcatSpliterator(T_SPLITR aSpliterator, T_SPLITR bSpliterator) {
            this.aSpliterator = aSpliterator;
            this.bSpliterator = bSpliterator;
            beforeSplit = true;
            // The spliterator is known to be unsized before splitting if the
            // sum of the estimates overflows.
            unsized = aSpliterator.estimateSize() + bSpliterator.estimateSize() < 0;
        }

        @Override
        public T_SPLITR trySplit() {
            @SuppressWarnings("unchecked")
            T_SPLITR ret = beforeSplit ? aSpliterator : (T_SPLITR) bSpliterator.trySplit();
            beforeSplit = false;
            return ret;
        }

        @Override
        public boolean tryAdvance(Consumer<? super T> consumer) {
            boolean hasNext;
            if (beforeSplit) {
                hasNext = aSpliterator.tryAdvance(consumer);
                if (!hasNext) {
                    beforeSplit = false;
                    hasNext = bSpliterator.tryAdvance(consumer);
                }
            }
            else
                hasNext = bSpliterator.tryAdvance(consumer);
            return hasNext;
        }

        @Override
        public void forEachRemaining(Consumer<? super T> consumer) {
            if (beforeSplit)
                aSpliterator.forEachRemaining(consumer);
            bSpliterator.forEachRemaining(consumer);
        }

        @Override
        public long estimateSize() {
            if (beforeSplit) {
                // If one or both estimates are Long.MAX_VALUE then the sum
                // will either be Long.MAX_VALUE or overflow to a negative value
                long size = aSpliterator.estimateSize() + bSpliterator.estimateSize();
                return (size >= 0) ? size : Long.MAX_VALUE;
            }
            else {
                return bSpliterator.estimateSize();
            }
        }

        @Override
        public int characteristics() {
            if (beforeSplit) {
                // Concatenation loses DISTINCT and SORTED characteristics
                return aSpliterator.characteristics() & bSpliterator.characteristics()
                       & ~(Spliterator.DISTINCT | Spliterator.SORTED
                           | (unsized ? Spliterator.SIZED | Spliterator.SUBSIZED : 0));
            }
            else {
                return bSpliterator.characteristics();
            }
        }

        @Override
        public Comparator<? super T> getComparator() {
            if (beforeSplit)
                throw new IllegalStateException();
            return bSpliterator.getComparator();
        }

        static class OfRef<T> extends ConcatSpliterator<T, Spliterator<T>> {
            OfRef(Spliterator<T> aSpliterator, Spliterator<T> bSpliterator) {
                super(aSpliterator, bSpliterator);
            }
        }

        private abstract static class OfPrimitive<T, T_CONS, T_SPLITR extends Spliterator.OfPrimitive<T, T_CONS, T_SPLITR>>
                extends ConcatSpliterator<T, T_SPLITR>
                implements Spliterator.OfPrimitive<T, T_CONS, T_SPLITR> {
            private OfPrimitive(T_SPLITR aSpliterator, T_SPLITR bSpliterator) {
                super(aSpliterator, bSpliterator);
            }

            @Override
            public boolean tryAdvance(T_CONS action) {
                boolean hasNext;
                if (beforeSplit) {
                    hasNext = aSpliterator.tryAdvance(action);
                    if (!hasNext) {
                        beforeSplit = false;
                        hasNext = bSpliterator.tryAdvance(action);
                    }
                }
                else
                    hasNext = bSpliterator.tryAdvance(action);
                return hasNext;
            }

            @Override
            public void forEachRemaining(T_CONS action) {
                if (beforeSplit)
                    aSpliterator.forEachRemaining(action);
                bSpliterator.forEachRemaining(action);
            }
        }

        @SuppressWarnings("overloads")
        static class OfInt
                extends ConcatSpliterator.OfPrimitive<Integer, IntConsumer, Spliterator.OfInt>
                implements Spliterator.OfInt {
            OfInt(Spliterator.OfInt aSpliterator, Spliterator.OfInt bSpliterator) {
                super(aSpliterator, bSpliterator);
            }
        }

        @SuppressWarnings("overloads")
        static class OfLong
                extends ConcatSpliterator.OfPrimitive<Long, LongConsumer, Spliterator.OfLong>
                implements Spliterator.OfLong {
            OfLong(Spliterator.OfLong aSpliterator, Spliterator.OfLong bSpliterator) {
                super(aSpliterator, bSpliterator);
            }
        }

        @SuppressWarnings("overloads")
        static class OfDouble
                extends ConcatSpliterator.OfPrimitive<Double, DoubleConsumer, Spliterator.OfDouble>
                implements Spliterator.OfDouble {
            OfDouble(Spliterator.OfDouble aSpliterator, Spliterator.OfDouble bSpliterator) {
                super(aSpliterator, bSpliterator);
            }
        }
    }

    /**
     * Given two Runnables, return a Runnable that executes both in sequence,
     * even if the first throws an exception, and if both throw exceptions, add
     * any exceptions thrown by the second as suppressed exceptions of the first.
     */
    static Runnable composeWithExceptions(Runnable a, Runnable b) {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    a.run();
                }
                catch (Throwable e1) {
                    try {
                        b.run();
                    }
                    catch (Throwable e2) {
                        try {
                            e1.addSuppressed(e2);
                        } catch (Throwable ignore) {}
                    }
                    throw e1;
                }
                b.run();
            }
        };
    }

    /**
     * Given two streams, return a Runnable that
     * executes both of their {@link BaseStream#close} methods in sequence,
     * even if the first throws an exception, and if both throw exceptions, add
     * any exceptions thrown by the second as suppressed exceptions of the first.
     */
    static Runnable composedClose(BaseStream<?, ?> a, BaseStream<?, ?> b) {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    a.close();
                }
                catch (Throwable e1) {
                    try {
                        b.close();
                    }
                    catch (Throwable e2) {
                        try {
                            e1.addSuppressed(e2);
                        } catch (Throwable ignore) {}
                    }
                    throw e1;
                }
                b.close();
            }
        };
    }
}
