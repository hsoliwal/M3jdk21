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

import org.eclipse.collections.api.LazyShortIterable;
import org.eclipse.collections.api.block.function.primitive.ShortIntToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ShortToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectShortIntToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.ShortIntPredicate;
import org.eclipse.collections.api.block.predicate.primitive.ShortPredicate;
import org.eclipse.collections.api.ordered.ReversibleIterable;

/**
 * This file was automatically generated from template file reversiblePrimitiveIterable.stg.
 *
 * @since 5.0.
 */
public interface ReversibleShortIterable extends OrderedShortIterable
{
    short getLast();

    LazyShortIterable asReversed();

    @Override
    ReversibleShortIterable select(ShortPredicate predicate);

    @Override
    ReversibleShortIterable reject(ShortPredicate predicate);

    /**
     * Returns a new ReversibleShortIterable including all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    @Override
    default ReversibleShortIterable selectWithIndex(ShortIntPredicate predicate)
    {
        int[] index = {0};
        return this.select(each -> predicate.accept(each, index[0]++));
    }

    /**
     * Returns a new ReversibleShortIterable excluding all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    @Override
    default ReversibleShortIterable rejectWithIndex(ShortIntPredicate predicate)
    {
        int[] index = {0};
        return this.reject(each -> predicate.accept(each, index[0]++));
    }

    @Override
    <V> ReversibleIterable<V> collect(ShortToObjectFunction<? extends V> function);

    /**
     * Returns a new ReversibleIterable using results obtained by applying the specified function to each element
     * and its corresponding index.
     *
     * @since 9.1.
     */
    @Override
    default <V> ReversibleIterable<V> collectWithIndex(ShortIntToObjectFunction<? extends V> function)
    {
        int[] index = {0};
        return this.collect(each -> function.value(each, index[0]++));
    }

    ReversibleShortIterable toReversed();

    /**
     * @since 6.0.
     */
    ReversibleShortIterable distinct();

    @Override
    <T> T injectIntoWithIndex(T injectedValue, ObjectShortIntToObjectFunction<? super T, ? extends T> function);
}
