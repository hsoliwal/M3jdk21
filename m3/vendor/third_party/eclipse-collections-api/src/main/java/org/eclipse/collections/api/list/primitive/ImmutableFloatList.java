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
import org.eclipse.collections.api.block.procedure.primitive.FloatProcedure;
import org.eclipse.collections.api.collection.primitive.ImmutableFloatCollection;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.tuple.primitive.FloatFloatPair;
import org.eclipse.collections.api.tuple.primitive.FloatObjectPair;

/**
 * This file was automatically generated from template file immutablePrimitiveList.stg.
 *
 * @since 3.0.
 */
public interface ImmutableFloatList extends ImmutableFloatCollection, FloatList
{
    @Override
    ImmutableFloatList select(FloatPredicate predicate);

    @Override
    ImmutableFloatList reject(FloatPredicate predicate);

    /**
     * @since 9.0.
     */
    @Override
    default ImmutableFloatList tap(FloatProcedure procedure)
    {
        this.forEach(procedure);
        return this;
    }

    /**
     * Returns a new ImmutableFloatList including all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    @Override
    default ImmutableFloatList selectWithIndex(FloatIntPredicate predicate)
    {
        int[] index = {0};
        return this.select(each -> predicate.accept(each, index[0]++));
    }

    /**
     * Returns a new ImmutableFloatList excluding all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    @Override
    default ImmutableFloatList rejectWithIndex(FloatIntPredicate predicate)
    {
        int[] index = {0};
        return this.reject(each -> predicate.accept(each, index[0]++));
    }

    @Override
    <V> ImmutableList<V> collect(FloatToObjectFunction<? extends V> function);

    /**
     * Returns a new ImmutableList using results obtained by applying the specified function to each element
     * and its corresponding index.
     *
     * @since 9.1.
     */
    @Override
    default <V> ImmutableList<V> collectWithIndex(FloatIntToObjectFunction<? extends V> function)
    {
        int[] index = {0};
        return this.collect(each -> function.value(each, index[0]++));
    }

    @Override
    ImmutableFloatList newWith(float element);

    @Override
    ImmutableFloatList newWithout(float element);

    @Override
    ImmutableFloatList newWithAll(FloatIterable elements);

    @Override
    ImmutableFloatList newWithoutAll(FloatIterable elements);

    @Override
    ImmutableFloatList toReversed();

    /**
     * @since 6.0.
     */
    @Override
    ImmutableFloatList distinct();

    @Override
    ImmutableFloatList subList(int fromIndex, int toIndex);

    /**
     * Returns an {@code ImmutableList} formed from this {@code ImmutableFloatList} and another {@code FloatList} by
     * combining corresponding elements in pairs. If one of the two {@code FloatList}s is longer than the other, its
     * remaining elements are ignored.
     *
     * @since 9.1.
     */
    default ImmutableList<FloatFloatPair> zipFloat(FloatIterable iterable)
    {
        throw new UnsupportedOperationException("Default method to prevent breaking backwards compatibility");
    }

    /**
     * Returns an {@code ImmutableList} formed from this {@code ImmutableFloatList} and a {@code ListIterable} by
     * combining corresponding elements in pairs. If one of the two Lists is longer than the other, its
     * remaining elements are ignored.
     *
     * @since 9.1.
     */
    default <T> ImmutableList<FloatObjectPair<T>> zip(Iterable<T> iterable)
    {
        throw new UnsupportedOperationException("Default method to prevent breaking backwards compatibility");
    }
}
