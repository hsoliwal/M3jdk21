/*
 * Copyright (c) 2026 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.api.bag.primitive;

import jdk.m3.collections.eclipse.api.ShortIterable;
import jdk.m3.collections.eclipse.api.bag.MutableBag;
import jdk.m3.collections.eclipse.api.block.function.primitive.ShortToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.IntPredicate;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.ShortPredicate;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.ShortProcedure;
import jdk.m3.collections.eclipse.api.collection.primitive.MutableShortCollection;
import jdk.m3.collections.eclipse.api.list.MutableList;
import jdk.m3.collections.eclipse.api.tuple.primitive.ShortIntPair;
import jdk.m3.collections.eclipse.api.set.primitive.MutableShortSet;

/**
 * This file was automatically generated from template file mutablePrimitiveBag.stg.
 *
 * @since 3.0.
 */
public interface MutableShortBag extends MutableShortCollection, ShortBag
{
    void addOccurrences(short item, int occurrences);

    boolean removeOccurrences(short item, int occurrences);

    /**
     * @since 9.0.
     */
    @Override
    default MutableShortBag tap(ShortProcedure procedure)
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
    MutableShortBag selectByOccurrences(IntPredicate predicate);

    /**
     * Returns all elements of the bag that have more than one occurrence.
     *
     * @since 9.2
     */
    @Override
    default MutableShortBag selectDuplicates()
    {
        return this.selectByOccurrences(occurrences -> occurrences > 1);
    }

    /**
     * Returns all elements of the bag that have exactly one occurrence.
     *
     * @since 9.2
     */
    @Override
    MutableShortSet selectUnique();

    /**
     * Returns the {@code count} most frequently occurring items.
     *
     * In the event of a tie, all of the items with the number of occurrences that match the occurrences of the last
     * item will be returned.
     *
     * @since 8.0
     */
    @Override
    MutableList<ShortIntPair> topOccurrences(int count);

    /**
     * Returns the {@code count} least frequently occurring items.
     *
     * In the event of a tie, all of the items with the number of occurrences that match the occurrences of the last
     * item will be returned.
     *
     * @since 8.0
     */
    @Override
    MutableList<ShortIntPair> bottomOccurrences(int count);

    @Override
    MutableShortBag select(ShortPredicate predicate);

    @Override
    MutableShortBag reject(ShortPredicate predicate);

    @Override
    <V> MutableBag<V> collect(ShortToObjectFunction<? extends V> function);

    @Override
    MutableShortBag with(short element);

    @Override
    MutableShortBag without(short element);

    @Override
    MutableShortBag withAll(ShortIterable elements);

    @Override
    MutableShortBag withoutAll(ShortIterable elements);

    @Override
    MutableShortBag asUnmodifiable();

    @Override
    MutableShortBag asSynchronized();

    /**
     * Returns an immutable copy of this bag.
     */
    @Override
    ImmutableShortBag toImmutable();

    /**
     * Creates a new empty mutable version of the same Bag type.
     *
     * @since 9.2.
     */
    default MutableShortBag newEmpty()
    {
        throw new UnsupportedOperationException("Implement in concrete classes.");
    }
}
