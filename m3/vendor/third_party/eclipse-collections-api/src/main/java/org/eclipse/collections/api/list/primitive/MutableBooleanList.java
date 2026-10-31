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
import org.eclipse.collections.api.collection.primitive.MutableBooleanCollection;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.primitive.BooleanBooleanPair;
import org.eclipse.collections.api.tuple.primitive.BooleanObjectPair;

/**
 * This file was automatically generated from template file mutablePrimitiveList.stg.
 *
 * @since 3.0.
 */
public interface MutableBooleanList extends MutableBooleanCollection, BooleanList
{
    void addAtIndex(int index, boolean element);

    boolean addAllAtIndex(int index, boolean... source);

    boolean addAllAtIndex(int index, BooleanIterable source);

    boolean removeAtIndex(int index);

    boolean set(int index, boolean element);

    default void swap(int index1, int index2)
    {
        boolean value = this.get(index1);
        this.set(index1, this.get(index2));
        this.set(index2, value);
    }

    @Override
    MutableBooleanList select(BooleanPredicate predicate);

    @Override
    MutableBooleanList reject(BooleanPredicate predicate);

    @Override
    MutableBooleanList with(boolean element);

    @Override
    MutableBooleanList without(boolean element);

    @Override
    MutableBooleanList withAll(BooleanIterable elements);

    @Override
    MutableBooleanList withoutAll(BooleanIterable elements);

    /**
     * @since 9.0.
     */
    @Override
    default MutableBooleanList tap(BooleanProcedure procedure)
    {
        this.forEach(procedure);
        return this;
    }

    /**
     * Returns a new MutableBooleanList including all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    @Override
    default MutableBooleanList selectWithIndex(BooleanIntPredicate predicate)
    {
        int[] index = {0};
        return this.select(each -> predicate.accept(each, index[0]++));
    }

    /**
     * Returns a new MutableBooleanList excluding all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    @Override
    default MutableBooleanList rejectWithIndex(BooleanIntPredicate predicate)
    {
        int[] index = {0};
        return this.reject(each -> predicate.accept(each, index[0]++));
    }

    <V> MutableList<V> collect(BooleanToObjectFunction<? extends V> function);

    /**
     * Returns a new MutableList using results obtained by applying the specified function to each element
     * and its corresponding index.
     *
     * @since 9.1.
     */
    @Override
    default <V> MutableList<V> collectWithIndex(BooleanIntToObjectFunction<? extends V> function)
    {
        int[] index = {0};
        return this.collect(each -> function.value(each, index[0]++));
    }

    MutableBooleanList reverseThis();

    @Override
    MutableBooleanList toReversed();

    /**
     * @since 6.0.
     */
    @Override
    MutableBooleanList distinct();

    @Override
    MutableBooleanList asUnmodifiable();

    @Override
    MutableBooleanList asSynchronized();

    /**
     * Returns an immutable copy of this list.
     */
    @Override
    ImmutableBooleanList toImmutable();

    @Override
    MutableBooleanList subList(int fromIndex, int toIndex);

    /**
     * Returns a {@code MutableList} formed from this {@code MutableBooleanList} and another {@code BooleanList} by
     * combining corresponding elements in pairs. If one of the two {@code BooleanList}s is longer than the other, its
     * remaining elements are ignored.
     *
     * @since 9.1.
     */
    default MutableList<BooleanBooleanPair> zipBoolean(BooleanIterable iterable)
    {
        throw new UnsupportedOperationException("Default method to prevent breaking backwards compatibility");
    }

    /**
     * Returns a {@code MutableList} formed from this {@code MutableBooleanList} and a {@code ListIterable} by
     * combining corresponding elements in pairs. If one of the two Lists is longer than the other, its
     * remaining elements are ignored.
     *
     * @since 9.1.
     */
    default <T> MutableList<BooleanObjectPair<T>> zip(Iterable<T> list)
    {
        throw new UnsupportedOperationException("Default method to prevent breaking backwards compatibility");
    }

    /**
     * Creates a new empty mutable version of the same List type.
     *
     * @since 9.2.
     */
    default MutableBooleanList newEmpty()
    {
        throw new UnsupportedOperationException("Implement in concrete classes.");
    }
}
