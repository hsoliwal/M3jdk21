/*
 * Copyright (c) 2022 Goldman Sachs.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.api.factory.map.primitive;

import org.eclipse.collections.api.block.function.primitive.LongFunction;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.map.primitive.LongFloatMap;
import org.eclipse.collections.api.map.primitive.ImmutableLongFloatMap;

/**
 * A factory which creates instances of type {@link ImmutableLongFloatMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactory.stg.
 *
 * @since 4.0.
 */
public interface ImmutableLongFloatMapFactory
{
    /**
     * @since 6.0
     */
    ImmutableLongFloatMap empty();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableLongFloatMap of();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableLongFloatMap with();

    /**
     * Same as {@link #with(long, float)}.
     */
    ImmutableLongFloatMap of(long key, float value);

    ImmutableLongFloatMap with(long key, float value);

    /**
     * Same as {@link #withAll(LongFloatMap)}.
     */
    ImmutableLongFloatMap ofAll(LongFloatMap map);

    ImmutableLongFloatMap withAll(LongFloatMap map);

    /**
     * Creates an {@code ImmutableLongFloatMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T> ImmutableLongFloatMap from(Iterable<T> iterable, LongFunction<? super T> keyFunction, FloatFunction<? super T> valueFunction);
}
