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

import org.eclipse.collections.api.ByteIterable;
import org.eclipse.collections.api.block.function.primitive.ByteIntToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ByteToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.ByteIntPredicate;
import org.eclipse.collections.api.block.predicate.primitive.BytePredicate;
import org.eclipse.collections.api.block.procedure.primitive.ByteByteProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ByteProcedure;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.ordered.primitive.ReversibleByteIterable;
import org.eclipse.collections.api.tuple.primitive.ByteBytePair;
import org.eclipse.collections.api.tuple.primitive.ByteObjectPair;

import java.util.Objects;

/**
 * This file was automatically generated from template file primitiveList.stg.
 *
 * @since 3.0.
 */
public interface ByteList extends ReversibleByteIterable
{
    byte get(int index);

    long dotProduct(ByteList list);

    int binarySearch(byte value);

    int lastIndexOf(byte value);

    @Override
    ByteList select(BytePredicate predicate);

    @Override
    ByteList reject(BytePredicate predicate);

    /**
     * @since 9.0.
     */
    @Override
    default ByteList tap(ByteProcedure procedure)
    {
        this.forEach(procedure);
        return this;
    }

    /**
     * This method iterates over two CharList instances of the same size together using the specified CharCharProcedure.
     *
     * @since 11.1
     */
    default void forEachInBoth(ByteList other, ByteByteProcedure procedure)
    {
        Objects.requireNonNull(other);
        if (this.size() == other.size())
        {
            this.forEachWithIndex((each, index) -> procedure.value(each, other.get(index)));
        }
        else
        {
            throw new IllegalArgumentException("Attempt to call forEachInBoth with two ByteList instances of different sizes :"
                    + this.size()
                    + ':'
                    + other.size());
        }
    }

    /**
     * Returns a new ByteList including all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    @Override
    default ByteList selectWithIndex(ByteIntPredicate predicate)
    {
        int[] index = {0};
        return this.select(each -> predicate.accept(each, index[0]++));
    }

    /**
     * Returns a new ByteList excluding all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    @Override
    default ByteList rejectWithIndex(ByteIntPredicate predicate)
    {
        int[] index = {0};
        return this.reject(each -> predicate.accept(each, index[0]++));
    }

    @Override
    <V> ListIterable<V> collect(ByteToObjectFunction<? extends V> function);

    /**
     * Returns a new ListIterable using results obtained by applying the specified function to each element
     * and its corresponding index.
     *
     * @since 9.1.
     */
    @Override
    default <V> ListIterable<V> collectWithIndex(ByteIntToObjectFunction<? extends V> function)
    {
        int[] index = {0};
        return this.collect(each -> function.value(each, index[0]++));
    }

    /**
     * Follows the same general contract as {@link java.util.List#equals(Object)}.
     */
    @Override
    boolean equals(Object o);

    /**
     * Follows the same general contract as {@link java.util.List#hashCode()}.
     */
    @Override
    int hashCode();

    /**
     * Returns an immutable copy of this list. If the list is immutable, it returns itself.
     */
    ImmutableByteList toImmutable();

    /**
     * @since 6.0.
     */
    @Override
    ByteList distinct();

    /**
     * @since 5.0.
     */
    @Override
    ByteList toReversed();

    /**
     * @see java.util.List#subList(int fromIndex, int toIndex)
     * @since 5.0.
     */
    ByteList subList(int fromIndex, int toIndex);

    /**
     * Returns a {@code ListIterable} formed from this {@code ByteList} and another {@code ByteList} by
     * combining corresponding elements in pairs. If one of the two {@code ByteList}s is longer than the other, its
     * remaining elements are ignored.
     *
     * @since 9.1.
     */
    default ListIterable<ByteBytePair> zipByte(ByteIterable iterable)
    {
        throw new UnsupportedOperationException("Default method to prevent breaking backwards compatibility");
    }

    /**
     * Returns a {@code ListIterable} formed from this {@code ByteList} and a {@code ListIterable} by
     * combining corresponding elements in pairs. If one of the two Lists is longer than the other, its
     * remaining elements are ignored.
     *
     * @since 9.1.
     */
    default <T> ListIterable<ByteObjectPair<T>> zip(Iterable<T> iterable)
    {
        throw new UnsupportedOperationException("Default method to prevent breaking backwards compatibility");
    }
}
