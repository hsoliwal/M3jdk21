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

import org.eclipse.collections.api.BooleanIterable;
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.block.function.primitive.BooleanToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.IntPredicate;
import org.eclipse.collections.api.block.predicate.primitive.BooleanPredicate;
import org.eclipse.collections.api.block.procedure.primitive.BooleanProcedure;
import org.eclipse.collections.api.collection.primitive.MutableBooleanCollection;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.primitive.BooleanIntPair;
import org.eclipse.collections.api.set.primitive.MutableBooleanSet;

/**
 * This file was automatically generated from template file mutablePrimitiveBag.stg.
 *
 * @since 3.0.
 */
public interface MutableBooleanBag extends MutableBooleanCollection, BooleanBag
{
    void addOccurrences(boolean item, int occurrences);

    boolean removeOccurrences(boolean item, int occurrences);

    /**
     * @since 9.0.
     */
    @Override
    default MutableBooleanBag tap(BooleanProcedure procedure)
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
    MutableBooleanBag selectByOccurrences(IntPredicate predicate);

    /**
     * Returns all elements of the bag that have more than one occurrence.
     *
     * @since 9.2
     */
    @Override
    default MutableBooleanBag selectDuplicates()
    {
        return this.selectByOccurrences(occurrences -> occurrences > 1);
    }

    /**
     * Returns all elements of the bag that have exactly one occurrence.
     *
     * @since 9.2
     */
    @Override
    MutableBooleanSet selectUnique();

    /**
     * Returns the {@code count} most frequently occurring items.
     *
     * In the event of a tie, all of the items with the number of occurrences that match the occurrences of the last
     * item will be returned.
     *
     * @since 8.0
     */
    @Override
    MutableList<BooleanIntPair> topOccurrences(int count);

    /**
     * Returns the {@code count} least frequently occurring items.
     *
     * In the event of a tie, all of the items with the number of occurrences that match the occurrences of the last
     * item will be returned.
     *
     * @since 8.0
     */
    @Override
    MutableList<BooleanIntPair> bottomOccurrences(int count);

    @Override
    MutableBooleanBag select(BooleanPredicate predicate);

    @Override
    MutableBooleanBag reject(BooleanPredicate predicate);

    @Override
    <V> MutableBag<V> collect(BooleanToObjectFunction<? extends V> function);

    @Override
    MutableBooleanBag with(boolean element);

    @Override
    MutableBooleanBag without(boolean element);

    @Override
    MutableBooleanBag withAll(BooleanIterable elements);

    @Override
    MutableBooleanBag withoutAll(BooleanIterable elements);

    @Override
    MutableBooleanBag asUnmodifiable();

    @Override
    MutableBooleanBag asSynchronized();

    /**
     * Returns an immutable copy of this bag.
     */
    @Override
    ImmutableBooleanBag toImmutable();

    /**
     * Creates a new empty mutable version of the same Bag type.
     *
     * @since 9.2.
     */
    default MutableBooleanBag newEmpty()
    {
        throw new UnsupportedOperationException("Implement in concrete classes.");
    }
}
