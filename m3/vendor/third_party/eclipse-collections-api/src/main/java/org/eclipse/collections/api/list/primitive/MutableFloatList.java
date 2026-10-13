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

import java.util.Comparator;
import java.util.Random;
import org.eclipse.collections.api.block.comparator.primitive.FloatComparator;

import org.eclipse.collections.api.FloatIterable;
import org.eclipse.collections.api.block.function.primitive.FloatIntToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.FloatToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.FloatIntPredicate;
import org.eclipse.collections.api.block.predicate.primitive.FloatPredicate;
import org.eclipse.collections.api.block.procedure.primitive.FloatProcedure;
import org.eclipse.collections.api.collection.primitive.MutableFloatCollection;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.primitive.FloatFloatPair;
import org.eclipse.collections.api.tuple.primitive.FloatObjectPair;

/**
 * This file was automatically generated from template file mutablePrimitiveList.stg.
 *
 * @since 3.0.
 */
public interface MutableFloatList extends MutableFloatCollection, FloatList
{
    void addAtIndex(int index, float element);

    boolean addAllAtIndex(int index, float... source);

    boolean addAllAtIndex(int index, FloatIterable source);

    float removeAtIndex(int index);

    float set(int index, float element);

    default void swap(int index1, int index2)
    {
        float value = this.get(index1);
        this.set(index1, this.get(index2));
        this.set(index2, value);
    }

    @Override
    MutableFloatList select(FloatPredicate predicate);

    @Override
    MutableFloatList reject(FloatPredicate predicate);

    @Override
    MutableFloatList with(float element);

    @Override
    MutableFloatList without(float element);

    @Override
    MutableFloatList withAll(FloatIterable elements);

    @Override
    MutableFloatList withoutAll(FloatIterable elements);

    /**
     * @since 9.0.
     */
    @Override
    default MutableFloatList tap(FloatProcedure procedure)
    {
        this.forEach(procedure);
        return this;
    }

    /**
     * Returns a new MutableFloatList including all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    @Override
    default MutableFloatList selectWithIndex(FloatIntPredicate predicate)
    {
        int[] index = {0};
        return this.select(each -> predicate.accept(each, index[0]++));
    }

    /**
     * Returns a new MutableFloatList excluding all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    @Override
    default MutableFloatList rejectWithIndex(FloatIntPredicate predicate)
    {
        int[] index = {0};
        return this.reject(each -> predicate.accept(each, index[0]++));
    }

    <V> MutableList<V> collect(FloatToObjectFunction<? extends V> function);

    /**
     * Returns a new MutableList using results obtained by applying the specified function to each element
     * and its corresponding index.
     *
     * @since 9.1.
     */
    @Override
    default <V> MutableList<V> collectWithIndex(FloatIntToObjectFunction<? extends V> function)
    {
        int[] index = {0};
        return this.collect(each -> function.value(each, index[0]++));
    }

    MutableFloatList reverseThis();

    @Override
    MutableFloatList toReversed();

    /**
     * @since 6.0.
     */
    @Override
    MutableFloatList distinct();

    /**
     * Sorts this list mutating its contents and returns the same mutable list (this).
     */
    MutableFloatList sortThis();

    /**
     * Sorts the internal data structure of this list and returns the list itself as a convenience.
     */
    default MutableFloatList sortThis(FloatComparator comparator)
    {
        throw new UnsupportedOperationException("sortThis(FloatComparator comparator) is not supported on " + this.getClass());
    }

    /**
     * Sorts the internal data structure of this list based on the natural order of the key returned by {@code
     * function}.
     */
    default <T> MutableFloatList sortThisBy(FloatToObjectFunction<T> function)
    {
        return sortThisBy(function, (Comparator<? super T>) Comparator.naturalOrder());
    }

    /**
     * Sorts the internal data structure of this list based on the key returned by {@code
     * function} using the provided {@code comparator}.
     */
    default <T> MutableFloatList sortThisBy(FloatToObjectFunction<T> function, Comparator<? super T> comparator)
    {
        return this.sortThis((i1, i2) -> comparator.compare(function.valueOf(i1), function.valueOf(i2)));
    }

    /**
     * Randomly permutes this list mutating its contents and returns the same list (this).
     *
     * Uses {@code java.util.Random} as the source of randomness.
     */
    default MutableFloatList shuffleThis()
    {
        return this.shuffleThis(new Random());
    }

    /**
     * Randomly permutes this list mutating its contents and returns the same list (this).
     *
     * Implements the Fisher-Yates shuffle algorithm using the provided source of randomness.
     */
    default MutableFloatList shuffleThis(Random rnd)
    {
        for (int j = this.size() - 1; j > 0; j--)
        {
            int k = rnd.nextInt(j + 1);
            float selected = this.get(j);
            this.set(j, this.get(k));
            this.set(k, selected);
        }

        return this;
    }

    @Override
    MutableFloatList asUnmodifiable();

    @Override
    MutableFloatList asSynchronized();

    /**
     * Returns an immutable copy of this list.
     */
    @Override
    ImmutableFloatList toImmutable();

    @Override
    MutableFloatList subList(int fromIndex, int toIndex);

    /**
     * Returns a {@code MutableList} formed from this {@code MutableFloatList} and another {@code FloatList} by
     * combining corresponding elements in pairs. If one of the two {@code FloatList}s is longer than the other, its
     * remaining elements are ignored.
     *
     * @since 9.1.
     */
    default MutableList<FloatFloatPair> zipFloat(FloatIterable iterable)
    {
        throw new UnsupportedOperationException("Default method to prevent breaking backwards compatibility");
    }

    /**
     * Returns a {@code MutableList} formed from this {@code MutableFloatList} and a {@code ListIterable} by
     * combining corresponding elements in pairs. If one of the two Lists is longer than the other, its
     * remaining elements are ignored.
     *
     * @since 9.1.
     */
    default <T> MutableList<FloatObjectPair<T>> zip(Iterable<T> list)
    {
        throw new UnsupportedOperationException("Default method to prevent breaking backwards compatibility");
    }

    /**
     * Creates a new empty mutable version of the same List type.
     *
     * @since 9.2.
     */
    default MutableFloatList newEmpty()
    {
        throw new UnsupportedOperationException("Implement in concrete classes.");
    }
}
