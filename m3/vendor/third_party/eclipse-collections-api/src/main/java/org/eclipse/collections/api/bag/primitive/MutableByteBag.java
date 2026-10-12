/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.api.bag.primitive;

import org.eclipse.collections.api.ByteIterable;
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.block.function.primitive.ByteToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.IntPredicate;
import org.eclipse.collections.api.block.predicate.primitive.BytePredicate;
import org.eclipse.collections.api.block.procedure.primitive.ByteProcedure;
import org.eclipse.collections.api.collection.primitive.MutableByteCollection;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.primitive.ByteIntPair;
import org.eclipse.collections.api.set.primitive.MutableByteSet;

/**
 * This file was automatically generated from template file mutablePrimitiveBag.stg.
 *
 * @since 3.0.
 */
public interface MutableByteBag extends MutableByteCollection, ByteBag
{
    void addOccurrences(byte item, int occurrences);

    boolean removeOccurrences(byte item, int occurrences);

    /**
     * @since 9.0.
     */
    @Override
    default MutableByteBag tap(ByteProcedure procedure)
    {
        this.forEach(procedure);
        return this;
    }

    /**
     * Returns all elements of the bag that have a number of occurrences that satisfy the predicate.
     *
     * @since 8.0
     */
    @Override
    MutableByteBag selectByOccurrences(IntPredicate predicate);

    /**
     * Returns all elements of the bag that have more than one occurrence.
     *
     * @since 9.2
     */
    @Override
    default MutableByteBag selectDuplicates()
    {
        return this.selectByOccurrences(occurrences -> occurrences > 1);
    }

    /**
     * Returns all elements of the bag that have exactly one occurrence.
     *
     * @since 9.2
     */
    @Override
    MutableByteSet selectUnique();

    /**
     * Returns the {@code count} most frequently occurring items.
     *
     * In the event of a tie, all of the items with the number of occurrences that match the occurrences of the last
     * item will be returned.
     *
     * @since 8.0
     */
    @Override
    MutableList<ByteIntPair> topOccurrences(int count);

    /**
     * Returns the {@code count} least frequently occurring items.
     *
     * In the event of a tie, all of the items with the number of occurrences that match the occurrences of the last
     * item will be returned.
     *
     * @since 8.0
     */
    @Override
    MutableList<ByteIntPair> bottomOccurrences(int count);

    @Override
    MutableByteBag select(BytePredicate predicate);

    @Override
    MutableByteBag reject(BytePredicate predicate);

    @Override
    <V> MutableBag<V> collect(ByteToObjectFunction<? extends V> function);

    @Override
    MutableByteBag with(byte element);

    @Override
    MutableByteBag without(byte element);

    @Override
    MutableByteBag withAll(ByteIterable elements);

    @Override
    MutableByteBag withoutAll(ByteIterable elements);

    @Override
    MutableByteBag asUnmodifiable();

    @Override
    MutableByteBag asSynchronized();

    /**
     * Returns an immutable copy of this bag.
     */
    @Override
    ImmutableByteBag toImmutable();

    /**
     * Creates a new empty mutable version of the same Bag type.
     *
     * @since 9.2.
     */
    default MutableByteBag newEmpty()
    {
        throw new UnsupportedOperationException("Implement in concrete classes.");
    }
}
