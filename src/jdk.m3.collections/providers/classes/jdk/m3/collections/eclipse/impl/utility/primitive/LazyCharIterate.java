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

import jdk.m3.collections.eclipse.api.CharIterable;
import jdk.m3.collections.eclipse.api.LazyCharIterable;
import jdk.m3.collections.eclipse.api.LazyIterable;
import jdk.m3.collections.eclipse.api.block.function.primitive.CharToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.CharPredicate;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.CharProcedure;
import jdk.m3.collections.eclipse.impl.factory.primitive.CharLists;
import jdk.m3.collections.eclipse.impl.lazy.primitive.CollectCharToObjectIterable;
import jdk.m3.collections.eclipse.impl.lazy.primitive.FlatCollectCharToObjectIterable;
import jdk.m3.collections.eclipse.impl.lazy.primitive.LazyCharIterableAdapter;
import jdk.m3.collections.eclipse.impl.lazy.primitive.SelectCharIterable;
import jdk.m3.collections.eclipse.impl.lazy.primitive.TapCharIterable;

/**
 * LazyCharIterate is a factory class which creates "deferred" char iterables around the specified char iterables. A "deferred"
 * char iterable performs some operation, such as filtering or transforming, when the result char iterable is iterated over.  This
 * makes the operation very memory efficient, because you don't have to create intermediate collections during the
 * operation.
 * This file was automatically generated from template file lazyPrimitiveIterate.stg.
 *
 * @since 5.0
 */
public final class LazyCharIterate
{
    private static final LazyCharIterable EMPTY_ITERABLE = CharLists.immutable.empty().asLazy();

    private LazyCharIterate()
    {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }

    /**
     * Creates a deferred char iterable for the specified char iterable.
     */
    public static LazyCharIterable adapt(CharIterable iterable)
    {
        return new LazyCharIterableAdapter(iterable);
    }

    /**
     * Creates a deferred filtering char iterable for the specified char iterable.
     */
    public static LazyCharIterable select(CharIterable iterable, CharPredicate predicate)
    {
        return new SelectCharIterable(iterable, predicate);
    }

    /**
     * Creates a deferred transforming char iterable for the specified char iterable.
     */
    public static <V> LazyIterable<V> collect(
            CharIterable iterable,
            CharToObjectFunction<? extends V> function)
    {
        return new CollectCharToObjectIterable<>(iterable, function);
    }

    /**
     * Creates a deferred transforming and flattening char iterable for the specified char iterable.
     *
     * @since 9.0
     */
    public static <V> LazyIterable<V> flatCollect(
            CharIterable iterable,
            CharToObjectFunction<? extends Iterable<V>> function)
    {
        return new FlatCollectCharToObjectIterable<>(iterable, function);
    }

    /**
     * Creates a deferred filtering and transforming char iterable for the specified char iterable.
     */
    public static <V> LazyIterable<V> collectIf(
            CharIterable iterable,
            CharPredicate predicate,
            CharToObjectFunction<? extends V> function)
    {
        return LazyCharIterate.select(iterable, predicate).collect(function);
    }

    public static LazyCharIterable empty()
    {
        return EMPTY_ITERABLE;
    }

    /**
     * Creates a deferred tap iterable for the specified iterable.
     *
     * @since 9.0
     */
    public static LazyCharIterable tap(CharIterable iterable, CharProcedure procedure)
    {
        return new TapCharIterable(iterable, procedure);
    }
}
