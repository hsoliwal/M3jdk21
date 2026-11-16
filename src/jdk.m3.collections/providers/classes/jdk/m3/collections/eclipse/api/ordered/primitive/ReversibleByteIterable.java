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

import jdk.m3.collections.eclipse.api.LazyByteIterable;
import jdk.m3.collections.eclipse.api.block.function.primitive.ByteIntToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ByteToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ObjectByteIntToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.ByteIntPredicate;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.BytePredicate;
import jdk.m3.collections.eclipse.api.ordered.ReversibleIterable;

/**
 * This file was automatically generated from template file reversiblePrimitiveIterable.stg.
 *
 * @since 5.0.
 */
public interface ReversibleByteIterable extends OrderedByteIterable
{
    byte getLast();

    LazyByteIterable asReversed();

    @Override
    ReversibleByteIterable select(BytePredicate predicate);

    @Override
    ReversibleByteIterable reject(BytePredicate predicate);

    /**
     * Returns a new ReversibleByteIterable including all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    @Override
    default ReversibleByteIterable selectWithIndex(ByteIntPredicate predicate)
    {
        int[] index = {0};
        return this.select(each -> predicate.accept(each, index[0]++));
    }

    /**
     * Returns a new ReversibleByteIterable excluding all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    @Override
    default ReversibleByteIterable rejectWithIndex(ByteIntPredicate predicate)
    {
        int[] index = {0};
        return this.reject(each -> predicate.accept(each, index[0]++));
    }

    @Override
    <V> ReversibleIterable<V> collect(ByteToObjectFunction<? extends V> function);

    /**
     * Returns a new ReversibleIterable using results obtained by applying the specified function to each element
     * and its corresponding index.
     *
     * @since 9.1.
     */
    @Override
    default <V> ReversibleIterable<V> collectWithIndex(ByteIntToObjectFunction<? extends V> function)
    {
        int[] index = {0};
        return this.collect(each -> function.value(each, index[0]++));
    }

    ReversibleByteIterable toReversed();

    /**
     * @since 6.0.
     */
    ReversibleByteIterable distinct();

    @Override
    <T> T injectIntoWithIndex(T injectedValue, ObjectByteIntToObjectFunction<? super T, ? extends T> function);
}
