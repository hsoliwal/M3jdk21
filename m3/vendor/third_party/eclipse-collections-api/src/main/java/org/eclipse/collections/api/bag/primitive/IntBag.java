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

import java.util.StringJoiner;

import org.eclipse.collections.api.IntIterable;
import org.eclipse.collections.api.bag.Bag;
import org.eclipse.collections.api.block.function.primitive.IntToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.IntPredicate;
import org.eclipse.collections.api.block.procedure.primitive.IntProcedure;
import org.eclipse.collections.api.block.procedure.primitive.IntIntProcedure;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.tuple.primitive.IntIntPair;
import org.eclipse.collections.api.set.primitive.IntSet;

/**
 * This file was automatically generated from template file primitiveBag.stg.
 *
 * @since 3.0.
 */
public interface IntBag extends IntIterable
{
    /**
     * The size of the Bag when counting only distinct elements.
     */
    int sizeDistinct();

    /**
     * @since 9.0.
     */
    @Override
    default IntBag tap(IntProcedure procedure)
    {
        this.forEach(procedure);
        return this;
    }

    /**
     * Returns all elements of the bag that have a number of occurrences that satisfy the predicate.
     *
     * @since 8.0
     */
    IntBag selectByOccurrences(IntPredicate predicate);

    /**
     * Returns all elements of the bag that have more than one occurrence.
     *
     * @since 9.2
     */
    default IntBag selectDuplicates()
    {
        return this.selectByOccurrences(occurrences -> occurrences > 1);
    }

    /**
     * Returns all elements of the bag that have exactly one occurrence.
     *
     * @since 9.2
     */
    IntSet selectUnique();

    /**
     * Returns the {@code count} most frequently occurring items.
     *
     * In the event of a tie, all of the items with the number of occurrences that match the occurrences of the last
     * item will be returned.
     *
     * @since 8.0
     */
    ListIterable<IntIntPair> topOccurrences(int count);

    /**
     * Returns the {@code count} least frequently occurring items.
     *
     * In the event of a tie, all of the items with the number of occurrences that match the occurrences of the last
     * item will be returned.
     *
     * @since 8.0
     */
    ListIterable<IntIntPair> bottomOccurrences(int count);

    /**
     * The occurrences of a distinct item in the bag.
     */
    int occurrencesOf(int item);

    /**
     * For each distinct item, with the number of occurrences, execute the specified procedure.
     */
    void forEachWithOccurrences(IntIntProcedure procedure);

    @Override
    IntBag select(IntPredicate predicate);

    @Override
    IntBag reject(IntPredicate predicate);

    @Override
    <V> Bag<V> collect(IntToObjectFunction<? extends V> function);

    /**
     * Follows the same general contract as {@link Bag#equals(Object)}.
     */
    @Override
    boolean equals(Object o);

    /**
     * Follows the same general contract as {@link Bag#hashCode()}.
     */
    @Override
    int hashCode();

    /**
     * Returns an immutable copy of this bag. If the bag is immutable, it returns itself.
     */
    ImmutableIntBag toImmutable();

    /**
     * @since 9.2
     */
    default String toStringOfItemToCount()
    {
        StringJoiner joiner = new StringJoiner(", ", "{", "}");
        this.forEachWithOccurrences((each, occurrences) -> joiner.add(each + "=" + occurrences));
        return joiner.toString();
    }
}
