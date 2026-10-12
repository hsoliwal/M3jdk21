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
import org.eclipse.collections.api.block.function.primitive.ByteFunction;
import org.eclipse.collections.api.map.primitive.FloatByteMap;
import org.eclipse.collections.api.map.primitive.ImmutableFloatByteMap;

/**
 * A factory which creates instances of type {@link ImmutableFloatByteMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactory.stg.
 *
 * @since 4.0.
 */
public interface ImmutableFloatByteMapFactory
{
    /**
     * @since 6.0
     */
    ImmutableFloatByteMap empty();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableFloatByteMap of();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableFloatByteMap with();

    /**
     * Same as {@link #with(float, byte)}.
     */
    ImmutableFloatByteMap of(float key, byte value);

    ImmutableFloatByteMap with(float key, byte value);

    /**
     * Same as {@link #withAll(FloatByteMap)}.
     */
    ImmutableFloatByteMap ofAll(FloatByteMap map);

    ImmutableFloatByteMap withAll(FloatByteMap map);

    /**
     * Creates an {@code ImmutableFloatByteMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T> ImmutableFloatByteMap from(Iterable<T> iterable, FloatFunction<? super T> keyFunction, ByteFunction<? super T> valueFunction);
}
