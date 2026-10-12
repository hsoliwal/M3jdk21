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
import org.eclipse.collections.api.block.comparator.primitive.CharComparator;

import org.eclipse.collections.api.CharIterable;
import org.eclipse.collections.api.block.function.primitive.CharIntToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.CharToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.CharIntPredicate;
import org.eclipse.collections.api.block.predicate.primitive.CharPredicate;
import org.eclipse.collections.api.block.procedure.primitive.CharProcedure;
import org.eclipse.collections.api.collection.primitive.MutableCharCollection;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.primitive.CharCharPair;
import org.eclipse.collections.api.tuple.primitive.CharObjectPair;

/**
 * This file was automatically generated from template file mutablePrimitiveList.stg.
 *
 * @since 3.0.
 */
public interface MutableCharList extends MutableCharCollection, CharList
{
    void addAtIndex(int index, char element);

    boolean addAllAtIndex(int index, char... source);

    boolean addAllAtIndex(int index, CharIterable source);

    char removeAtIndex(int index);

    char set(int index, char element);

    default void swap(int index1, int index2)
    {
        char value = this.get(index1);
        this.set(index1, this.get(index2));
        this.set(index2, value);
    }

    @Override
    MutableCharList select(CharPredicate predicate);

    @Override
    MutableCharList reject(CharPredicate predicate);

    @Override
    MutableCharList with(char element);

    @Override
    MutableCharList without(char element);

    @Override
    MutableCharList withAll(CharIterable elements);

    @Override
    MutableCharList withoutAll(CharIterable elements);

    /**
     * @since 9.0.
     */
    @Override
    default MutableCharList tap(CharProcedure procedure)
    {
        this.forEach(procedure);
        return this;
    }

    /**
     * Returns a new MutableCharList including all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    @Override
    default MutableCharList selectWithIndex(CharIntPredicate predicate)
    {
        int[] index = {0};
        return this.select(each -> predicate.accept(each, index[0]++));
    }

    /**
     * Returns a new MutableCharList excluding all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    @Override
    default MutableCharList rejectWithIndex(CharIntPredicate predicate)
    {
        int[] index = {0};
        return this.reject(each -> predicate.accept(each, index[0]++));
    }

    <V> MutableList<V> collect(CharToObjectFunction<? extends V> function);

    /**
     * Returns a new MutableList using results obtained by applying the specified function to each element
     * and its corresponding index.
     *
     * @since 9.1.
     */
    @Override
    default <V> MutableList<V> collectWithIndex(CharIntToObjectFunction<? extends V> function)
    {
        int[] index = {0};
        return this.collect(each -> function.value(each, index[0]++));
    }

    MutableCharList reverseThis();

    @Override
    MutableCharList toReversed();

    /**
     * @since 6.0.
     */
    @Override
    MutableCharList distinct();

    /**
     * Sorts this list mutating its contents and returns the same mutable list (this).
     */
    MutableCharList sortThis();

    /**
     * Sorts the internal data structure of this list and returns the list itself as a convenience.
     */
    default MutableCharList sortThis(CharComparator comparator)
    {
        throw new UnsupportedOperationException("sortThis(CharComparator comparator) is not supported on " + this.getClass());
    }

    /**
     * Sorts the internal data structure of this list based on the natural order of the key returned by {@code
     * function}.
     */
    default <T> MutableCharList sortThisBy(CharToObjectFunction<T> function)
    {
        return sortThisBy(function, (Comparator<? super T>) Comparator.naturalOrder());
    }

    /**
     * Sorts the internal data structure of this list based on the key returned by {@code
     * function} using the provided {@code comparator}.
     */
    default <T> MutableCharList sortThisBy(CharToObjectFunction<T> function, Comparator<? super T> comparator)
    {
        return this.sortThis((i1, i2) -> comparator.compare(function.valueOf(i1), function.valueOf(i2)));
    }

    /**
     * Randomly permutes this list mutating its contents and returns the same list (this).
     *
     * Uses {@code java.util.Random} as the source of randomness.
     */
    default MutableCharList shuffleThis()
    {
        return this.shuffleThis(new Random());
    }

    /**
     * Randomly permutes this list mutating its contents and returns the same list (this).
     *
     * Implements the Fisher-Yates shuffle algorithm using the provided source of randomness.
     */
    default MutableCharList shuffleThis(Random rnd)
    {
        for (int j = this.size() - 1; j > 0; j--)
        {
            int k = rnd.nextInt(j + 1);
            char selected = this.get(j);
            this.set(j, this.get(k));
            this.set(k, selected);
        }

        return this;
    }

    @Override
    MutableCharList asUnmodifiable();

    @Override
    MutableCharList asSynchronized();

    /**
     * Returns an immutable copy of this list.
     */
    @Override
    ImmutableCharList toImmutable();

    @Override
    MutableCharList subList(int fromIndex, int toIndex);

    /**
     * Returns a {@code MutableList} formed from this {@code MutableCharList} and another {@code CharList} by
     * combining corresponding elements in pairs. If one of the two {@code CharList}s is longer than the other, its
     * remaining elements are ignored.
     *
     * @since 9.1.
     */
    default MutableList<CharCharPair> zipChar(CharIterable iterable)
    {
        throw new UnsupportedOperationException("Default method to prevent breaking backwards compatibility");
    }

    /**
     * Returns a {@code MutableList} formed from this {@code MutableCharList} and a {@code ListIterable} by
     * combining corresponding elements in pairs. If one of the two Lists is longer than the other, its
     * remaining elements are ignored.
     *
     * @since 9.1.
     */
    default <T> MutableList<CharObjectPair<T>> zip(Iterable<T> list)
    {
        throw new UnsupportedOperationException("Default method to prevent breaking backwards compatibility");
    }

    /**
     * Creates a new empty mutable version of the same List type.
     *
     * @since 9.2.
     */
    default MutableCharList newEmpty()
    {
        throw new UnsupportedOperationException("Implement in concrete classes.");
    }
}
