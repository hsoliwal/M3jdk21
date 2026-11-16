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

import jdk.m3.collections.eclipse.api.LazyDoubleIterable;
import jdk.m3.collections.eclipse.api.block.function.primitive.DoubleIntToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.DoubleToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ObjectDoubleIntToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.DoubleIntPredicate;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.DoublePredicate;
import jdk.m3.collections.eclipse.api.ordered.ReversibleIterable;

/**
 * This file was automatically generated from template file reversiblePrimitiveIterable.stg.
 *
 * @since 5.0.
 */
public interface ReversibleDoubleIterable extends OrderedDoubleIterable
{
    double getLast();

    LazyDoubleIterable asReversed();

    @Override
    ReversibleDoubleIterable select(DoublePredicate predicate);

    @Override
    ReversibleDoubleIterable reject(DoublePredicate predicate);

    /**
     * Returns a new ReversibleDoubleIterable including all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    @Override
    default ReversibleDoubleIterable selectWithIndex(DoubleIntPredicate predicate)
    {
        int[] index = {0};
        return this.select(each -> predicate.accept(each, index[0]++));
    }

    /**
     * Returns a new ReversibleDoubleIterable excluding all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    @Override
    default ReversibleDoubleIterable rejectWithIndex(DoubleIntPredicate predicate)
    {
        int[] index = {0};
        return this.reject(each -> predicate.accept(each, index[0]++));
    }

    @Override
    <V> ReversibleIterable<V> collect(DoubleToObjectFunction<? extends V> function);

    /**
     * Returns a new ReversibleIterable using results obtained by applying the specified function to each element
     * and its corresponding index.
     *
     * @since 9.1.
     */
    @Override
    default <V> ReversibleIterable<V> collectWithIndex(DoubleIntToObjectFunction<? extends V> function)
    {
        int[] index = {0};
        return this.collect(each -> function.value(each, index[0]++));
    }

    ReversibleDoubleIterable toReversed();

    /**
     * @since 6.0.
     */
    ReversibleDoubleIterable distinct();

    @Override
    <T> T injectIntoWithIndex(T injectedValue, ObjectDoubleIntToObjectFunction<? super T, ? extends T> function);
}
