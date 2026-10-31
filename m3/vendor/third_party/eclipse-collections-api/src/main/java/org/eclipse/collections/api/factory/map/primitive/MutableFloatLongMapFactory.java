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
import org.eclipse.collections.api.block.function.primitive.LongFunction;
import org.eclipse.collections.api.map.primitive.MutableFloatLongMap;
import org.eclipse.collections.api.map.primitive.FloatLongMap;

/**
 * A factory which creates instances of type {@link MutableFloatLongMap}.
 * This file was automatically generated from template file mutablePrimitivePrimitiveMapFactory.stg.
 *
 * @since 6.0.
 */
public interface MutableFloatLongMapFactory
{
    MutableFloatLongMap empty();

    /**
     * Same as {@link #empty()}.
     */
    MutableFloatLongMap of();

    /**
     * Same as {@link #empty()}.
     */
    MutableFloatLongMap with();

    /**
     * @since 11.1.
     */
    default MutableFloatLongMap of(float key, long value)
    {
        return this.with(key, value);
    }

    /**
     * @since 11.1.
     */
    default MutableFloatLongMap with(float key, long value)
    {
        return this.with().withKeyValue(key, value);
    }

    /**
     * @since 11.1.
     */
    default MutableFloatLongMap of(float key1, long value1, float key2, long value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    /**
     * @since 11.1.
     */
    default MutableFloatLongMap with(float key1, long value1, float key2, long value2)
    {
        return this.with(key1, value1).withKeyValue(key1, value2);
    }

    /**
     * @since 11.1.
     */
    default MutableFloatLongMap of(float key1, long value1, float key2, long value2, float key3, long value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    /**
     * @since 11.1.
     */
    default MutableFloatLongMap with(float key1, long value1, float key2, long value2, float key3, long value3)
    {
        return this.with(key1, value1, key2, value2).withKeyValue(key3, value3);
    }

    /**
     * @since 11.1.
     */
    default MutableFloatLongMap of(float key1, long value1, float key2, long value2, float key3, long value3, float key4, long value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    /**
     * @since 11.1.
     */
    default MutableFloatLongMap with(float key1, long value1, float key2, long value2, float key3, long value3, float key4, long value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3).withKeyValue(key4, value4);
    }

    /**
     * Same as {@link #empty()}. but takes in an initial capacity
     */
    MutableFloatLongMap ofInitialCapacity(int capacity);

    /**
     * Same as {@link #empty()}. but takes in an initial capacity
     */
    MutableFloatLongMap withInitialCapacity(int capacity);

    /**
     * Same as {@link #withAll(FloatLongMap)}.
     */
    MutableFloatLongMap ofAll(FloatLongMap map);

    MutableFloatLongMap withAll(FloatLongMap map);

    /**
     * Creates an {@code MutableFloatLongMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T> MutableFloatLongMap from(Iterable<T> iterable, FloatFunction<? super T> keyFunction, LongFunction<? super T> valueFunction);
}
