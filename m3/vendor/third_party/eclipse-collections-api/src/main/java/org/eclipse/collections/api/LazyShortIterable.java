/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.api;

import org.eclipse.collections.api.block.function.primitive.ShortToBooleanFunction;
import org.eclipse.collections.api.block.function.primitive.ShortToByteFunction;
import org.eclipse.collections.api.block.function.primitive.ShortToCharFunction;
import org.eclipse.collections.api.block.function.primitive.ShortToDoubleFunction;
import org.eclipse.collections.api.block.function.primitive.ShortToFloatFunction;
import org.eclipse.collections.api.block.function.primitive.ShortToIntFunction;
import org.eclipse.collections.api.block.function.primitive.ShortToLongFunction;
import org.eclipse.collections.api.block.function.primitive.ShortToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ShortToShortFunction;
import org.eclipse.collections.api.block.predicate.primitive.ShortPredicate;
import org.eclipse.collections.api.block.procedure.primitive.ShortProcedure;

/**
 * This file was automatically generated from template file lazyPrimitiveIterable.stg.
 */
public interface LazyShortIterable extends ShortIterable
{
    /**
     * @since 9.0.
     */
    @Override
    LazyShortIterable tap(ShortProcedure procedure);

    @Override
    LazyShortIterable select(ShortPredicate predicate);

    @Override
    LazyShortIterable reject(ShortPredicate predicate);

    @Override
    <V> LazyIterable<V> collect(ShortToObjectFunction<? extends V> function);

    /**
     * @since 9.0
     */
    <V> LazyIterable<V> flatCollect(ShortToObjectFunction<? extends Iterable<V>> function);

    /**
     * @since 7.0
     */
    LazyBooleanIterable collectBoolean(ShortToBooleanFunction function);

    /**
     * @since 7.0
     */
    LazyByteIterable collectByte(ShortToByteFunction function);

    /**
     * @since 7.0
     */
    LazyCharIterable collectChar(ShortToCharFunction function);

    /**
     * @since 7.0
     */
    LazyShortIterable collectShort(ShortToShortFunction function);

    /**
     * @since 7.0
     */
    LazyIntIterable collectInt(ShortToIntFunction function);

    /**
     * @since 7.0
     */
    LazyFloatIterable collectFloat(ShortToFloatFunction function);

    /**
     * @since 7.0
     */
    LazyLongIterable collectLong(ShortToLongFunction function);

    /**
     * @since 7.0
     */
    LazyDoubleIterable collectDouble(ShortToDoubleFunction function);
}
