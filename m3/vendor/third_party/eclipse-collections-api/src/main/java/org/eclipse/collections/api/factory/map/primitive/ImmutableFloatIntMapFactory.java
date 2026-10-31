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
import org.eclipse.collections.api.block.function.primitive.IntFunction;
import org.eclipse.collections.api.map.primitive.FloatIntMap;
import org.eclipse.collections.api.map.primitive.ImmutableFloatIntMap;

/**
 * A factory which creates instances of type {@link ImmutableFloatIntMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactory.stg.
 *
 * @since 4.0.
 */
public interface ImmutableFloatIntMapFactory
{
    /**
     * @since 6.0
     */
    ImmutableFloatIntMap empty();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableFloatIntMap of();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableFloatIntMap with();

    /**
     * Same as {@link #with(float, int)}.
     */
    ImmutableFloatIntMap of(float key, int value);

    ImmutableFloatIntMap with(float key, int value);

    /**
     * Same as {@link #withAll(FloatIntMap)}.
     */
    ImmutableFloatIntMap ofAll(FloatIntMap map);

    ImmutableFloatIntMap withAll(FloatIntMap map);

    /**
     * Creates an {@code ImmutableFloatIntMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T> ImmutableFloatIntMap from(Iterable<T> iterable, FloatFunction<? super T> keyFunction, IntFunction<? super T> valueFunction);
}
