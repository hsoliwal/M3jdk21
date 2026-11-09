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

import org.eclipse.collections.api.block.function.primitive.FloatToBooleanFunction;
import org.eclipse.collections.api.block.function.primitive.FloatToByteFunction;
import org.eclipse.collections.api.block.function.primitive.FloatToCharFunction;
import org.eclipse.collections.api.block.function.primitive.FloatToDoubleFunction;
import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;
import org.eclipse.collections.api.block.function.primitive.FloatToIntFunction;
import org.eclipse.collections.api.block.function.primitive.FloatToLongFunction;
import org.eclipse.collections.api.block.function.primitive.FloatToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.FloatToShortFunction;
import org.eclipse.collections.api.block.predicate.primitive.FloatPredicate;
import org.eclipse.collections.api.block.procedure.primitive.FloatProcedure;

/**
 * This file was automatically generated from template file lazyPrimitiveIterable.stg.
 */
public interface LazyFloatIterable extends FloatIterable
{
    /**
     * @since 9.0.
     */
    @Override
    LazyFloatIterable tap(FloatProcedure procedure);

    @Override
    LazyFloatIterable select(FloatPredicate predicate);

    @Override
    LazyFloatIterable reject(FloatPredicate predicate);

    @Override
    <V> LazyIterable<V> collect(FloatToObjectFunction<? extends V> function);

    /**
     * @since 9.0
     */
    <V> LazyIterable<V> flatCollect(FloatToObjectFunction<? extends Iterable<V>> function);

    /**
     * @since 7.0
     */
    LazyBooleanIterable collectBoolean(FloatToBooleanFunction function);

    /**
     * @since 7.0
     */
    LazyByteIterable collectByte(FloatToByteFunction function);

    /**
     * @since 7.0
     */
    LazyCharIterable collectChar(FloatToCharFunction function);

    /**
     * @since 7.0
     */
    LazyShortIterable collectShort(FloatToShortFunction function);

    /**
     * @since 7.0
     */
    LazyIntIterable collectInt(FloatToIntFunction function);

    /**
     * @since 7.0
     */
    LazyFloatIterable collectFloat(FloatToFloatFunction function);

    /**
     * @since 7.0
     */
    LazyLongIterable collectLong(FloatToLongFunction function);

    /**
     * @since 7.0
     */
    LazyDoubleIterable collectDouble(FloatToDoubleFunction function);
}
