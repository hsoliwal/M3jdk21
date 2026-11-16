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

import jdk.m3.collections.eclipse.api.FloatIterable;
import jdk.m3.collections.eclipse.api.block.function.primitive.FloatIntToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.FloatToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ObjectFloatIntToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.FloatIntPredicate;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.FloatPredicate;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.FloatIntProcedure;
import jdk.m3.collections.eclipse.api.collection.primitive.MutableFloatCollection;
import jdk.m3.collections.eclipse.api.ordered.OrderedIterable;
import jdk.m3.collections.eclipse.api.stack.primitive.MutableFloatStack;

/**
 * This file was automatically generated from template file orderedPrimitiveIterable.stg.
 *
 * @since 6.0.
 */
public interface OrderedFloatIterable extends FloatIterable
{
    float getFirst();

    int indexOf(float value);

    @Override
    OrderedFloatIterable select(FloatPredicate predicate);

    @Override
    OrderedFloatIterable reject(FloatPredicate predicate);

    /**
     * Returns a new OrderedFloatIterable including all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    default OrderedFloatIterable selectWithIndex(FloatIntPredicate predicate)
    {
        int[] index = {0};
        return this.select(each -> predicate.accept(each, index[0]++));
    }

    /**
     * Returns a new MutableFloatCollection including all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    default <R extends MutableFloatCollection> R selectWithIndex(FloatIntPredicate predicate, R target)
    {
        int[] index = {0};
        return this.select(each -> predicate.accept(each, index[0]++), target);
    }

    /**
     * Returns a new OrderedFloatIterable excluding all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    default OrderedFloatIterable rejectWithIndex(FloatIntPredicate predicate)
    {
        int[] index = {0};
        return this.reject(each -> predicate.accept(each, index[0]++));
    }

    /**
     * Returns a new MutableFloatCollection excluding all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    default <R extends MutableFloatCollection> R rejectWithIndex(FloatIntPredicate predicate, R target)
    {
        int[] index = {0};
        return this.reject(each -> predicate.accept(each, index[0]++), target);
    }

    @Override
    <V> OrderedIterable<V> collect(FloatToObjectFunction<? extends V> function);

    /**
     * Returns a new OrderedIterable using results obtained by applying the specified function to each element
     * and its corresponding index.
     *
     * @since 9.1.
     */
    default <V> OrderedIterable<V> collectWithIndex(FloatIntToObjectFunction<? extends V> function)
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
    default <V, R extends Collection<V>> R collectWithIndex(FloatIntToObjectFunction<? extends V> function, R target)
    {
        int[] index = {0};
        return this.collect(each -> function.value(each, index[0]++), target);
    }

    <T> T injectIntoWithIndex(T injectedValue, ObjectFloatIntToObjectFunction<? super T, ? extends T> function);

    void forEachWithIndex(FloatIntProcedure procedure);

    /**
     * Converts the collection to a FloatStack.
     *
     * @since 12.0.
     */
    default MutableFloatStack toStack()
    {
        throw new UnsupportedOperationException("Implement in concrete classes.");
    }
}
