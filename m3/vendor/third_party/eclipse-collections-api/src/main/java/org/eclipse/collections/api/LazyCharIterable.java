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

import org.eclipse.collections.api.block.function.primitive.CharToBooleanFunction;
import org.eclipse.collections.api.block.function.primitive.CharToByteFunction;
import org.eclipse.collections.api.block.function.primitive.CharToCharFunction;
import org.eclipse.collections.api.block.function.primitive.CharToDoubleFunction;
import org.eclipse.collections.api.block.function.primitive.CharToFloatFunction;
import org.eclipse.collections.api.block.function.primitive.CharToIntFunction;
import org.eclipse.collections.api.block.function.primitive.CharToLongFunction;
import org.eclipse.collections.api.block.function.primitive.CharToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.CharToShortFunction;
import org.eclipse.collections.api.block.predicate.primitive.CharPredicate;
import org.eclipse.collections.api.block.procedure.primitive.CharProcedure;

/**
 * This file was automatically generated from template file lazyPrimitiveIterable.stg.
 */
public interface LazyCharIterable extends CharIterable
{
    /**
     * @since 9.0.
     */
    @Override
    LazyCharIterable tap(CharProcedure procedure);

    @Override
    LazyCharIterable select(CharPredicate predicate);

    @Override
    LazyCharIterable reject(CharPredicate predicate);

    @Override
    <V> LazyIterable<V> collect(CharToObjectFunction<? extends V> function);

    /**
     * @since 9.0
     */
    <V> LazyIterable<V> flatCollect(CharToObjectFunction<? extends Iterable<V>> function);

    /**
     * @since 7.0
     */
    LazyBooleanIterable collectBoolean(CharToBooleanFunction function);

    /**
     * @since 7.0
     */
    LazyByteIterable collectByte(CharToByteFunction function);

    /**
     * @since 7.0
     */
    LazyCharIterable collectChar(CharToCharFunction function);

    /**
     * @since 7.0
     */
    LazyShortIterable collectShort(CharToShortFunction function);

    /**
     * @since 7.0
     */
    LazyIntIterable collectInt(CharToIntFunction function);

    /**
     * @since 7.0
     */
    LazyFloatIterable collectFloat(CharToFloatFunction function);

    /**
     * @since 7.0
     */
    LazyLongIterable collectLong(CharToLongFunction function);

    /**
     * @since 7.0
     */
    LazyDoubleIterable collectDouble(CharToDoubleFunction function);
}
