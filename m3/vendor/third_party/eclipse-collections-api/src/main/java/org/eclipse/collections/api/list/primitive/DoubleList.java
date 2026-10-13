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

import org.eclipse.collections.api.DoubleIterable;
import org.eclipse.collections.api.block.function.primitive.DoubleIntToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.DoubleToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.DoubleIntPredicate;
import org.eclipse.collections.api.block.predicate.primitive.DoublePredicate;
import org.eclipse.collections.api.block.procedure.primitive.DoubleDoubleProcedure;
import org.eclipse.collections.api.block.procedure.primitive.DoubleProcedure;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.ordered.primitive.ReversibleDoubleIterable;
import org.eclipse.collections.api.tuple.primitive.DoubleDoublePair;
import org.eclipse.collections.api.tuple.primitive.DoubleObjectPair;

import java.util.Spliterator;
import java.util.stream.StreamSupport;
import java.util.stream.DoubleStream;
import java.util.Objects;

/**
 * This file was automatically generated from template file primitiveList.stg.
 *
 * @since 3.0.
 */
public interface DoubleList extends ReversibleDoubleIterable
{
    double get(int index);

    double dotProduct(DoubleList list);

    int binarySearch(double value);

    int lastIndexOf(double value);

    @Override
    DoubleList select(DoublePredicate predicate);

    @Override
    DoubleList reject(DoublePredicate predicate);

    /**
     * @since 9.0.
     */
    @Override
    default DoubleList tap(DoubleProcedure procedure)
    {
        this.forEach(procedure);
        return this;
    }

    /**
     * This method iterates over two CharList instances of the same size together using the specified CharCharProcedure.
     *
     * @since 11.1
     */
    default void forEachInBoth(DoubleList other, DoubleDoubleProcedure procedure)
    {
        Objects.requireNonNull(other);
        if (this.size() == other.size())
        {
            this.forEachWithIndex((each, index) -> procedure.value(each, other.get(index)));
        }
        else
        {
            throw new IllegalArgumentException("Attempt to call forEachInBoth with two DoubleList instances of different sizes :"
                    + this.size()
                    + ':'
                    + other.size());
        }
    }

    /**
     * Returns a new DoubleList including all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    @Override
    default DoubleList selectWithIndex(DoubleIntPredicate predicate)
    {
        int[] index = {0};
        return this.select(each -> predicate.accept(each, index[0]++));
    }

    /**
     * Returns a new DoubleList excluding all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    @Override
    default DoubleList rejectWithIndex(DoubleIntPredicate predicate)
    {
        int[] index = {0};
        return this.reject(each -> predicate.accept(each, index[0]++));
    }

    @Override
    <V> ListIterable<V> collect(DoubleToObjectFunction<? extends V> function);

    /**
     * Returns a new ListIterable using results obtained by applying the specified function to each element
     * and its corresponding index.
     *
     * @since 9.1.
     */
    @Override
    default <V> ListIterable<V> collectWithIndex(DoubleIntToObjectFunction<? extends V> function)
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
    ImmutableDoubleList toImmutable();

    /**
     * @since 6.0.
     */
    @Override
    DoubleList distinct();

    /**
     * @since 5.0.
     */
    @Override
    DoubleList toReversed();

    /**
     * @see java.util.List#subList(int fromIndex, int toIndex)
     * @since 5.0.
     */
    DoubleList subList(int fromIndex, int toIndex);

    /**
     * Returns a {@code ListIterable} formed from this {@code DoubleList} and another {@code DoubleList} by
     * combining corresponding elements in pairs. If one of the two {@code DoubleList}s is longer than the other, its
     * remaining elements are ignored.
     *
     * @since 9.1.
     */
    default ListIterable<DoubleDoublePair> zipDouble(DoubleIterable iterable)
    {
        throw new UnsupportedOperationException("Default method to prevent breaking backwards compatibility");
    }

    /**
     * Returns a {@code ListIterable} formed from this {@code DoubleList} and a {@code ListIterable} by
     * combining corresponding elements in pairs. If one of the two Lists is longer than the other, its
     * remaining elements are ignored.
     *
     * @since 9.1.
     */
    default <T> ListIterable<DoubleObjectPair<T>> zip(Iterable<T> iterable)
    {
        throw new UnsupportedOperationException("Default method to prevent breaking backwards compatibility");
    }

    /**
     * @since 10.0
     */
    Spliterator.OfDouble spliterator();

    /**
     * @since 10.0
     */
    default DoubleStream primitiveStream()
    {
        return StreamSupport.doubleStream(this.spliterator(), false);
    }

    /**
     * @since 10.0
     */
    default DoubleStream primitiveParallelStream()
    {
        return StreamSupport.doubleStream(this.spliterator(), true);
    }
}
