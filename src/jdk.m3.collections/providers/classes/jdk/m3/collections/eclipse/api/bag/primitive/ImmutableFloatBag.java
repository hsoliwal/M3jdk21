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

import jdk.m3.collections.eclipse.api.FloatIterable;
import jdk.m3.collections.eclipse.api.bag.ImmutableBag;
import jdk.m3.collections.eclipse.api.block.function.primitive.FloatToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.IntPredicate;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.FloatPredicate;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.FloatProcedure;
import jdk.m3.collections.eclipse.api.collection.primitive.ImmutableFloatCollection;
import jdk.m3.collections.eclipse.api.list.ImmutableList;
import jdk.m3.collections.eclipse.api.tuple.primitive.FloatIntPair;
import jdk.m3.collections.eclipse.api.set.primitive.ImmutableFloatSet;

/**
 * This file was automatically generated from template file immutablePrimitiveBag.stg.
 *
 * @since 3.0.
 */
public interface ImmutableFloatBag extends ImmutableFloatCollection, FloatBag
{
    @Override
    ImmutableFloatBag select(FloatPredicate predicate);

    @Override
    ImmutableFloatBag reject(FloatPredicate predicate);

    /**
     * @since 9.0.
     */
    @Override
    default ImmutableFloatBag tap(FloatProcedure procedure)
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
    ImmutableFloatBag selectByOccurrences(IntPredicate predicate);

    /**
     * Returns all elements of the bag that have more than one occurrence.
     *
     * @since 9.2
     */
    @Override
    default ImmutableFloatBag selectDuplicates()
    {
        return this.selectByOccurrences(occurrences -> occurrences > 1);
    }

    /**
     * Returns all elements of the bag that have exactly one occurrence.
     *
     * @since 9.2
     */
    @Override
    ImmutableFloatSet selectUnique();

    /**
     * Returns the {@code count} most frequently occurring items.
     *
     * In the event of a tie, all of the items with the number of occurrences that match the occurrences of the last
     * item will be returned.
     *
     * @since 8.0
     */
    @Override
    ImmutableList<FloatIntPair> topOccurrences(int count);

    /**
     * Returns the {@code count} least frequently occurring items.
     *
     * In the event of a tie, all of the items with the number of occurrences that match the occurrences of the last
     * item will be returned.
     *
     * @since 8.0
     */
    @Override
    ImmutableList<FloatIntPair> bottomOccurrences(int count);

    @Override
    <V> ImmutableBag<V> collect(FloatToObjectFunction<? extends V> function);

    @Override
    ImmutableFloatBag newWith(float element);

    @Override
    ImmutableFloatBag newWithout(float element);

    @Override
    ImmutableFloatBag newWithAll(FloatIterable elements);

    @Override
    ImmutableFloatBag newWithoutAll(FloatIterable elements);
}
