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

import org.eclipse.collections.api.CharIterable;
import org.eclipse.collections.api.block.function.primitive.CharIntToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.CharToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.CharIntPredicate;
import org.eclipse.collections.api.block.predicate.primitive.CharPredicate;
import org.eclipse.collections.api.block.procedure.primitive.CharProcedure;
import org.eclipse.collections.api.collection.primitive.ImmutableCharCollection;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.tuple.primitive.CharCharPair;
import org.eclipse.collections.api.tuple.primitive.CharObjectPair;

/**
 * This file was automatically generated from template file immutablePrimitiveList.stg.
 *
 * @since 3.0.
 */
public interface ImmutableCharList extends ImmutableCharCollection, CharList
{
    @Override
    ImmutableCharList select(CharPredicate predicate);

    @Override
    ImmutableCharList reject(CharPredicate predicate);

    /**
     * @since 9.0.
     */
    @Override
    default ImmutableCharList tap(CharProcedure procedure)
    {
        this.forEach(procedure);
        return this;
    }

    /**
     * Returns a new ImmutableCharList including all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    @Override
    default ImmutableCharList selectWithIndex(CharIntPredicate predicate)
    {
        int[] index = {0};
        return this.select(each -> predicate.accept(each, index[0]++));
    }

    /**
     * Returns a new ImmutableCharList excluding all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    @Override
    default ImmutableCharList rejectWithIndex(CharIntPredicate predicate)
    {
        int[] index = {0};
        return this.reject(each -> predicate.accept(each, index[0]++));
    }

    @Override
    <V> ImmutableList<V> collect(CharToObjectFunction<? extends V> function);

    /**
     * Returns a new ImmutableList using results obtained by applying the specified function to each element
     * and its corresponding index.
     *
     * @since 9.1.
     */
    @Override
    default <V> ImmutableList<V> collectWithIndex(CharIntToObjectFunction<? extends V> function)
    {
        int[] index = {0};
        return this.collect(each -> function.value(each, index[0]++));
    }

    @Override
    ImmutableCharList newWith(char element);

    @Override
    ImmutableCharList newWithout(char element);

    @Override
    ImmutableCharList newWithAll(CharIterable elements);

    @Override
    ImmutableCharList newWithoutAll(CharIterable elements);

    @Override
    ImmutableCharList toReversed();

    /**
     * @since 6.0.
     */
    @Override
    ImmutableCharList distinct();

    @Override
    ImmutableCharList subList(int fromIndex, int toIndex);

    /**
     * Returns an {@code ImmutableList} formed from this {@code ImmutableCharList} and another {@code CharList} by
     * combining corresponding elements in pairs. If one of the two {@code CharList}s is longer than the other, its
     * remaining elements are ignored.
     *
     * @since 9.1.
     */
    default ImmutableList<CharCharPair> zipChar(CharIterable iterable)
    {
        throw new UnsupportedOperationException("Default method to prevent breaking backwards compatibility");
    }

    /**
     * Returns an {@code ImmutableList} formed from this {@code ImmutableCharList} and a {@code ListIterable} by
     * combining corresponding elements in pairs. If one of the two Lists is longer than the other, its
     * remaining elements are ignored.
     *
     * @since 9.1.
     */
    default <T> ImmutableList<CharObjectPair<T>> zip(Iterable<T> iterable)
    {
        throw new UnsupportedOperationException("Default method to prevent breaking backwards compatibility");
    }
}
