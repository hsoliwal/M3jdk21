/*
 * Copyright (c) 2026 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.api.stack.primitive;

import jdk.m3.collections.eclipse.api.block.function.primitive.LongIntToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.LongToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.LongIntPredicate;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.LongPredicate;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.LongProcedure;
import jdk.m3.collections.eclipse.api.list.primitive.LongList;
import jdk.m3.collections.eclipse.api.ordered.primitive.OrderedLongIterable;
import jdk.m3.collections.eclipse.api.stack.StackIterable;

/**
 * This file was automatically generated from template file primitiveStack.stg.
 *
 * @since 3.0.
 */
public interface LongStack extends OrderedLongIterable
{
    /**
     * Returns the top of the stack.
     */
    long peek();

    /**
     * Returns LongList of the number of elements specified by the count, beginning with the top of the stack.
     */
    LongList peek(int count);

    /**
     * Returns the element at the specified index.
     *
     * @param index the location to peek into
     */
    long peekAt(int index);

    @Override
    LongStack select(LongPredicate predicate);

    @Override
    LongStack reject(LongPredicate predicate);

    /**
     * @since 9.0.
     */
    @Override
    default LongStack tap(LongProcedure procedure)
    {
        this.forEach(procedure);
        return this;
    }

    @Override
    <V> StackIterable<V> collect(LongToObjectFunction<? extends V> function);

    /**
     * Returns a new LongStack including all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    @Override
    default LongStack selectWithIndex(LongIntPredicate predicate)
    {
        int[] index = {0};
        return this.select(each -> predicate.accept(each, index[0]++));
    }

    /**
     * Returns a new LongStack excluding all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    @Override
    default LongStack rejectWithIndex(LongIntPredicate predicate)
    {
        int[] index = {0};
        return this.reject(each -> predicate.accept(each, index[0]++));
    }

    /**
     * Returns a new StackIterable using results obtained by applying the specified function to each element
     * and its corresponding index.
     *
     * @since 9.1.
     */
    @Override
    default <V> StackIterable<V> collectWithIndex(LongIntToObjectFunction<? extends V> function)
    {
        int[] index = {0};
        return this.collect(each -> function.value(each, index[0]++));
    }

    /**
     * Follows the same general contract as {@link StackIterable#equals(Object)}.
     */
    @Override
    boolean equals(Object o);

    /**
     * Follows the same general contract as {@link StackIterable#hashCode()}.
     */
    @Override
    int hashCode();

    ImmutableLongStack toImmutable();
}
