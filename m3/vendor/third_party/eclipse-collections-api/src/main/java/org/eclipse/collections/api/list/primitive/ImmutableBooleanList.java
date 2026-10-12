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

import org.eclipse.collections.api.BooleanIterable;
import org.eclipse.collections.api.block.function.primitive.BooleanIntToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.BooleanToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.BooleanIntPredicate;
import org.eclipse.collections.api.block.predicate.primitive.BooleanPredicate;
import org.eclipse.collections.api.block.procedure.primitive.BooleanProcedure;
import org.eclipse.collections.api.collection.primitive.ImmutableBooleanCollection;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.tuple.primitive.BooleanBooleanPair;
import org.eclipse.collections.api.tuple.primitive.BooleanObjectPair;

/**
 * This file was automatically generated from template file immutablePrimitiveList.stg.
 *
 * @since 3.0.
 */
public interface ImmutableBooleanList extends ImmutableBooleanCollection, BooleanList
{
    @Override
    ImmutableBooleanList select(BooleanPredicate predicate);

    @Override
    ImmutableBooleanList reject(BooleanPredicate predicate);

    /**
     * @since 9.0.
     */
    @Override
    default ImmutableBooleanList tap(BooleanProcedure procedure)
    {
        this.forEach(procedure);
        return this;
    }

    /**
     * Returns a new ImmutableBooleanList including all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    @Override
    default ImmutableBooleanList selectWithIndex(BooleanIntPredicate predicate)
    {
        int[] index = {0};
        return this.select(each -> predicate.accept(each, index[0]++));
    }

    /**
     * Returns a new ImmutableBooleanList excluding all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    @Override
    default ImmutableBooleanList rejectWithIndex(BooleanIntPredicate predicate)
    {
        int[] index = {0};
        return this.reject(each -> predicate.accept(each, index[0]++));
    }

    @Override
    <V> ImmutableList<V> collect(BooleanToObjectFunction<? extends V> function);

    /**
     * Returns a new ImmutableList using results obtained by applying the specified function to each element
     * and its corresponding index.
     *
     * @since 9.1.
     */
    @Override
    default <V> ImmutableList<V> collectWithIndex(BooleanIntToObjectFunction<? extends V> function)
    {
        int[] index = {0};
        return this.collect(each -> function.value(each, index[0]++));
    }

    @Override
    ImmutableBooleanList newWith(boolean element);

    @Override
    ImmutableBooleanList newWithout(boolean element);

    @Override
    ImmutableBooleanList newWithAll(BooleanIterable elements);

    @Override
    ImmutableBooleanList newWithoutAll(BooleanIterable elements);

    @Override
    ImmutableBooleanList toReversed();

    /**
     * @since 6.0.
     */
    @Override
    ImmutableBooleanList distinct();

    @Override
    ImmutableBooleanList subList(int fromIndex, int toIndex);

    /**
     * Returns an {@code ImmutableList} formed from this {@code ImmutableBooleanList} and another {@code BooleanList} by
     * combining corresponding elements in pairs. If one of the two {@code BooleanList}s is longer than the other, its
     * remaining elements are ignored.
     *
     * @since 9.1.
     */
    default ImmutableList<BooleanBooleanPair> zipBoolean(BooleanIterable iterable)
    {
        throw new UnsupportedOperationException("Default method to prevent breaking backwards compatibility");
    }

    /**
     * Returns an {@code ImmutableList} formed from this {@code ImmutableBooleanList} and a {@code ListIterable} by
     * combining corresponding elements in pairs. If one of the two Lists is longer than the other, its
     * remaining elements are ignored.
     *
     * @since 9.1.
     */
    default <T> ImmutableList<BooleanObjectPair<T>> zip(Iterable<T> iterable)
    {
        throw new UnsupportedOperationException("Default method to prevent breaking backwards compatibility");
    }
}
