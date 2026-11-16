/*
 * Copyright (c) 2026 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.api;

import jdk.m3.collections.eclipse.api.block.function.primitive.BooleanToBooleanFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.BooleanToByteFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.BooleanToCharFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.BooleanToDoubleFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.BooleanToFloatFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.BooleanToIntFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.BooleanToLongFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.BooleanToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.BooleanToShortFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.BooleanPredicate;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.BooleanProcedure;

/**
 * This file was automatically generated from template file lazyPrimitiveIterable.stg.
 */
public interface LazyBooleanIterable extends BooleanIterable
{
    /**
     * @since 9.0.
     */
    @Override
    LazyBooleanIterable tap(BooleanProcedure procedure);

    @Override
    LazyBooleanIterable select(BooleanPredicate predicate);

    @Override
    LazyBooleanIterable reject(BooleanPredicate predicate);

    @Override
    <V> LazyIterable<V> collect(BooleanToObjectFunction<? extends V> function);

    /**
     * @since 9.0
     */
    <V> LazyIterable<V> flatCollect(BooleanToObjectFunction<? extends Iterable<V>> function);

    /**
     * @since 7.0
     */
    LazyBooleanIterable collectBoolean(BooleanToBooleanFunction function);

    /**
     * @since 7.0
     */
    LazyByteIterable collectByte(BooleanToByteFunction function);

    /**
     * @since 7.0
     */
    LazyCharIterable collectChar(BooleanToCharFunction function);

    /**
     * @since 7.0
     */
    LazyShortIterable collectShort(BooleanToShortFunction function);

    /**
     * @since 7.0
     */
    LazyIntIterable collectInt(BooleanToIntFunction function);

    /**
     * @since 7.0
     */
    LazyFloatIterable collectFloat(BooleanToFloatFunction function);

    /**
     * @since 7.0
     */
    LazyLongIterable collectLong(BooleanToLongFunction function);

    /**
     * @since 7.0
     */
    LazyDoubleIterable collectDouble(BooleanToDoubleFunction function);
}
