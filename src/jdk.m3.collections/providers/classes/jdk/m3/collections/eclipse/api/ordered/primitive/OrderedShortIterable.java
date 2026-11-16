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

import jdk.m3.collections.eclipse.api.ShortIterable;
import jdk.m3.collections.eclipse.api.block.function.primitive.ShortIntToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ShortToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ObjectShortIntToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.ShortIntPredicate;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.ShortPredicate;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.ShortIntProcedure;
import jdk.m3.collections.eclipse.api.collection.primitive.MutableShortCollection;
import jdk.m3.collections.eclipse.api.ordered.OrderedIterable;
import jdk.m3.collections.eclipse.api.stack.primitive.MutableShortStack;

/**
 * This file was automatically generated from template file orderedPrimitiveIterable.stg.
 *
 * @since 6.0.
 */
public interface OrderedShortIterable extends ShortIterable
{
    short getFirst();

    int indexOf(short value);

    @Override
    OrderedShortIterable select(ShortPredicate predicate);

    @Override
    OrderedShortIterable reject(ShortPredicate predicate);

    /**
     * Returns a new OrderedShortIterable including all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    default OrderedShortIterable selectWithIndex(ShortIntPredicate predicate)
    {
        int[] index = {0};
        return this.select(each -> predicate.accept(each, index[0]++));
    }

    /**
     * Returns a new MutableShortCollection including all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    default <R extends MutableShortCollection> R selectWithIndex(ShortIntPredicate predicate, R target)
    {
        int[] index = {0};
        return this.select(each -> predicate.accept(each, index[0]++), target);
    }

    /**
     * Returns a new OrderedShortIterable excluding all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    default OrderedShortIterable rejectWithIndex(ShortIntPredicate predicate)
    {
        int[] index = {0};
        return this.reject(each -> predicate.accept(each, index[0]++));
    }

    /**
     * Returns a new MutableShortCollection excluding all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    default <R extends MutableShortCollection> R rejectWithIndex(ShortIntPredicate predicate, R target)
    {
        int[] index = {0};
        return this.reject(each -> predicate.accept(each, index[0]++), target);
    }

    @Override
    <V> OrderedIterable<V> collect(ShortToObjectFunction<? extends V> function);

    /**
     * Returns a new OrderedIterable using results obtained by applying the specified function to each element
     * and its corresponding index.
     *
     * @since 9.1.
     */
    default <V> OrderedIterable<V> collectWithIndex(ShortIntToObjectFunction<? extends V> function)
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
    default <V, R extends Collection<V>> R collectWithIndex(ShortIntToObjectFunction<? extends V> function, R target)
    {
        int[] index = {0};
        return this.collect(each -> function.value(each, index[0]++), target);
    }

    <T> T injectIntoWithIndex(T injectedValue, ObjectShortIntToObjectFunction<? super T, ? extends T> function);

    void forEachWithIndex(ShortIntProcedure procedure);

    /**
     * Converts the collection to a ShortStack.
     *
     * @since 12.0.
     */
    default MutableShortStack toStack()
    {
        throw new UnsupportedOperationException("Implement in concrete classes.");
    }
}
