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
import org.eclipse.collections.api.block.procedure.primitive.LongProcedure;
import org.eclipse.collections.api.collection.primitive.ImmutableLongCollection;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.tuple.primitive.LongLongPair;
import org.eclipse.collections.api.tuple.primitive.LongObjectPair;

/**
 * This file was automatically generated from template file immutablePrimitiveList.stg.
 *
 * @since 3.0.
 */
public interface ImmutableLongList extends ImmutableLongCollection, LongList
{
    @Override
    ImmutableLongList select(LongPredicate predicate);

    @Override
    ImmutableLongList reject(LongPredicate predicate);

    /**
     * @since 9.0.
     */
    @Override
    default ImmutableLongList tap(LongProcedure procedure)
    {
        this.forEach(procedure);
        return this;
    }

    /**
     * Returns a new ImmutableLongList including all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    @Override
    default ImmutableLongList selectWithIndex(LongIntPredicate predicate)
    {
        int[] index = {0};
        return this.select(each -> predicate.accept(each, index[0]++));
    }

    /**
     * Returns a new ImmutableLongList excluding all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    @Override
    default ImmutableLongList rejectWithIndex(LongIntPredicate predicate)
    {
        int[] index = {0};
        return this.reject(each -> predicate.accept(each, index[0]++));
    }

    @Override
    <V> ImmutableList<V> collect(LongToObjectFunction<? extends V> function);

    /**
     * Returns a new ImmutableList using results obtained by applying the specified function to each element
     * and its corresponding index.
     *
     * @since 9.1.
     */
    @Override
    default <V> ImmutableList<V> collectWithIndex(LongIntToObjectFunction<? extends V> function)
    {
        int[] index = {0};
        return this.collect(each -> function.value(each, index[0]++));
    }

    @Override
    ImmutableLongList newWith(long element);

    @Override
    ImmutableLongList newWithout(long element);

    @Override
    ImmutableLongList newWithAll(LongIterable elements);

    @Override
    ImmutableLongList newWithoutAll(LongIterable elements);

    @Override
    ImmutableLongList toReversed();

    /**
     * @since 6.0.
     */
    @Override
    ImmutableLongList distinct();

    @Override
    ImmutableLongList subList(int fromIndex, int toIndex);

    /**
     * Returns an {@code ImmutableList} formed from this {@code ImmutableLongList} and another {@code LongList} by
     * combining corresponding elements in pairs. If one of the two {@code LongList}s is longer than the other, its
     * remaining elements are ignored.
     *
     * @since 9.1.
     */
    default ImmutableList<LongLongPair> zipLong(LongIterable iterable)
    {
        throw new UnsupportedOperationException("Default method to prevent breaking backwards compatibility");
    }

    /**
     * Returns an {@code ImmutableList} formed from this {@code ImmutableLongList} and a {@code ListIterable} by
     * combining corresponding elements in pairs. If one of the two Lists is longer than the other, its
     * remaining elements are ignored.
     *
     * @since 9.1.
     */
    default <T> ImmutableList<LongObjectPair<T>> zip(Iterable<T> iterable)
    {
        throw new UnsupportedOperationException("Default method to prevent breaking backwards compatibility");
    }
}
