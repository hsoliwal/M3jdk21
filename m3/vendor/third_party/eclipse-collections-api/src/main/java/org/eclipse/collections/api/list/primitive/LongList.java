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

import org.eclipse.collections.api.LongIterable;
import org.eclipse.collections.api.block.function.primitive.LongIntToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.LongToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.LongIntPredicate;
import org.eclipse.collections.api.block.predicate.primitive.LongPredicate;
import org.eclipse.collections.api.block.procedure.primitive.LongLongProcedure;
import org.eclipse.collections.api.block.procedure.primitive.LongProcedure;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.ordered.primitive.ReversibleLongIterable;
import org.eclipse.collections.api.tuple.primitive.LongLongPair;
import org.eclipse.collections.api.tuple.primitive.LongObjectPair;

import java.util.Spliterator;
import java.util.stream.StreamSupport;
import java.util.stream.LongStream;
import java.util.Objects;

/**
 * This file was automatically generated from template file primitiveList.stg.
 *
 * @since 3.0.
 */
public interface LongList extends ReversibleLongIterable
{
    long get(int index);

    long dotProduct(LongList list);

    int binarySearch(long value);

    int lastIndexOf(long value);

    @Override
    LongList select(LongPredicate predicate);

    @Override
    LongList reject(LongPredicate predicate);

    /**
     * @since 9.0.
     */
    @Override
    default LongList tap(LongProcedure procedure)
    {
        this.forEach(procedure);
        return this;
    }

    /**
     * This method iterates over two CharList instances of the same size together using the specified CharCharProcedure.
     *
     * @since 11.1
     */
    default void forEachInBoth(LongList other, LongLongProcedure procedure)
    {
        Objects.requireNonNull(other);
        if (this.size() == other.size())
        {
            this.forEachWithIndex((each, index) -> procedure.value(each, other.get(index)));
        }
        else
        {
            throw new IllegalArgumentException("Attempt to call forEachInBoth with two LongList instances of different sizes :"
                    + this.size()
                    + ':'
                    + other.size());
        }
    }

    /**
     * Returns a new LongList including all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    @Override
    default LongList selectWithIndex(LongIntPredicate predicate)
    {
        int[] index = {0};
        return this.select(each -> predicate.accept(each, index[0]++));
    }

    /**
     * Returns a new LongList excluding all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    @Override
    default LongList rejectWithIndex(LongIntPredicate predicate)
    {
        int[] index = {0};
        return this.reject(each -> predicate.accept(each, index[0]++));
    }

    @Override
    <V> ListIterable<V> collect(LongToObjectFunction<? extends V> function);

    /**
     * Returns a new ListIterable using results obtained by applying the specified function to each element
     * and its corresponding index.
     *
     * @since 9.1.
     */
    @Override
    default <V> ListIterable<V> collectWithIndex(LongIntToObjectFunction<? extends V> function)
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
    ImmutableLongList toImmutable();

    /**
     * @since 6.0.
     */
    @Override
    LongList distinct();

    /**
     * @since 5.0.
     */
    @Override
    LongList toReversed();

    /**
     * @see java.util.List#subList(int fromIndex, int toIndex)
     * @since 5.0.
     */
    LongList subList(int fromIndex, int toIndex);

    /**
     * Returns a {@code ListIterable} formed from this {@code LongList} and another {@code LongList} by
     * combining corresponding elements in pairs. If one of the two {@code LongList}s is longer than the other, its
     * remaining elements are ignored.
     *
     * @since 9.1.
     */
    default ListIterable<LongLongPair> zipLong(LongIterable iterable)
    {
        throw new UnsupportedOperationException("Default method to prevent breaking backwards compatibility");
    }

    /**
     * Returns a {@code ListIterable} formed from this {@code LongList} and a {@code ListIterable} by
     * combining corresponding elements in pairs. If one of the two Lists is longer than the other, its
     * remaining elements are ignored.
     *
     * @since 9.1.
     */
    default <T> ListIterable<LongObjectPair<T>> zip(Iterable<T> iterable)
    {
        throw new UnsupportedOperationException("Default method to prevent breaking backwards compatibility");
    }

    /**
     * @since 10.0
     */
    Spliterator.OfLong spliterator();

    /**
     * @since 10.0
     */
    default LongStream primitiveStream()
    {
        return StreamSupport.longStream(this.spliterator(), false);
    }

    /**
     * @since 10.0
     */
    default LongStream primitiveParallelStream()
    {
        return StreamSupport.longStream(this.spliterator(), true);
    }
}
