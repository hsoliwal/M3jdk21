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

import org.eclipse.collections.api.IntIterable;
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.block.function.primitive.IntToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.IntPredicate;
import org.eclipse.collections.api.block.procedure.primitive.IntProcedure;
import org.eclipse.collections.api.collection.primitive.MutableIntCollection;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.primitive.IntIntPair;
import org.eclipse.collections.api.set.primitive.MutableIntSet;

/**
 * This file was automatically generated from template file mutablePrimitiveBag.stg.
 *
 * @since 3.0.
 */
public interface MutableIntBag extends MutableIntCollection, IntBag
{
    void addOccurrences(int item, int occurrences);

    boolean removeOccurrences(int item, int occurrences);

    /**
     * @since 9.0.
     */
    @Override
    default MutableIntBag tap(IntProcedure procedure)
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
    MutableIntBag selectByOccurrences(IntPredicate predicate);

    /**
     * Returns all elements of the bag that have more than one occurrence.
     *
     * @since 9.2
     */
    @Override
    default MutableIntBag selectDuplicates()
    {
        return this.selectByOccurrences(occurrences -> occurrences > 1);
    }

    /**
     * Returns all elements of the bag that have exactly one occurrence.
     *
     * @since 9.2
     */
    @Override
    MutableIntSet selectUnique();

    /**
     * Returns the {@code count} most frequently occurring items.
     *
     * In the event of a tie, all of the items with the number of occurrences that match the occurrences of the last
     * item will be returned.
     *
     * @since 8.0
     */
    @Override
    MutableList<IntIntPair> topOccurrences(int count);

    /**
     * Returns the {@code count} least frequently occurring items.
     *
     * In the event of a tie, all of the items with the number of occurrences that match the occurrences of the last
     * item will be returned.
     *
     * @since 8.0
     */
    @Override
    MutableList<IntIntPair> bottomOccurrences(int count);

    @Override
    MutableIntBag select(IntPredicate predicate);

    @Override
    MutableIntBag reject(IntPredicate predicate);

    @Override
    <V> MutableBag<V> collect(IntToObjectFunction<? extends V> function);

    @Override
    MutableIntBag with(int element);

    @Override
    MutableIntBag without(int element);

    @Override
    MutableIntBag withAll(IntIterable elements);

    @Override
    MutableIntBag withoutAll(IntIterable elements);

    @Override
    MutableIntBag asUnmodifiable();

    @Override
    MutableIntBag asSynchronized();

    /**
     * Returns an immutable copy of this bag.
     */
    @Override
    ImmutableIntBag toImmutable();

    /**
     * Creates a new empty mutable version of the same Bag type.
     *
     * @since 9.2.
     */
    default MutableIntBag newEmpty()
    {
        throw new UnsupportedOperationException("Implement in concrete classes.");
    }
}
