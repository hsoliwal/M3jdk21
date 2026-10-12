/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.utility.primitive;

import org.eclipse.collections.api.ByteIterable;
import org.eclipse.collections.api.LazyByteIterable;
import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.block.function.primitive.ByteToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.BytePredicate;
import org.eclipse.collections.api.block.procedure.primitive.ByteProcedure;
import org.eclipse.collections.impl.factory.primitive.ByteLists;
import org.eclipse.collections.impl.lazy.primitive.CollectByteToObjectIterable;
import org.eclipse.collections.impl.lazy.primitive.FlatCollectByteToObjectIterable;
import org.eclipse.collections.impl.lazy.primitive.LazyByteIterableAdapter;
import org.eclipse.collections.impl.lazy.primitive.SelectByteIterable;
import org.eclipse.collections.impl.lazy.primitive.TapByteIterable;

/**
 * LazyByteIterate is a factory class which creates "deferred" byte iterables around the specified byte iterables. A "deferred"
 * byte iterable performs some operation, such as filtering or transforming, when the result byte iterable is iterated over.  This
 * makes the operation very memory efficient, because you don't have to create intermediate collections during the
 * operation.
 * This file was automatically generated from template file lazyPrimitiveIterate.stg.
 *
 * @since 5.0
 */
public final class LazyByteIterate
{
    private static final LazyByteIterable EMPTY_ITERABLE = ByteLists.immutable.empty().asLazy();

    private LazyByteIterate()
    {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }

    /**
     * Creates a deferred byte iterable for the specified byte iterable.
     */
    public static LazyByteIterable adapt(ByteIterable iterable)
    {
        return new LazyByteIterableAdapter(iterable);
    }

    /**
     * Creates a deferred filtering byte iterable for the specified byte iterable.
     */
    public static LazyByteIterable select(ByteIterable iterable, BytePredicate predicate)
    {
        return new SelectByteIterable(iterable, predicate);
    }

    /**
     * Creates a deferred transforming byte iterable for the specified byte iterable.
     */
    public static <V> LazyIterable<V> collect(
            ByteIterable iterable,
            ByteToObjectFunction<? extends V> function)
    {
        return new CollectByteToObjectIterable<>(iterable, function);
    }

    /**
     * Creates a deferred transforming and flattening byte iterable for the specified byte iterable.
     *
     * @since 9.0
     */
    public static <V> LazyIterable<V> flatCollect(
            ByteIterable iterable,
            ByteToObjectFunction<? extends Iterable<V>> function)
    {
        return new FlatCollectByteToObjectIterable<>(iterable, function);
    }

    /**
     * Creates a deferred filtering and transforming byte iterable for the specified byte iterable.
     */
    public static <V> LazyIterable<V> collectIf(
            ByteIterable iterable,
            BytePredicate predicate,
            ByteToObjectFunction<? extends V> function)
    {
        return LazyByteIterate.select(iterable, predicate).collect(function);
    }

    public static LazyByteIterable empty()
    {
        return EMPTY_ITERABLE;
    }

    /**
     * Creates a deferred tap iterable for the specified iterable.
     *
     * @since 9.0
     */
    public static LazyByteIterable tap(ByteIterable iterable, ByteProcedure procedure)
    {
        return new TapByteIterable(iterable, procedure);
    }
}
