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

import org.eclipse.collections.api.ShortIterable;
import org.eclipse.collections.api.LazyShortIterable;
import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.block.function.primitive.ShortToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.ShortPredicate;
import org.eclipse.collections.api.block.procedure.primitive.ShortProcedure;
import org.eclipse.collections.impl.factory.primitive.ShortLists;
import org.eclipse.collections.impl.lazy.primitive.CollectShortToObjectIterable;
import org.eclipse.collections.impl.lazy.primitive.FlatCollectShortToObjectIterable;
import org.eclipse.collections.impl.lazy.primitive.LazyShortIterableAdapter;
import org.eclipse.collections.impl.lazy.primitive.SelectShortIterable;
import org.eclipse.collections.impl.lazy.primitive.TapShortIterable;

/**
 * LazyShortIterate is a factory class which creates "deferred" short iterables around the specified short iterables. A "deferred"
 * short iterable performs some operation, such as filtering or transforming, when the result short iterable is iterated over.  This
 * makes the operation very memory efficient, because you don't have to create intermediate collections during the
 * operation.
 * This file was automatically generated from template file lazyPrimitiveIterate.stg.
 *
 * @since 5.0
 */
public final class LazyShortIterate
{
    private static final LazyShortIterable EMPTY_ITERABLE = ShortLists.immutable.empty().asLazy();

    private LazyShortIterate()
    {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }

    /**
     * Creates a deferred short iterable for the specified short iterable.
     */
    public static LazyShortIterable adapt(ShortIterable iterable)
    {
        return new LazyShortIterableAdapter(iterable);
    }

    /**
     * Creates a deferred filtering short iterable for the specified short iterable.
     */
    public static LazyShortIterable select(ShortIterable iterable, ShortPredicate predicate)
    {
        return new SelectShortIterable(iterable, predicate);
    }

    /**
     * Creates a deferred transforming short iterable for the specified short iterable.
     */
    public static <V> LazyIterable<V> collect(
            ShortIterable iterable,
            ShortToObjectFunction<? extends V> function)
    {
        return new CollectShortToObjectIterable<>(iterable, function);
    }

    /**
     * Creates a deferred transforming and flattening short iterable for the specified short iterable.
     *
     * @since 9.0
     */
    public static <V> LazyIterable<V> flatCollect(
            ShortIterable iterable,
            ShortToObjectFunction<? extends Iterable<V>> function)
    {
        return new FlatCollectShortToObjectIterable<>(iterable, function);
    }

    /**
     * Creates a deferred filtering and transforming short iterable for the specified short iterable.
     */
    public static <V> LazyIterable<V> collectIf(
            ShortIterable iterable,
            ShortPredicate predicate,
            ShortToObjectFunction<? extends V> function)
    {
        return LazyShortIterate.select(iterable, predicate).collect(function);
    }

    public static LazyShortIterable empty()
    {
        return EMPTY_ITERABLE;
    }

    /**
     * Creates a deferred tap iterable for the specified iterable.
     *
     * @since 9.0
     */
    public static LazyShortIterable tap(ShortIterable iterable, ShortProcedure procedure)
    {
        return new TapShortIterable(iterable, procedure);
    }
}
