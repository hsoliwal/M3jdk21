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

import org.eclipse.collections.api.block.function.primitive.IntToBooleanFunction;
import org.eclipse.collections.api.block.function.primitive.IntToByteFunction;
import org.eclipse.collections.api.block.function.primitive.IntToCharFunction;
import org.eclipse.collections.api.block.function.primitive.IntToDoubleFunction;
import org.eclipse.collections.api.block.function.primitive.IntToFloatFunction;
import org.eclipse.collections.api.block.function.primitive.IntToIntFunction;
import org.eclipse.collections.api.block.function.primitive.IntToLongFunction;
import org.eclipse.collections.api.block.function.primitive.IntToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.IntToShortFunction;
import org.eclipse.collections.api.block.predicate.primitive.IntPredicate;
import org.eclipse.collections.api.block.procedure.primitive.IntProcedure;

/**
 * This file was automatically generated from template file lazyPrimitiveIterable.stg.
 */
public interface LazyIntIterable extends IntIterable
{
    /**
     * @since 9.0.
     */
    @Override
    LazyIntIterable tap(IntProcedure procedure);

    @Override
    LazyIntIterable select(IntPredicate predicate);

    @Override
    LazyIntIterable reject(IntPredicate predicate);

    @Override
    <V> LazyIterable<V> collect(IntToObjectFunction<? extends V> function);

    /**
     * @since 9.0
     */
    <V> LazyIterable<V> flatCollect(IntToObjectFunction<? extends Iterable<V>> function);

    /**
     * @since 7.0
     */
    LazyBooleanIterable collectBoolean(IntToBooleanFunction function);

    /**
     * @since 7.0
     */
    LazyByteIterable collectByte(IntToByteFunction function);

    /**
     * @since 7.0
     */
    LazyCharIterable collectChar(IntToCharFunction function);

    /**
     * @since 7.0
     */
    LazyShortIterable collectShort(IntToShortFunction function);

    /**
     * @since 7.0
     */
    LazyIntIterable collectInt(IntToIntFunction function);

    /**
     * @since 7.0
     */
    LazyFloatIterable collectFloat(IntToFloatFunction function);

    /**
     * @since 7.0
     */
    LazyLongIterable collectLong(IntToLongFunction function);

    /**
     * @since 7.0
     */
    LazyDoubleIterable collectDouble(IntToDoubleFunction function);
}
