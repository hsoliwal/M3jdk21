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
import org.eclipse.collections.api.block.function.primitive.ShortFunction;
import org.eclipse.collections.api.map.primitive.FloatShortMap;
import org.eclipse.collections.api.map.primitive.ImmutableFloatShortMap;

/**
 * A factory which creates instances of type {@link ImmutableFloatShortMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactory.stg.
 *
 * @since 4.0.
 */
public interface ImmutableFloatShortMapFactory
{
    /**
     * @since 6.0
     */
    ImmutableFloatShortMap empty();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableFloatShortMap of();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableFloatShortMap with();

    /**
     * Same as {@link #with(float, short)}.
     */
    ImmutableFloatShortMap of(float key, short value);

    ImmutableFloatShortMap with(float key, short value);

    /**
     * Same as {@link #withAll(FloatShortMap)}.
     */
    ImmutableFloatShortMap ofAll(FloatShortMap map);

    ImmutableFloatShortMap withAll(FloatShortMap map);

    /**
     * Creates an {@code ImmutableFloatShortMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T> ImmutableFloatShortMap from(Iterable<T> iterable, FloatFunction<? super T> keyFunction, ShortFunction<? super T> valueFunction);
}
