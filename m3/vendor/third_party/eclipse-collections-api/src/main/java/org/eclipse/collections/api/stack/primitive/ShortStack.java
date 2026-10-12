/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.api.stack.primitive;

import org.eclipse.collections.api.block.function.primitive.ShortIntToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ShortToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.ShortIntPredicate;
import org.eclipse.collections.api.block.predicate.primitive.ShortPredicate;
import org.eclipse.collections.api.block.procedure.primitive.ShortProcedure;
import org.eclipse.collections.api.list.primitive.ShortList;
import org.eclipse.collections.api.ordered.primitive.OrderedShortIterable;
import org.eclipse.collections.api.stack.StackIterable;

/**
 * This file was automatically generated from template file primitiveStack.stg.
 *
 * @since 3.0.
 */
public interface ShortStack extends OrderedShortIterable
{
    /**
     * Returns the top of the stack.
     */
    short peek();

    /**
     * Returns ShortList of the number of elements specified by the count, beginning with the top of the stack.
     */
    ShortList peek(int count);

    /**
     * Returns the element at the specified index.
     *
     * @param index the location to peek into
     */
    short peekAt(int index);

    @Override
    ShortStack select(ShortPredicate predicate);

    @Override
    ShortStack reject(ShortPredicate predicate);

    /**
     * @since 9.0.
     */
    @Override
    default ShortStack tap(ShortProcedure procedure)
    {
        this.forEach(procedure);
        return this;
    }

    @Override
    <V> StackIterable<V> collect(ShortToObjectFunction<? extends V> function);

    /**
     * Returns a new ShortStack including all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    @Override
    default ShortStack selectWithIndex(ShortIntPredicate predicate)
    {
        int[] index = {0};
        return this.select(each -> predicate.accept(each, index[0]++));
    }

    /**
     * Returns a new ShortStack excluding all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    @Override
    default ShortStack rejectWithIndex(ShortIntPredicate predicate)
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
    default <V> StackIterable<V> collectWithIndex(ShortIntToObjectFunction<? extends V> function)
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

    ImmutableShortStack toImmutable();
}
