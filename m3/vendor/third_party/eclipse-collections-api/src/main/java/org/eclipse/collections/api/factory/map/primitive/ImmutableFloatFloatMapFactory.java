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

import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.map.primitive.FloatFloatMap;
import org.eclipse.collections.api.map.primitive.ImmutableFloatFloatMap;

/**
 * A factory which creates instances of type {@link ImmutableFloatFloatMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactory.stg.
 *
 * @since 4.0.
 */
public interface ImmutableFloatFloatMapFactory
{
    /**
     * @since 6.0
     */
    ImmutableFloatFloatMap empty();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableFloatFloatMap of();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableFloatFloatMap with();

    /**
     * Same as {@link #with(float, float)}.
     */
    ImmutableFloatFloatMap of(float key, float value);

    ImmutableFloatFloatMap with(float key, float value);

    /**
     * Same as {@link #withAll(FloatFloatMap)}.
     */
    ImmutableFloatFloatMap ofAll(FloatFloatMap map);

    ImmutableFloatFloatMap withAll(FloatFloatMap map);

    /**
     * Creates an {@code ImmutableFloatFloatMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T> ImmutableFloatFloatMap from(Iterable<T> iterable, FloatFunction<? super T> keyFunction, FloatFunction<? super T> valueFunction);
}
