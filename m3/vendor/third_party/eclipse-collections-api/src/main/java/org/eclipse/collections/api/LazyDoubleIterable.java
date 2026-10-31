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

import org.eclipse.collections.api.block.function.primitive.DoubleToBooleanFunction;
import org.eclipse.collections.api.block.function.primitive.DoubleToByteFunction;
import org.eclipse.collections.api.block.function.primitive.DoubleToCharFunction;
import org.eclipse.collections.api.block.function.primitive.DoubleToDoubleFunction;
import org.eclipse.collections.api.block.function.primitive.DoubleToFloatFunction;
import org.eclipse.collections.api.block.function.primitive.DoubleToIntFunction;
import org.eclipse.collections.api.block.function.primitive.DoubleToLongFunction;
import org.eclipse.collections.api.block.function.primitive.DoubleToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.DoubleToShortFunction;
import org.eclipse.collections.api.block.predicate.primitive.DoublePredicate;
import org.eclipse.collections.api.block.procedure.primitive.DoubleProcedure;

/**
 * This file was automatically generated from template file lazyPrimitiveIterable.stg.
 */
public interface LazyDoubleIterable extends DoubleIterable
{
    /**
     * @since 9.0.
     */
    @Override
    LazyDoubleIterable tap(DoubleProcedure procedure);

    @Override
    LazyDoubleIterable select(DoublePredicate predicate);

    @Override
    LazyDoubleIterable reject(DoublePredicate predicate);

    @Override
    <V> LazyIterable<V> collect(DoubleToObjectFunction<? extends V> function);

    /**
     * @since 9.0
     */
    <V> LazyIterable<V> flatCollect(DoubleToObjectFunction<? extends Iterable<V>> function);

    /**
     * @since 7.0
     */
    LazyBooleanIterable collectBoolean(DoubleToBooleanFunction function);

    /**
     * @since 7.0
     */
    LazyByteIterable collectByte(DoubleToByteFunction function);

    /**
     * @since 7.0
     */
    LazyCharIterable collectChar(DoubleToCharFunction function);

    /**
     * @since 7.0
     */
    LazyShortIterable collectShort(DoubleToShortFunction function);

    /**
     * @since 7.0
     */
    LazyIntIterable collectInt(DoubleToIntFunction function);

    /**
     * @since 7.0
     */
    LazyFloatIterable collectFloat(DoubleToFloatFunction function);

    /**
     * @since 7.0
     */
    LazyLongIterable collectLong(DoubleToLongFunction function);

    /**
     * @since 7.0
     */
    LazyDoubleIterable collectDouble(DoubleToDoubleFunction function);
}
