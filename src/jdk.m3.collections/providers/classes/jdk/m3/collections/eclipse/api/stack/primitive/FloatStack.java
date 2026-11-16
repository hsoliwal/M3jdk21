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

import jdk.m3.collections.eclipse.api.block.function.primitive.FloatIntToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.FloatToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.FloatIntPredicate;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.FloatPredicate;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.FloatProcedure;
import jdk.m3.collections.eclipse.api.list.primitive.FloatList;
import jdk.m3.collections.eclipse.api.ordered.primitive.OrderedFloatIterable;
import jdk.m3.collections.eclipse.api.stack.StackIterable;

/**
 * This file was automatically generated from template file primitiveStack.stg.
 *
 * @since 3.0.
 */
public interface FloatStack extends OrderedFloatIterable
{
    /**
     * Returns the top of the stack.
     */
    float peek();

    /**
     * Returns FloatList of the number of elements specified by the count, beginning with the top of the stack.
     */
    FloatList peek(int count);

    /**
     * Returns the element at the specified index.
     *
     * @param index the location to peek into
     */
    float peekAt(int index);

    @Override
    FloatStack select(FloatPredicate predicate);

    @Override
    FloatStack reject(FloatPredicate predicate);

    /**
     * @since 9.0.
     */
    @Override
    default FloatStack tap(FloatProcedure procedure)
    {
        this.forEach(procedure);
        return this;
    }

    @Override
    <V> StackIterable<V> collect(FloatToObjectFunction<? extends V> function);

    /**
     * Returns a new FloatStack including all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    @Override
    default FloatStack selectWithIndex(FloatIntPredicate predicate)
    {
        int[] index = {0};
        return this.select(each -> predicate.accept(each, index[0]++));
    }

    /**
     * Returns a new FloatStack excluding all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    @Override
    default FloatStack rejectWithIndex(FloatIntPredicate predicate)
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
    default <V> StackIterable<V> collectWithIndex(FloatIntToObjectFunction<? extends V> function)
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

    ImmutableFloatStack toImmutable();
}
