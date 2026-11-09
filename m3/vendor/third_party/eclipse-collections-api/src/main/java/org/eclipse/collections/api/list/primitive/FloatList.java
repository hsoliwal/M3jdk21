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

import org.eclipse.collections.api.FloatIterable;
import org.eclipse.collections.api.block.function.primitive.FloatIntToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.FloatToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.FloatIntPredicate;
import org.eclipse.collections.api.block.predicate.primitive.FloatPredicate;
import org.eclipse.collections.api.block.procedure.primitive.FloatFloatProcedure;
import org.eclipse.collections.api.block.procedure.primitive.FloatProcedure;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.ordered.primitive.ReversibleFloatIterable;
import org.eclipse.collections.api.tuple.primitive.FloatFloatPair;
import org.eclipse.collections.api.tuple.primitive.FloatObjectPair;

import java.util.Objects;

/**
 * This file was automatically generated from template file primitiveList.stg.
 *
 * @since 3.0.
 */
public interface FloatList extends ReversibleFloatIterable
{
    float get(int index);

    double dotProduct(FloatList list);

    int binarySearch(float value);

    int lastIndexOf(float value);

    @Override
    FloatList select(FloatPredicate predicate);

    @Override
    FloatList reject(FloatPredicate predicate);

    /**
     * @since 9.0.
     */
    @Override
    default FloatList tap(FloatProcedure procedure)
    {
        this.forEach(procedure);
        return this;
    }

    /**
     * This method iterates over two CharList instances of the same size together using the specified CharCharProcedure.
     *
     * @since 11.1
     */
    default void forEachInBoth(FloatList other, FloatFloatProcedure procedure)
    {
        Objects.requireNonNull(other);
        if (this.size() == other.size())
        {
            this.forEachWithIndex((each, index) -> procedure.value(each, other.get(index)));
        }
        else
        {
            throw new IllegalArgumentException("Attempt to call forEachInBoth with two FloatList instances of different sizes :"
                    + this.size()
                    + ':'
                    + other.size());
        }
    }

    /**
     * Returns a new FloatList including all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    @Override
    default FloatList selectWithIndex(FloatIntPredicate predicate)
    {
        int[] index = {0};
        return this.select(each -> predicate.accept(each, index[0]++));
    }

    /**
     * Returns a new FloatList excluding all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    @Override
    default FloatList rejectWithIndex(FloatIntPredicate predicate)
    {
        int[] index = {0};
        return this.reject(each -> predicate.accept(each, index[0]++));
    }

    @Override
    <V> ListIterable<V> collect(FloatToObjectFunction<? extends V> function);

    /**
     * Returns a new ListIterable using results obtained by applying the specified function to each element
     * and its corresponding index.
     *
     * @since 9.1.
     */
    @Override
    default <V> ListIterable<V> collectWithIndex(FloatIntToObjectFunction<? extends V> function)
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
    ImmutableFloatList toImmutable();

    /**
     * @since 6.0.
     */
    @Override
    FloatList distinct();

    /**
     * @since 5.0.
     */
    @Override
    FloatList toReversed();

    /**
     * @see java.util.List#subList(int fromIndex, int toIndex)
     * @since 5.0.
     */
    FloatList subList(int fromIndex, int toIndex);

    /**
     * Returns a {@code ListIterable} formed from this {@code FloatList} and another {@code FloatList} by
     * combining corresponding elements in pairs. If one of the two {@code FloatList}s is longer than the other, its
     * remaining elements are ignored.
     *
     * @since 9.1.
     */
    default ListIterable<FloatFloatPair> zipFloat(FloatIterable iterable)
    {
        throw new UnsupportedOperationException("Default method to prevent breaking backwards compatibility");
    }

    /**
     * Returns a {@code ListIterable} formed from this {@code FloatList} and a {@code ListIterable} by
     * combining corresponding elements in pairs. If one of the two Lists is longer than the other, its
     * remaining elements are ignored.
     *
     * @since 9.1.
     */
    default <T> ListIterable<FloatObjectPair<T>> zip(Iterable<T> iterable)
    {
        throw new UnsupportedOperationException("Default method to prevent breaking backwards compatibility");
    }
}
