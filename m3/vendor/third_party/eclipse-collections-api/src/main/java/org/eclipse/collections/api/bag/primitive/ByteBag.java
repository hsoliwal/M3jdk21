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

import org.eclipse.collections.api.ByteIterable;
import org.eclipse.collections.api.bag.Bag;
import org.eclipse.collections.api.block.function.primitive.ByteToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.IntPredicate;
import org.eclipse.collections.api.block.predicate.primitive.BytePredicate;
import org.eclipse.collections.api.block.procedure.primitive.ByteProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ByteIntProcedure;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.tuple.primitive.ByteIntPair;
import org.eclipse.collections.api.set.primitive.ByteSet;

/**
 * This file was automatically generated from template file primitiveBag.stg.
 *
 * @since 3.0.
 */
public interface ByteBag extends ByteIterable
{
    /**
     * The size of the Bag when counting only distinct elements.
     */
    int sizeDistinct();

    /**
     * @since 9.0.
     */
    @Override
    default ByteBag tap(ByteProcedure procedure)
    {
        this.forEach(procedure);
        return this;
    }

    /**
     * Returns all elements of the bag that have a number of occurrences that satisfy the predicate.
     *
     * @since 8.0
     */
    ByteBag selectByOccurrences(IntPredicate predicate);

    /**
     * Returns all elements of the bag that have more than one occurrence.
     *
     * @since 9.2
     */
    default ByteBag selectDuplicates()
    {
        return this.selectByOccurrences(occurrences -> occurrences > 1);
    }

    /**
     * Returns all elements of the bag that have exactly one occurrence.
     *
     * @since 9.2
     */
    ByteSet selectUnique();

    /**
     * Returns the {@code count} most frequently occurring items.
     *
     * In the event of a tie, all of the items with the number of occurrences that match the occurrences of the last
     * item will be returned.
     *
     * @since 8.0
     */
    ListIterable<ByteIntPair> topOccurrences(int count);

    /**
     * Returns the {@code count} least frequently occurring items.
     *
     * In the event of a tie, all of the items with the number of occurrences that match the occurrences of the last
     * item will be returned.
     *
     * @since 8.0
     */
    ListIterable<ByteIntPair> bottomOccurrences(int count);

    /**
     * The occurrences of a distinct item in the bag.
     */
    int occurrencesOf(byte item);

    /**
     * For each distinct item, with the number of occurrences, execute the specified procedure.
     */
    void forEachWithOccurrences(ByteIntProcedure procedure);

    @Override
    ByteBag select(BytePredicate predicate);

    @Override
    ByteBag reject(BytePredicate predicate);

    @Override
    <V> Bag<V> collect(ByteToObjectFunction<? extends V> function);

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
    ImmutableByteBag toImmutable();

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
