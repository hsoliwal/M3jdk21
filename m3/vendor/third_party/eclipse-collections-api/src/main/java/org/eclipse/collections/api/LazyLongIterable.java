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

import org.eclipse.collections.api.block.function.primitive.LongToBooleanFunction;
import org.eclipse.collections.api.block.function.primitive.LongToByteFunction;
import org.eclipse.collections.api.block.function.primitive.LongToCharFunction;
import org.eclipse.collections.api.block.function.primitive.LongToDoubleFunction;
import org.eclipse.collections.api.block.function.primitive.LongToFloatFunction;
import org.eclipse.collections.api.block.function.primitive.LongToIntFunction;
import org.eclipse.collections.api.block.function.primitive.LongToLongFunction;
import org.eclipse.collections.api.block.function.primitive.LongToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.LongToShortFunction;
import org.eclipse.collections.api.block.predicate.primitive.LongPredicate;
import org.eclipse.collections.api.block.procedure.primitive.LongProcedure;

/**
 * This file was automatically generated from template file lazyPrimitiveIterable.stg.
 */
public interface LazyLongIterable extends LongIterable
{
    /**
     * @since 9.0.
     */
    @Override
    LazyLongIterable tap(LongProcedure procedure);

    @Override
    LazyLongIterable select(LongPredicate predicate);

    @Override
    LazyLongIterable reject(LongPredicate predicate);

    @Override
    <V> LazyIterable<V> collect(LongToObjectFunction<? extends V> function);

    /**
     * @since 9.0
     */
    <V> LazyIterable<V> flatCollect(LongToObjectFunction<? extends Iterable<V>> function);

    /**
     * @since 7.0
     */
    LazyBooleanIterable collectBoolean(LongToBooleanFunction function);

    /**
     * @since 7.0
     */
    LazyByteIterable collectByte(LongToByteFunction function);

    /**
     * @since 7.0
     */
    LazyCharIterable collectChar(LongToCharFunction function);

    /**
     * @since 7.0
     */
    LazyShortIterable collectShort(LongToShortFunction function);

    /**
     * @since 7.0
     */
    LazyIntIterable collectInt(LongToIntFunction function);

    /**
     * @since 7.0
     */
    LazyFloatIterable collectFloat(LongToFloatFunction function);

    /**
     * @since 7.0
     */
    LazyLongIterable collectLong(LongToLongFunction function);

    /**
     * @since 7.0
     */
    LazyDoubleIterable collectDouble(LongToDoubleFunction function);
}
