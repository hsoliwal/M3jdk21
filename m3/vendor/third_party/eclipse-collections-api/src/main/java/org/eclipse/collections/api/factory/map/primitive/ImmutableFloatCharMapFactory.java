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
import org.eclipse.collections.api.block.function.primitive.CharFunction;
import org.eclipse.collections.api.map.primitive.FloatCharMap;
import org.eclipse.collections.api.map.primitive.ImmutableFloatCharMap;

/**
 * A factory which creates instances of type {@link ImmutableFloatCharMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactory.stg.
 *
 * @since 4.0.
 */
public interface ImmutableFloatCharMapFactory
{
    /**
     * @since 6.0
     */
    ImmutableFloatCharMap empty();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableFloatCharMap of();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableFloatCharMap with();

    /**
     * Same as {@link #with(float, char)}.
     */
    ImmutableFloatCharMap of(float key, char value);

    ImmutableFloatCharMap with(float key, char value);

    /**
     * Same as {@link #withAll(FloatCharMap)}.
     */
    ImmutableFloatCharMap ofAll(FloatCharMap map);

    ImmutableFloatCharMap withAll(FloatCharMap map);

    /**
     * Creates an {@code ImmutableFloatCharMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T> ImmutableFloatCharMap from(Iterable<T> iterable, FloatFunction<? super T> keyFunction, CharFunction<? super T> valueFunction);
}
