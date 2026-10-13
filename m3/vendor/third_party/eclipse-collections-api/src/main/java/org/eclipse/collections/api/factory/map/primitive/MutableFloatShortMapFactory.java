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
import org.eclipse.collections.api.block.function.primitive.ShortFunction;
import org.eclipse.collections.api.map.primitive.MutableFloatShortMap;
import org.eclipse.collections.api.map.primitive.FloatShortMap;

/**
 * A factory which creates instances of type {@link MutableFloatShortMap}.
 * This file was automatically generated from template file mutablePrimitivePrimitiveMapFactory.stg.
 *
 * @since 6.0.
 */
public interface MutableFloatShortMapFactory
{
    MutableFloatShortMap empty();

    /**
     * Same as {@link #empty()}.
     */
    MutableFloatShortMap of();

    /**
     * Same as {@link #empty()}.
     */
    MutableFloatShortMap with();

    /**
     * @since 11.1.
     */
    default MutableFloatShortMap of(float key, short value)
    {
        return this.with(key, value);
    }

    /**
     * @since 11.1.
     */
    default MutableFloatShortMap with(float key, short value)
    {
        return this.with().withKeyValue(key, value);
    }

    /**
     * @since 11.1.
     */
    default MutableFloatShortMap of(float key1, short value1, float key2, short value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    /**
     * @since 11.1.
     */
    default MutableFloatShortMap with(float key1, short value1, float key2, short value2)
    {
        return this.with(key1, value1).withKeyValue(key1, value2);
    }

    /**
     * @since 11.1.
     */
    default MutableFloatShortMap of(float key1, short value1, float key2, short value2, float key3, short value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    /**
     * @since 11.1.
     */
    default MutableFloatShortMap with(float key1, short value1, float key2, short value2, float key3, short value3)
    {
        return this.with(key1, value1, key2, value2).withKeyValue(key3, value3);
    }

    /**
     * @since 11.1.
     */
    default MutableFloatShortMap of(float key1, short value1, float key2, short value2, float key3, short value3, float key4, short value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    /**
     * @since 11.1.
     */
    default MutableFloatShortMap with(float key1, short value1, float key2, short value2, float key3, short value3, float key4, short value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3).withKeyValue(key4, value4);
    }

    /**
     * Same as {@link #empty()}. but takes in an initial capacity
     */
    MutableFloatShortMap ofInitialCapacity(int capacity);

    /**
     * Same as {@link #empty()}. but takes in an initial capacity
     */
    MutableFloatShortMap withInitialCapacity(int capacity);

    /**
     * Same as {@link #withAll(FloatShortMap)}.
     */
    MutableFloatShortMap ofAll(FloatShortMap map);

    MutableFloatShortMap withAll(FloatShortMap map);

    /**
     * Creates an {@code MutableFloatShortMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T> MutableFloatShortMap from(Iterable<T> iterable, FloatFunction<? super T> keyFunction, ShortFunction<? super T> valueFunction);
}
