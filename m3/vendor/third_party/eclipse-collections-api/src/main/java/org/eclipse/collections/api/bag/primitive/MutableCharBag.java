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
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.block.function.primitive.CharToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.IntPredicate;
import org.eclipse.collections.api.block.predicate.primitive.CharPredicate;
import org.eclipse.collections.api.block.procedure.primitive.CharProcedure;
import org.eclipse.collections.api.collection.primitive.MutableCharCollection;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.primitive.CharIntPair;
import org.eclipse.collections.api.set.primitive.MutableCharSet;

/**
 * This file was automatically generated from template file mutablePrimitiveBag.stg.
 *
 * @since 3.0.
 */
public interface MutableCharBag extends MutableCharCollection, CharBag
{
    void addOccurrences(char item, int occurrences);

    boolean removeOccurrences(char item, int occurrences);

    /**
     * @since 9.0.
     */
    @Override
    default MutableCharBag tap(CharProcedure procedure)
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
    MutableCharBag selectByOccurrences(IntPredicate predicate);

    /**
     * Returns all elements of the bag that have more than one occurrence.
     *
     * @since 9.2
     */
    @Override
    default MutableCharBag selectDuplicates()
    {
        return this.selectByOccurrences(occurrences -> occurrences > 1);
    }

    /**
     * Returns all elements of the bag that have exactly one occurrence.
     *
     * @since 9.2
     */
    @Override
    MutableCharSet selectUnique();

    /**
     * Returns the {@code count} most frequently occurring items.
     *
     * In the event of a tie, all of the items with the number of occurrences that match the occurrences of the last
     * item will be returned.
     *
     * @since 8.0
     */
    @Override
    MutableList<CharIntPair> topOccurrences(int count);

    /**
     * Returns the {@code count} least frequently occurring items.
     *
     * In the event of a tie, all of the items with the number of occurrences that match the occurrences of the last
     * item will be returned.
     *
     * @since 8.0
     */
    @Override
    MutableList<CharIntPair> bottomOccurrences(int count);

    @Override
    MutableCharBag select(CharPredicate predicate);

    @Override
    MutableCharBag reject(CharPredicate predicate);

    @Override
    <V> MutableBag<V> collect(CharToObjectFunction<? extends V> function);

    @Override
    MutableCharBag with(char element);

    @Override
    MutableCharBag without(char element);

    @Override
    MutableCharBag withAll(CharIterable elements);

    @Override
    MutableCharBag withoutAll(CharIterable elements);

    @Override
    MutableCharBag asUnmodifiable();

    @Override
    MutableCharBag asSynchronized();

    /**
     * Returns an immutable copy of this bag.
     */
    @Override
    ImmutableCharBag toImmutable();

    /**
     * Creates a new empty mutable version of the same Bag type.
     *
     * @since 9.2.
     */
    default MutableCharBag newEmpty()
    {
        throw new UnsupportedOperationException("Implement in concrete classes.");
    }
}
