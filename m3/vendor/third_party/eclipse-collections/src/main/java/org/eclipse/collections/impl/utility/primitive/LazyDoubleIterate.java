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

import org.eclipse.collections.api.DoubleIterable;
import org.eclipse.collections.api.LazyDoubleIterable;
import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.block.function.primitive.DoubleToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.DoublePredicate;
import org.eclipse.collections.api.block.procedure.primitive.DoubleProcedure;
import org.eclipse.collections.impl.factory.primitive.DoubleLists;
import org.eclipse.collections.impl.lazy.primitive.CollectDoubleToObjectIterable;
import org.eclipse.collections.impl.lazy.primitive.FlatCollectDoubleToObjectIterable;
import org.eclipse.collections.impl.lazy.primitive.LazyDoubleIterableAdapter;
import org.eclipse.collections.impl.lazy.primitive.SelectDoubleIterable;
import org.eclipse.collections.impl.lazy.primitive.TapDoubleIterable;

/**
 * LazyDoubleIterate is a factory class which creates "deferred" double iterables around the specified double iterables. A "deferred"
 * double iterable performs some operation, such as filtering or transforming, when the result double iterable is iterated over.  This
 * makes the operation very memory efficient, because you don't have to create intermediate collections during the
 * operation.
 * This file was automatically generated from template file lazyPrimitiveIterate.stg.
 *
 * @since 5.0
 */
public final class LazyDoubleIterate
{
    private static final LazyDoubleIterable EMPTY_ITERABLE = DoubleLists.immutable.empty().asLazy();

    private LazyDoubleIterate()
    {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }

    /**
     * Creates a deferred double iterable for the specified double iterable.
     */
    public static LazyDoubleIterable adapt(DoubleIterable iterable)
    {
        return new LazyDoubleIterableAdapter(iterable);
    }

    /**
     * Creates a deferred filtering double iterable for the specified double iterable.
     */
    public static LazyDoubleIterable select(DoubleIterable iterable, DoublePredicate predicate)
    {
        return new SelectDoubleIterable(iterable, predicate);
    }

    /**
     * Creates a deferred transforming double iterable for the specified double iterable.
     */
    public static <V> LazyIterable<V> collect(
            DoubleIterable iterable,
            DoubleToObjectFunction<? extends V> function)
    {
        return new CollectDoubleToObjectIterable<>(iterable, function);
    }

    /**
     * Creates a deferred transforming and flattening double iterable for the specified double iterable.
     *
     * @since 9.0
     */
    public static <V> LazyIterable<V> flatCollect(
            DoubleIterable iterable,
            DoubleToObjectFunction<? extends Iterable<V>> function)
    {
        return new FlatCollectDoubleToObjectIterable<>(iterable, function);
    }

    /**
     * Creates a deferred filtering and transforming double iterable for the specified double iterable.
     */
    public static <V> LazyIterable<V> collectIf(
            DoubleIterable iterable,
            DoublePredicate predicate,
            DoubleToObjectFunction<? extends V> function)
    {
        return LazyDoubleIterate.select(iterable, predicate).collect(function);
    }

    public static LazyDoubleIterable empty()
    {
        return EMPTY_ITERABLE;
    }

    /**
     * Creates a deferred tap iterable for the specified iterable.
     *
     * @since 9.0
     */
    public static LazyDoubleIterable tap(DoubleIterable iterable, DoubleProcedure procedure)
    {
        return new TapDoubleIterable(iterable, procedure);
    }
}
