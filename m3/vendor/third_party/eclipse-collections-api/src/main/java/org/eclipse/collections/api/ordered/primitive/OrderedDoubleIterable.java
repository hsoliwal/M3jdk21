/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.api.ordered.primitive;

import java.util.Collection;

import org.eclipse.collections.api.DoubleIterable;
import org.eclipse.collections.api.block.function.primitive.DoubleIntToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.DoubleToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectDoubleIntToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.DoubleIntPredicate;
import org.eclipse.collections.api.block.predicate.primitive.DoublePredicate;
import org.eclipse.collections.api.block.procedure.primitive.DoubleIntProcedure;
import org.eclipse.collections.api.collection.primitive.MutableDoubleCollection;
import org.eclipse.collections.api.ordered.OrderedIterable;

/**
 * This file was automatically generated from template file orderedPrimitiveIterable.stg.
 *
 * @since 6.0.
 */
public interface OrderedDoubleIterable extends DoubleIterable
{
    double getFirst();

    int indexOf(double value);

    @Override
    OrderedDoubleIterable select(DoublePredicate predicate);

    @Override
    OrderedDoubleIterable reject(DoublePredicate predicate);

    /**
     * Returns a new OrderedDoubleIterable including all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    default OrderedDoubleIterable selectWithIndex(DoubleIntPredicate predicate)
    {
        int[] index = {0};
        return this.select(each -> predicate.accept(each, index[0]++));
    }

    /**
     * Returns a new MutableDoubleCollection including all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    default <R extends MutableDoubleCollection> R selectWithIndex(DoubleIntPredicate predicate, R target)
    {
        int[] index = {0};
        return this.select(each -> predicate.accept(each, index[0]++), target);
    }

    /**
     * Returns a new OrderedDoubleIterable excluding all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    default OrderedDoubleIterable rejectWithIndex(DoubleIntPredicate predicate)
    {
        int[] index = {0};
        return this.reject(each -> predicate.accept(each, index[0]++));
    }

    /**
     * Returns a new MutableDoubleCollection excluding all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    default <R extends MutableDoubleCollection> R rejectWithIndex(DoubleIntPredicate predicate, R target)
    {
        int[] index = {0};
        return this.reject(each -> predicate.accept(each, index[0]++), target);
    }

    @Override
    <V> OrderedIterable<V> collect(DoubleToObjectFunction<? extends V> function);

    /**
     * Returns a new OrderedIterable using results obtained by applying the specified function to each element
     * and its corresponding index.
     *
     * @since 9.1.
     */
    default <V> OrderedIterable<V> collectWithIndex(DoubleIntToObjectFunction<? extends V> function)
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
    default <V, R extends Collection<V>> R collectWithIndex(DoubleIntToObjectFunction<? extends V> function, R target)
    {
        int[] index = {0};
        return this.collect(each -> function.value(each, index[0]++), target);
    }

    <T> T injectIntoWithIndex(T injectedValue, ObjectDoubleIntToObjectFunction<? super T, ? extends T> function);

    void forEachWithIndex(DoubleIntProcedure procedure);
}
