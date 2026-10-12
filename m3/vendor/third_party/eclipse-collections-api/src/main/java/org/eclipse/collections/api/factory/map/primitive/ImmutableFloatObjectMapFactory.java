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

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.map.primitive.ImmutableFloatObjectMap;
import org.eclipse.collections.api.map.primitive.FloatObjectMap;

/**
 * A factory which creates instances of type {@link ImmutableFloatObjectMap}.
 * This file was automatically generated from template file immutablePrimitiveObjectMapFactory.stg.
 *
 * @since 4.0.
 */
public interface ImmutableFloatObjectMapFactory
{
    /**
     * @since 6.0
     */
    <V> ImmutableFloatObjectMap<V> empty();

    /**
     * Same as {@link #empty()}.
     */
    <V> ImmutableFloatObjectMap<V> of();

    /**
     * Same as {@link #empty()}.
     */
    <V> ImmutableFloatObjectMap<V> with();

    /**
     * Same as {@link #with(float, Object)}.
     */
    <V> ImmutableFloatObjectMap<V> of(float key, V value);

    <V> ImmutableFloatObjectMap<V> with(float key, V value);

    /**
     * Same as {@link #withAll(FloatObjectMap)}.
     */
    <V> ImmutableFloatObjectMap<V> ofAll(FloatObjectMap<? extends V> map);

    <V> ImmutableFloatObjectMap<V> withAll(FloatObjectMap<? extends V> map);

    /**
     * Creates an {@code ImmutableFloatObjectMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T, V> ImmutableFloatObjectMap<V> from(Iterable<T> iterable, FloatFunction<? super T> keyFunction, Function<? super T, ? extends V> valueFunction);
}
