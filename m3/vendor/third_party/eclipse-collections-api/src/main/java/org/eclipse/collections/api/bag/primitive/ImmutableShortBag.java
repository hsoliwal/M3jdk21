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

import org.eclipse.collections.api.ShortIterable;
import org.eclipse.collections.api.bag.ImmutableBag;
import org.eclipse.collections.api.block.function.primitive.ShortToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.IntPredicate;
import org.eclipse.collections.api.block.predicate.primitive.ShortPredicate;
import org.eclipse.collections.api.block.procedure.primitive.ShortProcedure;
import org.eclipse.collections.api.collection.primitive.ImmutableShortCollection;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.tuple.primitive.ShortIntPair;
import org.eclipse.collections.api.set.primitive.ImmutableShortSet;

/**
 * This file was automatically generated from template file immutablePrimitiveBag.stg.
 *
 * @since 3.0.
 */
public interface ImmutableShortBag extends ImmutableShortCollection, ShortBag
{
    @Override
    ImmutableShortBag select(ShortPredicate predicate);

    @Override
    ImmutableShortBag reject(ShortPredicate predicate);

    /**
     * @since 9.0.
     */
    @Override
    default ImmutableShortBag tap(ShortProcedure procedure)
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
    ImmutableShortBag selectByOccurrences(IntPredicate predicate);

    /**
     * Returns all elements of the bag that have more than one occurrence.
     *
     * @since 9.2
     */
    @Override
    default ImmutableShortBag selectDuplicates()
    {
        return this.selectByOccurrences(occurrences -> occurrences > 1);
    }

    /**
     * Returns all elements of the bag that have exactly one occurrence.
     *
     * @since 9.2
     */
    @Override
    ImmutableShortSet selectUnique();

    /**
     * Returns the {@code count} most frequently occurring items.
     *
     * In the event of a tie, all of the items with the number of occurrences that match the occurrences of the last
     * item will be returned.
     *
     * @since 8.0
     */
    @Override
    ImmutableList<ShortIntPair> topOccurrences(int count);

    /**
     * Returns the {@code count} least frequently occurring items.
     *
     * In the event of a tie, all of the items with the number of occurrences that match the occurrences of the last
     * item will be returned.
     *
     * @since 8.0
     */
    @Override
    ImmutableList<ShortIntPair> bottomOccurrences(int count);

    @Override
    <V> ImmutableBag<V> collect(ShortToObjectFunction<? extends V> function);

    @Override
    ImmutableShortBag newWith(short element);

    @Override
    ImmutableShortBag newWithout(short element);

    @Override
    ImmutableShortBag newWithAll(ShortIterable elements);

    @Override
    ImmutableShortBag newWithoutAll(ShortIterable elements);
}
