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
import org.eclipse.collections.api.block.comparator.primitive.LongComparator;

import org.eclipse.collections.api.LongIterable;
import org.eclipse.collections.api.block.function.primitive.LongIntToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.LongToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.LongIntPredicate;
import org.eclipse.collections.api.block.predicate.primitive.LongPredicate;
import org.eclipse.collections.api.block.procedure.primitive.LongProcedure;
import org.eclipse.collections.api.collection.primitive.MutableLongCollection;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.primitive.LongLongPair;
import org.eclipse.collections.api.tuple.primitive.LongObjectPair;

/**
 * This file was automatically generated from template file mutablePrimitiveList.stg.
 *
 * @since 3.0.
 */
public interface MutableLongList extends MutableLongCollection, LongList
{
    void addAtIndex(int index, long element);

    boolean addAllAtIndex(int index, long... source);

    boolean addAllAtIndex(int index, LongIterable source);

    long removeAtIndex(int index);

    long set(int index, long element);

    default void swap(int index1, int index2)
    {
        long value = this.get(index1);
        this.set(index1, this.get(index2));
        this.set(index2, value);
    }

    @Override
    MutableLongList select(LongPredicate predicate);

    @Override
    MutableLongList reject(LongPredicate predicate);

    @Override
    MutableLongList with(long element);

    @Override
    MutableLongList without(long element);

    @Override
    MutableLongList withAll(LongIterable elements);

    @Override
    MutableLongList withoutAll(LongIterable elements);

    /**
     * @since 9.0.
     */
    @Override
    default MutableLongList tap(LongProcedure procedure)
    {
        this.forEach(procedure);
        return this;
    }

    /**
     * Returns a new MutableLongList including all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    @Override
    default MutableLongList selectWithIndex(LongIntPredicate predicate)
    {
        int[] index = {0};
        return this.select(each -> predicate.accept(each, index[0]++));
    }

    /**
     * Returns a new MutableLongList excluding all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    @Override
    default MutableLongList rejectWithIndex(LongIntPredicate predicate)
    {
        int[] index = {0};
        return this.reject(each -> predicate.accept(each, index[0]++));
    }

    <V> MutableList<V> collect(LongToObjectFunction<? extends V> function);

    /**
     * Returns a new MutableList using results obtained by applying the specified function to each element
     * and its corresponding index.
     *
     * @since 9.1.
     */
    @Override
    default <V> MutableList<V> collectWithIndex(LongIntToObjectFunction<? extends V> function)
    {
        int[] index = {0};
        return this.collect(each -> function.value(each, index[0]++));
    }

    MutableLongList reverseThis();

    @Override
    MutableLongList toReversed();

    /**
     * @since 6.0.
     */
    @Override
    MutableLongList distinct();

    /**
     * Sorts this list mutating its contents and returns the same mutable list (this).
     */
    MutableLongList sortThis();

    /**
     * Sorts the internal data structure of this list and returns the list itself as a convenience.
     */
    default MutableLongList sortThis(LongComparator comparator)
    {
        throw new UnsupportedOperationException("sortThis(LongComparator comparator) is not supported on " + this.getClass());
    }

    /**
     * Sorts the internal data structure of this list based on the natural order of the key returned by {@code
     * function}.
     */
    default <T> MutableLongList sortThisBy(LongToObjectFunction<T> function)
    {
        return sortThisBy(function, (Comparator<? super T>) Comparator.naturalOrder());
    }

    /**
     * Sorts the internal data structure of this list based on the key returned by {@code
     * function} using the provided {@code comparator}.
     */
    default <T> MutableLongList sortThisBy(LongToObjectFunction<T> function, Comparator<? super T> comparator)
    {
        return this.sortThis((i1, i2) -> comparator.compare(function.valueOf(i1), function.valueOf(i2)));
    }

    /**
     * Randomly permutes this list mutating its contents and returns the same list (this).
     *
     * Uses {@code java.util.Random} as the source of randomness.
     */
    default MutableLongList shuffleThis()
    {
        return this.shuffleThis(new Random());
    }

    /**
     * Randomly permutes this list mutating its contents and returns the same list (this).
     *
     * Implements the Fisher-Yates shuffle algorithm using the provided source of randomness.
     */
    default MutableLongList shuffleThis(Random rnd)
    {
        for (int j = this.size() - 1; j > 0; j--)
        {
            int k = rnd.nextInt(j + 1);
            long selected = this.get(j);
            this.set(j, this.get(k));
            this.set(k, selected);
        }

        return this;
    }

    @Override
    MutableLongList asUnmodifiable();

    @Override
    MutableLongList asSynchronized();

    /**
     * Returns an immutable copy of this list.
     */
    @Override
    ImmutableLongList toImmutable();

    @Override
    MutableLongList subList(int fromIndex, int toIndex);

    /**
     * Returns a {@code MutableList} formed from this {@code MutableLongList} and another {@code LongList} by
     * combining corresponding elements in pairs. If one of the two {@code LongList}s is longer than the other, its
     * remaining elements are ignored.
     *
     * @since 9.1.
     */
    default MutableList<LongLongPair> zipLong(LongIterable iterable)
    {
        throw new UnsupportedOperationException("Default method to prevent breaking backwards compatibility");
    }

    /**
     * Returns a {@code MutableList} formed from this {@code MutableLongList} and a {@code ListIterable} by
     * combining corresponding elements in pairs. If one of the two Lists is longer than the other, its
     * remaining elements are ignored.
     *
     * @since 9.1.
     */
    default <T> MutableList<LongObjectPair<T>> zip(Iterable<T> list)
    {
        throw new UnsupportedOperationException("Default method to prevent breaking backwards compatibility");
    }

    /**
     * Creates a new empty mutable version of the same List type.
     *
     * @since 9.2.
     */
    default MutableLongList newEmpty()
    {
        throw new UnsupportedOperationException("Implement in concrete classes.");
    }
}
