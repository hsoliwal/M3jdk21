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

import org.eclipse.collections.api.IntIterable;
import org.eclipse.collections.api.LazyIntIterable;
import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.block.function.primitive.IntToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.IntPredicate;
import org.eclipse.collections.api.block.procedure.primitive.IntProcedure;
import org.eclipse.collections.impl.factory.primitive.IntLists;
import org.eclipse.collections.impl.lazy.primitive.CollectIntToObjectIterable;
import org.eclipse.collections.impl.lazy.primitive.FlatCollectIntToObjectIterable;
import org.eclipse.collections.impl.lazy.primitive.LazyIntIterableAdapter;
import org.eclipse.collections.impl.lazy.primitive.SelectIntIterable;
import org.eclipse.collections.impl.lazy.primitive.TapIntIterable;

/**
 * LazyIntIterate is a factory class which creates "deferred" int iterables around the specified int iterables. A "deferred"
 * int iterable performs some operation, such as filtering or transforming, when the result int iterable is iterated over.  This
 * makes the operation very memory efficient, because you don't have to create intermediate collections during the
 * operation.
 * This file was automatically generated from template file lazyPrimitiveIterate.stg.
 *
 * @since 5.0
 */
public final class LazyIntIterate
{
    private static final LazyIntIterable EMPTY_ITERABLE = IntLists.immutable.empty().asLazy();

    private LazyIntIterate()
    {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }

    /**
     * Creates a deferred int iterable for the specified int iterable.
     */
    public static LazyIntIterable adapt(IntIterable iterable)
    {
        return new LazyIntIterableAdapter(iterable);
    }

    /**
     * Creates a deferred filtering int iterable for the specified int iterable.
     */
    public static LazyIntIterable select(IntIterable iterable, IntPredicate predicate)
    {
        return new SelectIntIterable(iterable, predicate);
    }

    /**
     * Creates a deferred transforming int iterable for the specified int iterable.
     */
    public static <V> LazyIterable<V> collect(
            IntIterable iterable,
            IntToObjectFunction<? extends V> function)
    {
        return new CollectIntToObjectIterable<>(iterable, function);
    }

    /**
     * Creates a deferred transforming and flattening int iterable for the specified int iterable.
     *
     * @since 9.0
     */
    public static <V> LazyIterable<V> flatCollect(
            IntIterable iterable,
            IntToObjectFunction<? extends Iterable<V>> function)
    {
        return new FlatCollectIntToObjectIterable<>(iterable, function);
    }

    /**
     * Creates a deferred filtering and transforming int iterable for the specified int iterable.
     */
    public static <V> LazyIterable<V> collectIf(
            IntIterable iterable,
            IntPredicate predicate,
            IntToObjectFunction<? extends V> function)
    {
        return LazyIntIterate.select(iterable, predicate).collect(function);
    }

    public static LazyIntIterable empty()
    {
        return EMPTY_ITERABLE;
    }

    /**
     * Creates a deferred tap iterable for the specified iterable.
     *
     * @since 9.0
     */
    public static LazyIntIterable tap(IntIterable iterable, IntProcedure procedure)
    {
        return new TapIntIterable(iterable, procedure);
    }
}
