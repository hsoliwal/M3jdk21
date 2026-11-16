/*
 * Copyright (c) 2026 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.api.ordered.primitive;

import java.util.Collection;

import jdk.m3.collections.eclipse.api.CharIterable;
import jdk.m3.collections.eclipse.api.block.function.primitive.CharIntToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.CharToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ObjectCharIntToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.CharIntPredicate;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.CharPredicate;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.CharIntProcedure;
import jdk.m3.collections.eclipse.api.collection.primitive.MutableCharCollection;
import jdk.m3.collections.eclipse.api.ordered.OrderedIterable;
import jdk.m3.collections.eclipse.api.stack.primitive.MutableCharStack;

/**
 * This file was automatically generated from template file orderedPrimitiveIterable.stg.
 *
 * @since 6.0.
 */
public interface OrderedCharIterable extends CharIterable
{
    char getFirst();

    int indexOf(char value);

    @Override
    OrderedCharIterable select(CharPredicate predicate);

    @Override
    OrderedCharIterable reject(CharPredicate predicate);

    /**
     * Returns a new OrderedCharIterable including all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    default OrderedCharIterable selectWithIndex(CharIntPredicate predicate)
    {
        int[] index = {0};
        return this.select(each -> predicate.accept(each, index[0]++));
    }

    /**
     * Returns a new MutableCharCollection including all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    default <R extends MutableCharCollection> R selectWithIndex(CharIntPredicate predicate, R target)
    {
        int[] index = {0};
        return this.select(each -> predicate.accept(each, index[0]++), target);
    }

    /**
     * Returns a new OrderedCharIterable excluding all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    default OrderedCharIterable rejectWithIndex(CharIntPredicate predicate)
    {
        int[] index = {0};
        return this.reject(each -> predicate.accept(each, index[0]++));
    }

    /**
     * Returns a new MutableCharCollection excluding all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    default <R extends MutableCharCollection> R rejectWithIndex(CharIntPredicate predicate, R target)
    {
        int[] index = {0};
        return this.reject(each -> predicate.accept(each, index[0]++), target);
    }

    @Override
    <V> OrderedIterable<V> collect(CharToObjectFunction<? extends V> function);

    /**
     * Returns a new OrderedIterable using results obtained by applying the specified function to each element
     * and its corresponding index.
     *
     * @since 9.1.
     */
    default <V> OrderedIterable<V> collectWithIndex(CharIntToObjectFunction<? extends V> function)
    {
        int[] index = {0};
        return this.collect(each -> function.value(each, index[0]++));
    }

    /**
     * Adds elements to the target Collection using results obtained by applying the specified function to each element
     * and its corresponding index.
     *
     * @since 9.1.
     */
    default <V, R extends Collection<V>> R collectWithIndex(CharIntToObjectFunction<? extends V> function, R target)
    {
        int[] index = {0};
        return this.collect(each -> function.value(each, index[0]++), target);
    }

    <T> T injectIntoWithIndex(T injectedValue, ObjectCharIntToObjectFunction<? super T, ? extends T> function);

    void forEachWithIndex(CharIntProcedure procedure);

    /**
     * Converts the collection to a CharStack.
     *
     * @since 12.0.
     */
    default MutableCharStack toStack()
    {
        throw new UnsupportedOperationException("Implement in concrete classes.");
    }
}
