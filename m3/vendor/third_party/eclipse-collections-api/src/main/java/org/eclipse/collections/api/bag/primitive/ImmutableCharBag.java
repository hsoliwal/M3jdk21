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

import org.eclipse.collections.api.CharIterable;
import org.eclipse.collections.api.bag.ImmutableBag;
import org.eclipse.collections.api.block.function.primitive.CharToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.IntPredicate;
import org.eclipse.collections.api.block.predicate.primitive.CharPredicate;
import org.eclipse.collections.api.block.procedure.primitive.CharProcedure;
import org.eclipse.collections.api.collection.primitive.ImmutableCharCollection;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.tuple.primitive.CharIntPair;
import org.eclipse.collections.api.set.primitive.ImmutableCharSet;

/**
 * This file was automatically generated from template file immutablePrimitiveBag.stg.
 *
 * @since 3.0.
 */
public interface ImmutableCharBag extends ImmutableCharCollection, CharBag
{
    @Override
    ImmutableCharBag select(CharPredicate predicate);

    @Override
    ImmutableCharBag reject(CharPredicate predicate);

    /**
     * @since 9.0.
     */
    @Override
    default ImmutableCharBag tap(CharProcedure procedure)
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
    ImmutableCharBag selectByOccurrences(IntPredicate predicate);

    /**
     * Returns all elements of the bag that have more than one occurrence.
     *
     * @since 9.2
     */
    @Override
    default ImmutableCharBag selectDuplicates()
    {
        return this.selectByOccurrences(occurrences -> occurrences > 1);
    }

    /**
     * Returns all elements of the bag that have exactly one occurrence.
     *
     * @since 9.2
     */
    @Override
    ImmutableCharSet selectUnique();

    /**
     * Returns the {@code count} most frequently occurring items.
     *
     * In the event of a tie, all of the items with the number of occurrences that match the occurrences of the last
     * item will be returned.
     *
     * @since 8.0
     */
    @Override
    ImmutableList<CharIntPair> topOccurrences(int count);

    /**
     * Returns the {@code count} least frequently occurring items.
     *
     * In the event of a tie, all of the items with the number of occurrences that match the occurrences of the last
     * item will be returned.
     *
     * @since 8.0
     */
    @Override
    ImmutableList<CharIntPair> bottomOccurrences(int count);

    @Override
    <V> ImmutableBag<V> collect(CharToObjectFunction<? extends V> function);

    @Override
    ImmutableCharBag newWith(char element);

    @Override
    ImmutableCharBag newWithout(char element);

    @Override
    ImmutableCharBag newWithAll(CharIterable elements);

    @Override
    ImmutableCharBag newWithoutAll(CharIterable elements);
}
