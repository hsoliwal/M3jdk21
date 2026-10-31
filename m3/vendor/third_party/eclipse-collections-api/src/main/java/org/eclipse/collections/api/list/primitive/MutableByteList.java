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
import org.eclipse.collections.api.block.comparator.primitive.ByteComparator;

import org.eclipse.collections.api.ByteIterable;
import org.eclipse.collections.api.block.function.primitive.ByteIntToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ByteToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.ByteIntPredicate;
import org.eclipse.collections.api.block.predicate.primitive.BytePredicate;
import org.eclipse.collections.api.block.procedure.primitive.ByteProcedure;
import org.eclipse.collections.api.collection.primitive.MutableByteCollection;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.primitive.ByteBytePair;
import org.eclipse.collections.api.tuple.primitive.ByteObjectPair;

/**
 * This file was automatically generated from template file mutablePrimitiveList.stg.
 *
 * @since 3.0.
 */
public interface MutableByteList extends MutableByteCollection, ByteList
{
    void addAtIndex(int index, byte element);

    boolean addAllAtIndex(int index, byte... source);

    boolean addAllAtIndex(int index, ByteIterable source);

    byte removeAtIndex(int index);

    byte set(int index, byte element);

    default void swap(int index1, int index2)
    {
        byte value = this.get(index1);
        this.set(index1, this.get(index2));
        this.set(index2, value);
    }

    @Override
    MutableByteList select(BytePredicate predicate);

    @Override
    MutableByteList reject(BytePredicate predicate);

    @Override
    MutableByteList with(byte element);

    @Override
    MutableByteList without(byte element);

    @Override
    MutableByteList withAll(ByteIterable elements);

    @Override
    MutableByteList withoutAll(ByteIterable elements);

    /**
     * @since 9.0.
     */
    @Override
    default MutableByteList tap(ByteProcedure procedure)
    {
        this.forEach(procedure);
        return this;
    }

    /**
     * Returns a new MutableByteList including all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    @Override
    default MutableByteList selectWithIndex(ByteIntPredicate predicate)
    {
        int[] index = {0};
        return this.select(each -> predicate.accept(each, index[0]++));
    }

    /**
     * Returns a new MutableByteList excluding all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    @Override
    default MutableByteList rejectWithIndex(ByteIntPredicate predicate)
    {
        int[] index = {0};
        return this.reject(each -> predicate.accept(each, index[0]++));
    }

    <V> MutableList<V> collect(ByteToObjectFunction<? extends V> function);

    /**
     * Returns a new MutableList using results obtained by applying the specified function to each element
     * and its corresponding index.
     *
     * @since 9.1.
     */
    @Override
    default <V> MutableList<V> collectWithIndex(ByteIntToObjectFunction<? extends V> function)
    {
        int[] index = {0};
        return this.collect(each -> function.value(each, index[0]++));
    }

    MutableByteList reverseThis();

    @Override
    MutableByteList toReversed();

    /**
     * @since 6.0.
     */
    @Override
    MutableByteList distinct();

    /**
     * Sorts this list mutating its contents and returns the same mutable list (this).
     */
    MutableByteList sortThis();

    /**
     * Sorts the internal data structure of this list and returns the list itself as a convenience.
     */
    default MutableByteList sortThis(ByteComparator comparator)
    {
        throw new UnsupportedOperationException("sortThis(ByteComparator comparator) is not supported on " + this.getClass());
    }

    /**
     * Sorts the internal data structure of this list based on the natural order of the key returned by {@code
     * function}.
     */
    default <T> MutableByteList sortThisBy(ByteToObjectFunction<T> function)
    {
        return sortThisBy(function, (Comparator<? super T>) Comparator.naturalOrder());
    }

    /**
     * Sorts the internal data structure of this list based on the key returned by {@code
     * function} using the provided {@code comparator}.
     */
    default <T> MutableByteList sortThisBy(ByteToObjectFunction<T> function, Comparator<? super T> comparator)
    {
        return this.sortThis((i1, i2) -> comparator.compare(function.valueOf(i1), function.valueOf(i2)));
    }

    /**
     * Randomly permutes this list mutating its contents and returns the same list (this).
     *
     * Uses {@code java.util.Random} as the source of randomness.
     */
    default MutableByteList shuffleThis()
    {
        return this.shuffleThis(new Random());
    }

    /**
     * Randomly permutes this list mutating its contents and returns the same list (this).
     *
     * Implements the Fisher-Yates shuffle algorithm using the provided source of randomness.
     */
    default MutableByteList shuffleThis(Random rnd)
    {
        for (int j = this.size() - 1; j > 0; j--)
        {
            int k = rnd.nextInt(j + 1);
            byte selected = this.get(j);
            this.set(j, this.get(k));
            this.set(k, selected);
        }

        return this;
    }

    @Override
    MutableByteList asUnmodifiable();

    @Override
    MutableByteList asSynchronized();

    /**
     * Returns an immutable copy of this list.
     */
    @Override
    ImmutableByteList toImmutable();

    @Override
    MutableByteList subList(int fromIndex, int toIndex);

    /**
     * Returns a {@code MutableList} formed from this {@code MutableByteList} and another {@code ByteList} by
     * combining corresponding elements in pairs. If one of the two {@code ByteList}s is longer than the other, its
     * remaining elements are ignored.
     *
     * @since 9.1.
     */
    default MutableList<ByteBytePair> zipByte(ByteIterable iterable)
    {
        throw new UnsupportedOperationException("Default method to prevent breaking backwards compatibility");
    }

    /**
     * Returns a {@code MutableList} formed from this {@code MutableByteList} and a {@code ListIterable} by
     * combining corresponding elements in pairs. If one of the two Lists is longer than the other, its
     * remaining elements are ignored.
     *
     * @since 9.1.
     */
    default <T> MutableList<ByteObjectPair<T>> zip(Iterable<T> list)
    {
        throw new UnsupportedOperationException("Default method to prevent breaking backwards compatibility");
    }

    /**
     * Creates a new empty mutable version of the same List type.
     *
     * @since 9.2.
     */
    default MutableByteList newEmpty()
    {
        throw new UnsupportedOperationException("Implement in concrete classes.");
    }
}
