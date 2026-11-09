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

import org.eclipse.collections.api.block.function.primitive.ByteToBooleanFunction;
import org.eclipse.collections.api.block.function.primitive.ByteToByteFunction;
import org.eclipse.collections.api.block.function.primitive.ByteToCharFunction;
import org.eclipse.collections.api.block.function.primitive.ByteToDoubleFunction;
import org.eclipse.collections.api.block.function.primitive.ByteToFloatFunction;
import org.eclipse.collections.api.block.function.primitive.ByteToIntFunction;
import org.eclipse.collections.api.block.function.primitive.ByteToLongFunction;
import org.eclipse.collections.api.block.function.primitive.ByteToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ByteToShortFunction;
import org.eclipse.collections.api.block.predicate.primitive.BytePredicate;
import org.eclipse.collections.api.block.procedure.primitive.ByteProcedure;

/**
 * This file was automatically generated from template file lazyPrimitiveIterable.stg.
 */
public interface LazyByteIterable extends ByteIterable
{
    /**
     * @since 9.0.
     */
    @Override
    LazyByteIterable tap(ByteProcedure procedure);

    @Override
    LazyByteIterable select(BytePredicate predicate);

    @Override
    LazyByteIterable reject(BytePredicate predicate);

    @Override
    <V> LazyIterable<V> collect(ByteToObjectFunction<? extends V> function);

    /**
     * @since 9.0
     */
    <V> LazyIterable<V> flatCollect(ByteToObjectFunction<? extends Iterable<V>> function);

    /**
     * @since 7.0
     */
    LazyBooleanIterable collectBoolean(ByteToBooleanFunction function);

    /**
     * @since 7.0
     */
    LazyByteIterable collectByte(ByteToByteFunction function);

    /**
     * @since 7.0
     */
    LazyCharIterable collectChar(ByteToCharFunction function);

    /**
     * @since 7.0
     */
    LazyShortIterable collectShort(ByteToShortFunction function);

    /**
     * @since 7.0
     */
    LazyIntIterable collectInt(ByteToIntFunction function);

    /**
     * @since 7.0
     */
    LazyFloatIterable collectFloat(ByteToFloatFunction function);

    /**
     * @since 7.0
     */
    LazyLongIterable collectLong(ByteToLongFunction function);

    /**
     * @since 7.0
     */
    LazyDoubleIterable collectDouble(ByteToDoubleFunction function);
}
