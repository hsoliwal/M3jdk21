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

import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.block.function.primitive.ByteFunction;
import org.eclipse.collections.api.map.primitive.MutableFloatByteMap;
import org.eclipse.collections.api.map.primitive.FloatByteMap;

/**
 * A factory which creates instances of type {@link MutableFloatByteMap}.
 * This file was automatically generated from template file mutablePrimitivePrimitiveMapFactory.stg.
 *
 * @since 6.0.
 */
public interface MutableFloatByteMapFactory
{
    MutableFloatByteMap empty();

    /**
     * Same as {@link #empty()}.
     */
    MutableFloatByteMap of();

    /**
     * Same as {@link #empty()}.
     */
    MutableFloatByteMap with();

    /**
     * @since 11.1.
     */
    default MutableFloatByteMap of(float key, byte value)
    {
        return this.with(key, value);
    }

    /**
     * @since 11.1.
     */
    default MutableFloatByteMap with(float key, byte value)
    {
        return this.with().withKeyValue(key, value);
    }

    /**
     * @since 11.1.
     */
    default MutableFloatByteMap of(float key1, byte value1, float key2, byte value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    /**
     * @since 11.1.
     */
    default MutableFloatByteMap with(float key1, byte value1, float key2, byte value2)
    {
        return this.with(key1, value1).withKeyValue(key1, value2);
    }

    /**
     * @since 11.1.
     */
    default MutableFloatByteMap of(float key1, byte value1, float key2, byte value2, float key3, byte value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    /**
     * @since 11.1.
     */
    default MutableFloatByteMap with(float key1, byte value1, float key2, byte value2, float key3, byte value3)
    {
        return this.with(key1, value1, key2, value2).withKeyValue(key3, value3);
    }

    /**
     * @since 11.1.
     */
    default MutableFloatByteMap of(float key1, byte value1, float key2, byte value2, float key3, byte value3, float key4, byte value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    /**
     * @since 11.1.
     */
    default MutableFloatByteMap with(float key1, byte value1, float key2, byte value2, float key3, byte value3, float key4, byte value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3).withKeyValue(key4, value4);
    }

    /**
     * Same as {@link #empty()}. but takes in an initial capacity
     */
    MutableFloatByteMap ofInitialCapacity(int capacity);

    /**
     * Same as {@link #empty()}. but takes in an initial capacity
     */
    MutableFloatByteMap withInitialCapacity(int capacity);

    /**
     * Same as {@link #withAll(FloatByteMap)}.
     */
    MutableFloatByteMap ofAll(FloatByteMap map);

    MutableFloatByteMap withAll(FloatByteMap map);

    /**
     * Creates an {@code MutableFloatByteMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T> MutableFloatByteMap from(Iterable<T> iterable, FloatFunction<? super T> keyFunction, ByteFunction<? super T> valueFunction);
}
