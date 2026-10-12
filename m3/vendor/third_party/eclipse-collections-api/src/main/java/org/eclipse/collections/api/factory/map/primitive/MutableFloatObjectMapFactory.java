/*
 * Copyright (c) 2022 Goldman Sachs and others.
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
import org.eclipse.collections.api.map.primitive.MutableFloatObjectMap;
import org.eclipse.collections.api.map.primitive.FloatObjectMap;

/**
 * A factory which creates instances of type {@link MutableFloatObjectMap}.
 * This file was automatically generated from template file mutablePrimitiveObjectMapFactory.stg.
 *
 * @since 6.0.
 */
public interface MutableFloatObjectMapFactory
{
    <V> MutableFloatObjectMap<V> empty();

    /**
     * Same as {@link #empty()}.
     */
    <V> MutableFloatObjectMap<V> of();

    /**
     * Same as {@link #empty()}.
     */
    <V> MutableFloatObjectMap<V> with();

    /**
     * @since 11.1.
     */
    default <V> MutableFloatObjectMap<V> of(float key, V value)
    {
        return this.with(key, value);
    }

    /**
     * @since 11.1.
     */
    default <V> MutableFloatObjectMap<V> with(float key, V value)
    {
        return this.<V>with().withKeyValue(key, value);
    }

    /**
     * @since 11.1.
     */
    default <V> MutableFloatObjectMap<V> of(float key1, V value1, float key2, V value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    /**
     * @since 11.1.
     */
    default <V> MutableFloatObjectMap<V> with(float key1, V value1, float key2, V value2)
    {
        return this.with(key1, value1).withKeyValue(key2, value2);
    }

    /**
     * @since 11.1.
     */
    default <V> MutableFloatObjectMap<V> of(float key1, V value1, float key2, V value2, float key3, V value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    /**
     * @since 11.1.
     */
    default <V> MutableFloatObjectMap<V> with(float key1, V value1, float key2, V value2, float key3, V value3)
    {
        return this.with(key1, value1, key2, value2).withKeyValue(key3, value3);
    }

    /**
     * @since 11.1.
     */
    default <V> MutableFloatObjectMap<V> of(float key1, V value1, float key2, V value2, float key3, V value3, float key4, V value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    /**
     * @since 11.1.
     */
    default <V> MutableFloatObjectMap<V> with(float key1, V value1, float key2, V value2, float key3, V value3, float key4, V value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3).withKeyValue(key4, value4);
    }

    /**
     * Same as {@link #empty()}. but takes in an initial capacity
     */
    <V> MutableFloatObjectMap<V> ofInitialCapacity(int capacity);

    /**
     * Same as {@link #empty()}. but takes in an initial capacity
     */
    <V> MutableFloatObjectMap<V> withInitialCapacity(int capacity);

    /**
     * Same as {@link #withAll(FloatObjectMap)}.
     */
    <V> MutableFloatObjectMap<V> ofAll(FloatObjectMap<? extends V> map);

    <V> MutableFloatObjectMap<V> withAll(FloatObjectMap<? extends V> map);

    /**
     * Creates an {@code MutableFloatObjectMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T, V> MutableFloatObjectMap<V> from(Iterable<T> iterable, FloatFunction<? super T> keyFunction, Function<? super T, ? extends V> valueFunction);
}
