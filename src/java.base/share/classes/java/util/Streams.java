/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 */
package java.util;

import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Sequential stream entry points for collections, iteration and optional values.
 *
 * <p>Each returned stream starts in sequential mode. Traversal by the stream is
 * deferred until its pipeline is evaluated. A collection or iterable supplies
 * its own spliterator when this method is called, retaining that spliterator's
 * encounter order, characteristics, binding time and interference policy. An
 * enumeration also supplies its iterator adapter when this method is called. Iterator and enumeration streams follow
 * the order in which their source supplies elements.
 *
 * <p>Sources are borrowed. These methods add no close handler and never close an
 * iterator, enumeration, collection or iterable. A caller must separately manage
 * any resource backing the source and must not independently consume the source
 * while its stream pipeline is in use. No wrapper is allocated for each element;
 * primitive iterator overloads use the corresponding primitive stream pipeline.
 */
public final class Streams {
    private Streams() { }

    /**
     * Creates a sequential stream from the collection's own spliterator.
     * @param values the non-null collection
     * @param <T> the element type
     * @return a stream with the source spliterator's encounter semantics
     */
    public static <T> Stream<T> stream(Collection<T> values) {
        Objects.requireNonNull(values, "values");
        return StreamSupport.stream(values.spliterator(), false);
    }

    /**
     * Creates a sequential stream from the iterable's own spliterator.
     * @param values the non-null iterable
     * @param <T> the element type
     * @return a stream with the source spliterator's encounter semantics
     */
    public static <T> Stream<T> stream(Iterable<T> values) {
        Objects.requireNonNull(values, "values");
        return StreamSupport.stream(values.spliterator(), false);
    }

    /**
     * Creates an ordered sequential stream of the iterator's remaining elements.
     * @param iterator the non-null borrowed iterator
     * @param <T> the element type
     * @return a lazily traversed stream
     */
    public static <T> Stream<T> stream(Iterator<? extends T> iterator) {
        Objects.requireNonNull(iterator, "iterator");
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false);
    }

    /**
     * Creates an ordered sequential stream of the enumeration's remaining elements.
     * @param enumeration the non-null borrowed enumeration
     * @param <T> the element type
     * @return a lazily traversed stream
     */
    public static <T> Stream<T> stream(Enumeration<? extends T> enumeration) {
        Objects.requireNonNull(enumeration, "enumeration");
        return stream(enumeration.asIterator());
    }

    /**
     * Returns the optional value's standard empty or single-element stream.
     * @param value the non-null optional
     * @param <T> the element type
     * @return an empty or single-element sequential stream
     */
    public static <T> Stream<T> stream(Optional<T> value) {
        return Objects.requireNonNull(value, "value").stream();
    }

    /**
     * Returns the optional int's standard primitive stream.
     * @param value the non-null optional
     * @return an empty or single-element sequential int stream
     */
    public static IntStream stream(OptionalInt value) {
        return Objects.requireNonNull(value, "value").stream();
    }

    /**
     * Returns the optional long's standard primitive stream.
     * @param value the non-null optional
     * @return an empty or single-element sequential long stream
     */
    public static LongStream stream(OptionalLong value) {
        return Objects.requireNonNull(value, "value").stream();
    }

    /**
     * Returns the optional double's standard primitive stream.
     * @param value the non-null optional
     * @return an empty or single-element sequential double stream
     */
    public static DoubleStream stream(OptionalDouble value) {
        return Objects.requireNonNull(value, "value").stream();
    }

    /**
     * Creates an ordered sequential int stream without boxed element traversal.
     * @param iterator the non-null borrowed primitive iterator
     * @return a lazily traversed int stream
     */
    public static IntStream stream(PrimitiveIterator.OfInt iterator) {
        Objects.requireNonNull(iterator, "iterator");
        return StreamSupport.intStream(
                Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false);
    }

    /**
     * Creates an ordered sequential long stream without boxed element traversal.
     * @param iterator the non-null borrowed primitive iterator
     * @return a lazily traversed long stream
     */
    public static LongStream stream(PrimitiveIterator.OfLong iterator) {
        Objects.requireNonNull(iterator, "iterator");
        return StreamSupport.longStream(
                Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false);
    }

    /**
     * Creates an ordered sequential double stream without boxed element traversal.
     * @param iterator the non-null borrowed primitive iterator
     * @return a lazily traversed double stream
     */
    public static DoubleStream stream(PrimitiveIterator.OfDouble iterator) {
        Objects.requireNonNull(iterator, "iterator");
        return StreamSupport.doubleStream(
                Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false);
    }
}
