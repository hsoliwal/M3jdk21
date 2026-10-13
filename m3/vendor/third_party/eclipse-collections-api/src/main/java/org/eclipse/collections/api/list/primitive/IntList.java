/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.api.list.primitive;

import org.eclipse.collections.api.IntIterable;
import org.eclipse.collections.api.block.function.primitive.IntIntToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.IntToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.IntIntPredicate;
import org.eclipse.collections.api.block.predicate.primitive.IntPredicate;
import org.eclipse.collections.api.block.procedure.primitive.IntIntProcedure;
import org.eclipse.collections.api.block.procedure.primitive.IntProcedure;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.ordered.primitive.ReversibleIntIterable;
import org.eclipse.collections.api.tuple.primitive.IntIntPair;
import org.eclipse.collections.api.tuple.primitive.IntObjectPair;

import java.util.Spliterator;
import java.util.stream.StreamSupport;
import java.util.stream.IntStream;
import java.util.Objects;

/**
 * This file was automatically generated from template file primitiveList.stg.
 *
 * @since 3.0.
 */
public interface IntList extends ReversibleIntIterable
{
    int get(int index);

    long dotProduct(IntList list);

    int binarySearch(int value);

    int lastIndexOf(int value);

    @Override
    IntList select(IntPredicate predicate);

    @Override
    IntList reject(IntPredicate predicate);

    /**
     * @since 9.0.
     */
    @Override
    default IntList tap(IntProcedure procedure)
    {
        this.forEach(procedure);
        return this;
    }

    /**
     * This method iterates over two CharList instances of the same size together using the specified CharCharProcedure.
     *
     * @since 11.1
     */
    default void forEachInBoth(IntList other, IntIntProcedure procedure)
    {
        Objects.requireNonNull(other);
        if (this.size() == other.size())
        {
            this.forEachWithIndex((each, index) -> procedure.value(each, other.get(index)));
        }
        else
        {
            throw new IllegalArgumentException("Attempt to call forEachInBoth with two IntList instances of different sizes :"
                    + this.size()
                    + ':'
                    + other.size());
        }
    }

    /**
     * Returns a new IntList including all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    @Override
    default IntList selectWithIndex(IntIntPredicate predicate)
    {
        int[] index = {0};
        return this.select(each -> predicate.accept(each, index[0]++));
    }

    /**
     * Returns a new IntList excluding all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    @Override
    default IntList rejectWithIndex(IntIntPredicate predicate)
    {
        int[] index = {0};
        return this.reject(each -> predicate.accept(each, index[0]++));
    }

    @Override
    <V> ListIterable<V> collect(IntToObjectFunction<? extends V> function);

    /**
     * Returns a new ListIterable using results obtained by applying the specified function to each element
     * and its corresponding index.
     *
     * @since 9.1.
     */
    @Override
    default <V> ListIterable<V> collectWithIndex(IntIntToObjectFunction<? extends V> function)
    {
        int[] index = {0};
        return this.collect(each -> function.value(each, index[0]++));
    }

    /**
     * Follows the same general contract as {@link java.util.List#equals(Object)}.
     */
    @Override
    boolean equals(Object o);

    /**
     * Follows the same general contract as {@link java.util.List#hashCode()}.
     */
    @Override
    int hashCode();

    /**
     * Returns an immutable copy of this list. If the list is immutable, it returns itself.
     */
    ImmutableIntList toImmutable();

    /**
     * @since 6.0.
     */
    @Override
    IntList distinct();

    /**
     * @since 5.0.
     */
    @Override
    IntList toReversed();

    /**
     * @see java.util.List#subList(int fromIndex, int toIndex)
     * @since 5.0.
     */
    IntList subList(int fromIndex, int toIndex);

    /**
     * Returns a {@code ListIterable} formed from this {@code IntList} and another {@code IntList} by
     * combining corresponding elements in pairs. If one of the two {@code IntList}s is longer than the other, its
     * remaining elements are ignored.
     *
     * @since 9.1.
     */
    default ListIterable<IntIntPair> zipInt(IntIterable iterable)
    {
        throw new UnsupportedOperationException("Default method to prevent breaking backwards compatibility");
    }

    /**
     * Returns a {@code ListIterable} formed from this {@code IntList} and a {@code ListIterable} by
     * combining corresponding elements in pairs. If one of the two Lists is longer than the other, its
     * remaining elements are ignored.
     *
     * @since 9.1.
     */
    default <T> ListIterable<IntObjectPair<T>> zip(Iterable<T> iterable)
    {
        throw new UnsupportedOperationException("Default method to prevent breaking backwards compatibility");
    }

    /**
     * @since 10.0
     */
    Spliterator.OfInt spliterator();

    /**
     * @since 10.0
     */
    default IntStream primitiveStream()
    {
        return StreamSupport.intStream(this.spliterator(), false);
    }

    /**
     * @since 10.0
     */
    default IntStream primitiveParallelStream()
    {
        return StreamSupport.intStream(this.spliterator(), true);
    }
}
