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
import org.eclipse.collections.api.block.function.primitive.LongFunction;
import org.eclipse.collections.api.map.primitive.FloatLongMap;
import org.eclipse.collections.api.map.primitive.ImmutableFloatLongMap;

/**
 * A factory which creates instances of type {@link ImmutableFloatLongMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactory.stg.
 *
 * @since 4.0.
 */
public interface ImmutableFloatLongMapFactory
{
    /**
     * @since 6.0
     */
    ImmutableFloatLongMap empty();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableFloatLongMap of();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableFloatLongMap with();

    /**
     * Same as {@link #with(float, long)}.
     */
    ImmutableFloatLongMap of(float key, long value);

    ImmutableFloatLongMap with(float key, long value);

    /**
     * Same as {@link #withAll(FloatLongMap)}.
     */
    ImmutableFloatLongMap ofAll(FloatLongMap map);

    ImmutableFloatLongMap withAll(FloatLongMap map);

    /**
     * Creates an {@code ImmutableFloatLongMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T> ImmutableFloatLongMap from(Iterable<T> iterable, FloatFunction<? super T> keyFunction, LongFunction<? super T> valueFunction);
}
