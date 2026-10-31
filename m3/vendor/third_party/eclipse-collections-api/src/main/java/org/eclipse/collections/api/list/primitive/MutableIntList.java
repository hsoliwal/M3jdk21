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
import org.eclipse.collections.api.block.comparator.primitive.IntComparator;

import org.eclipse.collections.api.IntIterable;
import org.eclipse.collections.api.block.function.primitive.IntIntToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.IntToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.IntIntPredicate;
import org.eclipse.collections.api.block.predicate.primitive.IntPredicate;
import org.eclipse.collections.api.block.procedure.primitive.IntProcedure;
import org.eclipse.collections.api.collection.primitive.MutableIntCollection;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.primitive.IntIntPair;
import org.eclipse.collections.api.tuple.primitive.IntObjectPair;

/**
 * This file was automatically generated from template file mutablePrimitiveList.stg.
 *
 * @since 3.0.
 */
public interface MutableIntList extends MutableIntCollection, IntList
{
    void addAtIndex(int index, int element);

    boolean addAllAtIndex(int index, int... source);

    boolean addAllAtIndex(int index, IntIterable source);

    int removeAtIndex(int index);

    int set(int index, int element);

    default void swap(int index1, int index2)
    {
        int value = this.get(index1);
        this.set(index1, this.get(index2));
        this.set(index2, value);
    }

    @Override
    MutableIntList select(IntPredicate predicate);

    @Override
    MutableIntList reject(IntPredicate predicate);

    @Override
    MutableIntList with(int element);

    @Override
    MutableIntList without(int element);

    @Override
    MutableIntList withAll(IntIterable elements);

    @Override
    MutableIntList withoutAll(IntIterable elements);

    /**
     * @since 9.0.
     */
    @Override
    default MutableIntList tap(IntProcedure procedure)
    {
        this.forEach(procedure);
        return this;
    }

    /**
     * Returns a new MutableIntList including all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    @Override
    default MutableIntList selectWithIndex(IntIntPredicate predicate)
    {
        int[] index = {0};
        return this.select(each -> predicate.accept(each, index[0]++));
    }

    /**
     * Returns a new MutableIntList excluding all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    @Override
    default MutableIntList rejectWithIndex(IntIntPredicate predicate)
    {
        int[] index = {0};
        return this.reject(each -> predicate.accept(each, index[0]++));
    }

    <V> MutableList<V> collect(IntToObjectFunction<? extends V> function);

    /**
     * Returns a new MutableList using results obtained by applying the specified function to each element
     * and its corresponding index.
     *
     * @since 9.1.
     */
    @Override
    default <V> MutableList<V> collectWithIndex(IntIntToObjectFunction<? extends V> function)
    {
        int[] index = {0};
        return this.collect(each -> function.value(each, index[0]++));
    }

    MutableIntList reverseThis();

    @Override
    MutableIntList toReversed();

    /**
     * @since 6.0.
     */
    @Override
    MutableIntList distinct();

    /**
     * Sorts this list mutating its contents and returns the same mutable list (this).
     */
    MutableIntList sortThis();

    /**
     * Sorts the internal data structure of this list and returns the list itself as a convenience.
     */
    default MutableIntList sortThis(IntComparator comparator)
    {
        throw new UnsupportedOperationException("sortThis(IntComparator comparator) is not supported on " + this.getClass());
    }

    /**
     * Sorts the internal data structure of this list based on the natural order of the key returned by {@code
     * function}.
     */
    default <T> MutableIntList sortThisBy(IntToObjectFunction<T> function)
    {
        return sortThisBy(function, (Comparator<? super T>) Comparator.naturalOrder());
    }

    /**
     * Sorts the internal data structure of this list based on the key returned by {@code
     * function} using the provided {@code comparator}.
     */
    default <T> MutableIntList sortThisBy(IntToObjectFunction<T> function, Comparator<? super T> comparator)
    {
        return this.sortThis((i1, i2) -> comparator.compare(function.valueOf(i1), function.valueOf(i2)));
    }

    /**
     * Randomly permutes this list mutating its contents and returns the same list (this).
     *
     * Uses {@code java.util.Random} as the source of randomness.
     */
    default MutableIntList shuffleThis()
    {
        return this.shuffleThis(new Random());
    }

    /**
     * Randomly permutes this list mutating its contents and returns the same list (this).
     *
     * Implements the Fisher-Yates shuffle algorithm using the provided source of randomness.
     */
    default MutableIntList shuffleThis(Random rnd)
    {
        for (int j = this.size() - 1; j > 0; j--)
        {
            int k = rnd.nextInt(j + 1);
            int selected = this.get(j);
            this.set(j, this.get(k));
            this.set(k, selected);
        }

        return this;
    }

    @Override
    MutableIntList asUnmodifiable();

    @Override
    MutableIntList asSynchronized();

    /**
     * Returns an immutable copy of this list.
     */
    @Override
    ImmutableIntList toImmutable();

    @Override
    MutableIntList subList(int fromIndex, int toIndex);

    /**
     * Returns a {@code MutableList} formed from this {@code MutableIntList} and another {@code IntList} by
     * combining corresponding elements in pairs. If one of the two {@code IntList}s is longer than the other, its
     * remaining elements are ignored.
     *
     * @since 9.1.
     */
    default MutableList<IntIntPair> zipInt(IntIterable iterable)
    {
        throw new UnsupportedOperationException("Default method to prevent breaking backwards compatibility");
    }

    /**
     * Returns a {@code MutableList} formed from this {@code MutableIntList} and a {@code ListIterable} by
     * combining corresponding elements in pairs. If one of the two Lists is longer than the other, its
     * remaining elements are ignored.
     *
     * @since 9.1.
     */
    default <T> MutableList<IntObjectPair<T>> zip(Iterable<T> list)
    {
        throw new UnsupportedOperationException("Default method to prevent breaking backwards compatibility");
    }

    /**
     * Creates a new empty mutable version of the same List type.
     *
     * @since 9.2.
     */
    default MutableIntList newEmpty()
    {
        throw new UnsupportedOperationException("Implement in concrete classes.");
    }
}
