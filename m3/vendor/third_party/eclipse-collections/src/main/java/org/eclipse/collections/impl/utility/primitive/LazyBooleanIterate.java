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

import org.eclipse.collections.api.BooleanIterable;
import org.eclipse.collections.api.LazyBooleanIterable;
import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.block.function.primitive.BooleanToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.BooleanPredicate;
import org.eclipse.collections.api.block.procedure.primitive.BooleanProcedure;
import org.eclipse.collections.impl.factory.primitive.BooleanLists;
import org.eclipse.collections.impl.lazy.primitive.CollectBooleanToObjectIterable;
import org.eclipse.collections.impl.lazy.primitive.FlatCollectBooleanToObjectIterable;
import org.eclipse.collections.impl.lazy.primitive.LazyBooleanIterableAdapter;
import org.eclipse.collections.impl.lazy.primitive.SelectBooleanIterable;
import org.eclipse.collections.impl.lazy.primitive.TapBooleanIterable;

/**
 * LazyBooleanIterate is a factory class which creates "deferred" boolean iterables around the specified boolean iterables. A "deferred"
 * boolean iterable performs some operation, such as filtering or transforming, when the result boolean iterable is iterated over.  This
 * makes the operation very memory efficient, because you don't have to create intermediate collections during the
 * operation.
 * This file was automatically generated from template file lazyPrimitiveIterate.stg.
 *
 * @since 5.0
 */
public final class LazyBooleanIterate
{
    private static final LazyBooleanIterable EMPTY_ITERABLE = BooleanLists.immutable.empty().asLazy();

    private LazyBooleanIterate()
    {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }

    /**
     * Creates a deferred boolean iterable for the specified boolean iterable.
     */
    public static LazyBooleanIterable adapt(BooleanIterable iterable)
    {
        return new LazyBooleanIterableAdapter(iterable);
    }

    /**
     * Creates a deferred filtering boolean iterable for the specified boolean iterable.
     */
    public static LazyBooleanIterable select(BooleanIterable iterable, BooleanPredicate predicate)
    {
        return new SelectBooleanIterable(iterable, predicate);
    }

    /**
     * Creates a deferred transforming boolean iterable for the specified boolean iterable.
     */
    public static <V> LazyIterable<V> collect(
            BooleanIterable iterable,
            BooleanToObjectFunction<? extends V> function)
    {
        return new CollectBooleanToObjectIterable<>(iterable, function);
    }

    /**
     * Creates a deferred transforming and flattening boolean iterable for the specified boolean iterable.
     *
     * @since 9.0
     */
    public static <V> LazyIterable<V> flatCollect(
            BooleanIterable iterable,
            BooleanToObjectFunction<? extends Iterable<V>> function)
    {
        return new FlatCollectBooleanToObjectIterable<>(iterable, function);
    }

    /**
     * Creates a deferred filtering and transforming boolean iterable for the specified boolean iterable.
     */
    public static <V> LazyIterable<V> collectIf(
            BooleanIterable iterable,
            BooleanPredicate predicate,
            BooleanToObjectFunction<? extends V> function)
    {
        return LazyBooleanIterate.select(iterable, predicate).collect(function);
    }

    public static LazyBooleanIterable empty()
    {
        return EMPTY_ITERABLE;
    }

    /**
     * Creates a deferred tap iterable for the specified iterable.
     *
     * @since 9.0
     */
    public static LazyBooleanIterable tap(BooleanIterable iterable, BooleanProcedure procedure)
    {
        return new TapBooleanIterable(iterable, procedure);
    }
}
