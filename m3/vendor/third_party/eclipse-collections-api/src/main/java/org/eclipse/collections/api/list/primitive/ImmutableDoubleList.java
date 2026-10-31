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
import org.eclipse.collections.api.block.procedure.primitive.DoubleProcedure;
import org.eclipse.collections.api.collection.primitive.ImmutableDoubleCollection;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.tuple.primitive.DoubleDoublePair;
import org.eclipse.collections.api.tuple.primitive.DoubleObjectPair;

/**
 * This file was automatically generated from template file immutablePrimitiveList.stg.
 *
 * @since 3.0.
 */
public interface ImmutableDoubleList extends ImmutableDoubleCollection, DoubleList
{
    @Override
    ImmutableDoubleList select(DoublePredicate predicate);

    @Override
    ImmutableDoubleList reject(DoublePredicate predicate);

    /**
     * @since 9.0.
     */
    @Override
    default ImmutableDoubleList tap(DoubleProcedure procedure)
    {
        this.forEach(procedure);
        return this;
    }

    /**
     * Returns a new ImmutableDoubleList including all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    @Override
    default ImmutableDoubleList selectWithIndex(DoubleIntPredicate predicate)
    {
        int[] index = {0};
        return this.select(each -> predicate.accept(each, index[0]++));
    }

    /**
     * Returns a new ImmutableDoubleList excluding all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    @Override
    default ImmutableDoubleList rejectWithIndex(DoubleIntPredicate predicate)
    {
        int[] index = {0};
        return this.reject(each -> predicate.accept(each, index[0]++));
    }

    @Override
    <V> ImmutableList<V> collect(DoubleToObjectFunction<? extends V> function);

    /**
     * Returns a new ImmutableList using results obtained by applying the specified function to each element
     * and its corresponding index.
     *
     * @since 9.1.
     */
    @Override
    default <V> ImmutableList<V> collectWithIndex(DoubleIntToObjectFunction<? extends V> function)
    {
        int[] index = {0};
        return this.collect(each -> function.value(each, index[0]++));
    }

    @Override
    ImmutableDoubleList newWith(double element);

    @Override
    ImmutableDoubleList newWithout(double element);

    @Override
    ImmutableDoubleList newWithAll(DoubleIterable elements);

    @Override
    ImmutableDoubleList newWithoutAll(DoubleIterable elements);

    @Override
    ImmutableDoubleList toReversed();

    /**
     * @since 6.0.
     */
    @Override
    ImmutableDoubleList distinct();

    @Override
    ImmutableDoubleList subList(int fromIndex, int toIndex);

    /**
     * Returns an {@code ImmutableList} formed from this {@code ImmutableDoubleList} and another {@code DoubleList} by
     * combining corresponding elements in pairs. If one of the two {@code DoubleList}s is longer than the other, its
     * remaining elements are ignored.
     *
     * @since 9.1.
     */
    default ImmutableList<DoubleDoublePair> zipDouble(DoubleIterable iterable)
    {
        throw new UnsupportedOperationException("Default method to prevent breaking backwards compatibility");
    }

    /**
     * Returns an {@code ImmutableList} formed from this {@code ImmutableDoubleList} and a {@code ListIterable} by
     * combining corresponding elements in pairs. If one of the two Lists is longer than the other, its
     * remaining elements are ignored.
     *
     * @since 9.1.
     */
    default <T> ImmutableList<DoubleObjectPair<T>> zip(Iterable<T> iterable)
    {
        throw new UnsupportedOperationException("Default method to prevent breaking backwards compatibility");
    }
}
