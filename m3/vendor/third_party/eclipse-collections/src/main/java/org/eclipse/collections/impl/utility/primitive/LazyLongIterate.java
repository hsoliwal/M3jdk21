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

import org.eclipse.collections.api.LongIterable;
import org.eclipse.collections.api.LazyLongIterable;
import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.block.function.primitive.LongToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.LongPredicate;
import org.eclipse.collections.api.block.procedure.primitive.LongProcedure;
import org.eclipse.collections.impl.factory.primitive.LongLists;
import org.eclipse.collections.impl.lazy.primitive.CollectLongToObjectIterable;
import org.eclipse.collections.impl.lazy.primitive.FlatCollectLongToObjectIterable;
import org.eclipse.collections.impl.lazy.primitive.LazyLongIterableAdapter;
import org.eclipse.collections.impl.lazy.primitive.SelectLongIterable;
import org.eclipse.collections.impl.lazy.primitive.TapLongIterable;

/**
 * LazyLongIterate is a factory class which creates "deferred" long iterables around the specified long iterables. A "deferred"
 * long iterable performs some operation, such as filtering or transforming, when the result long iterable is iterated over.  This
 * makes the operation very memory efficient, because you don't have to create intermediate collections during the
 * operation.
 * This file was automatically generated from template file lazyPrimitiveIterate.stg.
 *
 * @since 5.0
 */
public final class LazyLongIterate
{
    private static final LazyLongIterable EMPTY_ITERABLE = LongLists.immutable.empty().asLazy();

    private LazyLongIterate()
    {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }

    /**
     * Creates a deferred long iterable for the specified long iterable.
     */
    public static LazyLongIterable adapt(LongIterable iterable)
    {
        return new LazyLongIterableAdapter(iterable);
    }

    /**
     * Creates a deferred filtering long iterable for the specified long iterable.
     */
    public static LazyLongIterable select(LongIterable iterable, LongPredicate predicate)
    {
        return new SelectLongIterable(iterable, predicate);
    }

    /**
     * Creates a deferred transforming long iterable for the specified long iterable.
     */
    public static <V> LazyIterable<V> collect(
            LongIterable iterable,
            LongToObjectFunction<? extends V> function)
    {
        return new CollectLongToObjectIterable<>(iterable, function);
    }

    /**
     * Creates a deferred transforming and flattening long iterable for the specified long iterable.
     *
     * @since 9.0
     */
    public static <V> LazyIterable<V> flatCollect(
            LongIterable iterable,
            LongToObjectFunction<? extends Iterable<V>> function)
    {
        return new FlatCollectLongToObjectIterable<>(iterable, function);
    }

    /**
     * Creates a deferred filtering and transforming long iterable for the specified long iterable.
     */
    public static <V> LazyIterable<V> collectIf(
            LongIterable iterable,
            LongPredicate predicate,
            LongToObjectFunction<? extends V> function)
    {
        return LazyLongIterate.select(iterable, predicate).collect(function);
    }

    public static LazyLongIterable empty()
    {
        return EMPTY_ITERABLE;
    }

    /**
     * Creates a deferred tap iterable for the specified iterable.
     *
     * @since 9.0
     */
    public static LazyLongIterable tap(LongIterable iterable, LongProcedure procedure)
    {
        return new TapLongIterable(iterable, procedure);
    }
}
