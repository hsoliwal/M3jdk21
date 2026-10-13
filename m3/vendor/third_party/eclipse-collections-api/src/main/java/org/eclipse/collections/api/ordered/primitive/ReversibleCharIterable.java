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

import org.eclipse.collections.api.LazyCharIterable;
import org.eclipse.collections.api.block.function.primitive.CharIntToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.CharToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectCharIntToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.CharIntPredicate;
import org.eclipse.collections.api.block.predicate.primitive.CharPredicate;
import org.eclipse.collections.api.ordered.ReversibleIterable;

/**
 * This file was automatically generated from template file reversiblePrimitiveIterable.stg.
 *
 * @since 5.0.
 */
public interface ReversibleCharIterable extends OrderedCharIterable
{
    char getLast();

    LazyCharIterable asReversed();

    @Override
    ReversibleCharIterable select(CharPredicate predicate);

    @Override
    ReversibleCharIterable reject(CharPredicate predicate);

    /**
     * Returns a new ReversibleCharIterable including all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    @Override
    default ReversibleCharIterable selectWithIndex(CharIntPredicate predicate)
    {
        int[] index = {0};
        return this.select(each -> predicate.accept(each, index[0]++));
    }

    /**
     * Returns a new ReversibleCharIterable excluding all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    @Override
    default ReversibleCharIterable rejectWithIndex(CharIntPredicate predicate)
    {
        int[] index = {0};
        return this.reject(each -> predicate.accept(each, index[0]++));
    }

    @Override
    <V> ReversibleIterable<V> collect(CharToObjectFunction<? extends V> function);

    /**
     * Returns a new ReversibleIterable using results obtained by applying the specified function to each element
     * and its corresponding index.
     *
     * @since 9.1.
     */
    @Override
    default <V> ReversibleIterable<V> collectWithIndex(CharIntToObjectFunction<? extends V> function)
    {
        int[] index = {0};
        return this.collect(each -> function.value(each, index[0]++));
    }

    ReversibleCharIterable toReversed();

    /**
     * @since 6.0.
     */
    ReversibleCharIterable distinct();

    @Override
    <T> T injectIntoWithIndex(T injectedValue, ObjectCharIntToObjectFunction<? super T, ? extends T> function);
}
