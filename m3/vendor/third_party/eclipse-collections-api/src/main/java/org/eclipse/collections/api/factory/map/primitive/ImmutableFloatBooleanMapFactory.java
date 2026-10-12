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
import org.eclipse.collections.api.block.function.primitive.BooleanFunction;
import org.eclipse.collections.api.map.primitive.FloatBooleanMap;
import org.eclipse.collections.api.map.primitive.ImmutableFloatBooleanMap;

/**
 * A factory which creates instances of type {@link ImmutableFloatBooleanMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactory.stg.
 *
 * @since 4.0.
 */
public interface ImmutableFloatBooleanMapFactory
{
    /**
     * @since 6.0
     */
    ImmutableFloatBooleanMap empty();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableFloatBooleanMap of();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableFloatBooleanMap with();

    /**
     * Same as {@link #with(float, boolean)}.
     */
    ImmutableFloatBooleanMap of(float key, boolean value);

    ImmutableFloatBooleanMap with(float key, boolean value);

    /**
     * Same as {@link #withAll(FloatBooleanMap)}.
     */
    ImmutableFloatBooleanMap ofAll(FloatBooleanMap map);

    ImmutableFloatBooleanMap withAll(FloatBooleanMap map);

    /**
     * Creates an {@code ImmutableFloatBooleanMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T> ImmutableFloatBooleanMap from(Iterable<T> iterable, FloatFunction<? super T> keyFunction, BooleanFunction<? super T> valueFunction);
}
