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

import jdk.m3.collections.eclipse.api.block.function.primitive.CharIntToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.CharToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.CharIntPredicate;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.CharPredicate;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.CharProcedure;
import jdk.m3.collections.eclipse.api.list.primitive.CharList;
import jdk.m3.collections.eclipse.api.ordered.primitive.OrderedCharIterable;
import jdk.m3.collections.eclipse.api.stack.StackIterable;

/**
 * This file was automatically generated from template file primitiveStack.stg.
 *
 * @since 3.0.
 */
public interface CharStack extends OrderedCharIterable
{
    /**
     * Returns the top of the stack.
     */
    char peek();

    /**
     * Returns CharList of the number of elements specified by the count, beginning with the top of the stack.
     */
    CharList peek(int count);

    /**
     * Returns the element at the specified index.
     *
     * @param index the location to peek into
     */
    char peekAt(int index);

    @Override
    CharStack select(CharPredicate predicate);

    @Override
    CharStack reject(CharPredicate predicate);

    /**
     * @since 9.0.
     */
    @Override
    default CharStack tap(CharProcedure procedure)
    {
        this.forEach(procedure);
        return this;
    }

    @Override
    <V> StackIterable<V> collect(CharToObjectFunction<? extends V> function);

    /**
     * Returns a new CharStack including all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    @Override
    default CharStack selectWithIndex(CharIntPredicate predicate)
    {
        int[] index = {0};
        return this.select(each -> predicate.accept(each, index[0]++));
    }

    /**
     * Returns a new CharStack excluding all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    @Override
    default CharStack rejectWithIndex(CharIntPredicate predicate)
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
    default <V> StackIterable<V> collectWithIndex(CharIntToObjectFunction<? extends V> function)
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

    ImmutableCharStack toImmutable();
}
