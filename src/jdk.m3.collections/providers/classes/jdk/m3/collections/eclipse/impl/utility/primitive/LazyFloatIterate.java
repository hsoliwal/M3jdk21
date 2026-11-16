/*
 * Copyright (c) 2026 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.impl.utility.primitive;

import jdk.m3.collections.eclipse.api.FloatIterable;
import jdk.m3.collections.eclipse.api.LazyFloatIterable;
import jdk.m3.collections.eclipse.api.LazyIterable;
import jdk.m3.collections.eclipse.api.block.function.primitive.FloatToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.FloatPredicate;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.FloatProcedure;
import jdk.m3.collections.eclipse.impl.factory.primitive.FloatLists;
import jdk.m3.collections.eclipse.impl.lazy.primitive.CollectFloatToObjectIterable;
import jdk.m3.collections.eclipse.impl.lazy.primitive.FlatCollectFloatToObjectIterable;
import jdk.m3.collections.eclipse.impl.lazy.primitive.LazyFloatIterableAdapter;
import jdk.m3.collections.eclipse.impl.lazy.primitive.SelectFloatIterable;
import jdk.m3.collections.eclipse.impl.lazy.primitive.TapFloatIterable;

/**
 * LazyFloatIterate is a factory class which creates "deferred" float iterables around the specified float iterables. A "deferred"
 * float iterable performs some operation, such as filtering or transforming, when the result float iterable is iterated over.  This
 * makes the operation very memory efficient, because you don't have to create intermediate collections during the
 * operation.
 * This file was automatically generated from template file lazyPrimitiveIterate.stg.
 *
 * @since 5.0
 */
public final class LazyFloatIterate
{
    private static final LazyFloatIterable EMPTY_ITERABLE = FloatLists.immutable.empty().asLazy();

    private LazyFloatIterate()
    {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }

    /**
     * Creates a deferred float iterable for the specified float iterable.
     */
    public static LazyFloatIterable adapt(FloatIterable iterable)
    {
        return new LazyFloatIterableAdapter(iterable);
    }

    /**
     * Creates a deferred filtering float iterable for the specified float iterable.
     */
    public static LazyFloatIterable select(FloatIterable iterable, FloatPredicate predicate)
    {
        return new SelectFloatIterable(iterable, predicate);
    }

    /**
     * Creates a deferred transforming float iterable for the specified float iterable.
     */
    public static <V> LazyIterable<V> collect(
            FloatIterable iterable,
            FloatToObjectFunction<? extends V> function)
    {
        return new CollectFloatToObjectIterable<>(iterable, function);
    }

    /**
     * Creates a deferred transforming and flattening float iterable for the specified float iterable.
     *
     * @since 9.0
     */
    public static <V> LazyIterable<V> flatCollect(
            FloatIterable iterable,
            FloatToObjectFunction<? extends Iterable<V>> function)
    {
        return new FlatCollectFloatToObjectIterable<>(iterable, function);
    }

    /**
     * Creates a deferred filtering and transforming float iterable for the specified float iterable.
     */
    public static <V> LazyIterable<V> collectIf(
            FloatIterable iterable,
            FloatPredicate predicate,
            FloatToObjectFunction<? extends V> function)
    {
        return LazyFloatIterate.select(iterable, predicate).collect(function);
    }

    public static LazyFloatIterable empty()
    {
        return EMPTY_ITERABLE;
    }

    /**
     * Creates a deferred tap iterable for the specified iterable.
     *
     * @since 9.0
     */
    public static LazyFloatIterable tap(FloatIterable iterable, FloatProcedure procedure)
    {
        return new TapFloatIterable(iterable, procedure);
    }
}
