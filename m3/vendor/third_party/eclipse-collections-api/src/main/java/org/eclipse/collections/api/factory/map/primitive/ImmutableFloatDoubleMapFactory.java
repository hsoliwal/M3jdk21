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
import org.eclipse.collections.api.block.function.primitive.DoubleFunction;
import org.eclipse.collections.api.map.primitive.FloatDoubleMap;
import org.eclipse.collections.api.map.primitive.ImmutableFloatDoubleMap;

/**
 * A factory which creates instances of type {@link ImmutableFloatDoubleMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactory.stg.
 *
 * @since 4.0.
 */
public interface ImmutableFloatDoubleMapFactory
{
    /**
     * @since 6.0
     */
    ImmutableFloatDoubleMap empty();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableFloatDoubleMap of();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableFloatDoubleMap with();

    /**
     * Same as {@link #with(float, double)}.
     */
    ImmutableFloatDoubleMap of(float key, double value);

    ImmutableFloatDoubleMap with(float key, double value);

    /**
     * Same as {@link #withAll(FloatDoubleMap)}.
     */
    ImmutableFloatDoubleMap ofAll(FloatDoubleMap map);

    ImmutableFloatDoubleMap withAll(FloatDoubleMap map);

    /**
     * Creates an {@code ImmutableFloatDoubleMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T> ImmutableFloatDoubleMap from(Iterable<T> iterable, FloatFunction<? super T> keyFunction, DoubleFunction<? super T> valueFunction);
}
