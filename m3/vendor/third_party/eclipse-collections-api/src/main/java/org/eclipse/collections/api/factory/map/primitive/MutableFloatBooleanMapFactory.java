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
import org.eclipse.collections.api.block.function.primitive.BooleanFunction;
import org.eclipse.collections.api.map.primitive.MutableFloatBooleanMap;
import org.eclipse.collections.api.map.primitive.FloatBooleanMap;

/**
 * A factory which creates instances of type {@link MutableFloatBooleanMap}.
 * This file was automatically generated from template file mutablePrimitivePrimitiveMapFactory.stg.
 *
 * @since 6.0.
 */
public interface MutableFloatBooleanMapFactory
{
    MutableFloatBooleanMap empty();

    /**
     * Same as {@link #empty()}.
     */
    MutableFloatBooleanMap of();

    /**
     * Same as {@link #empty()}.
     */
    MutableFloatBooleanMap with();

    /**
     * @since 11.1.
     */
    default MutableFloatBooleanMap of(float key, boolean value)
    {
        return this.with(key, value);
    }

    /**
     * @since 11.1.
     */
    default MutableFloatBooleanMap with(float key, boolean value)
    {
        return this.with().withKeyValue(key, value);
    }

    /**
     * @since 11.1.
     */
    default MutableFloatBooleanMap of(float key1, boolean value1, float key2, boolean value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    /**
     * @since 11.1.
     */
    default MutableFloatBooleanMap with(float key1, boolean value1, float key2, boolean value2)
    {
        return this.with(key1, value1).withKeyValue(key1, value2);
    }

    /**
     * @since 11.1.
     */
    default MutableFloatBooleanMap of(float key1, boolean value1, float key2, boolean value2, float key3, boolean value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    /**
     * @since 11.1.
     */
    default MutableFloatBooleanMap with(float key1, boolean value1, float key2, boolean value2, float key3, boolean value3)
    {
        return this.with(key1, value1, key2, value2).withKeyValue(key3, value3);
    }

    /**
     * @since 11.1.
     */
    default MutableFloatBooleanMap of(float key1, boolean value1, float key2, boolean value2, float key3, boolean value3, float key4, boolean value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    /**
     * @since 11.1.
     */
    default MutableFloatBooleanMap with(float key1, boolean value1, float key2, boolean value2, float key3, boolean value3, float key4, boolean value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3).withKeyValue(key4, value4);
    }

    /**
     * Same as {@link #empty()}. but takes in an initial capacity
     */
    MutableFloatBooleanMap ofInitialCapacity(int capacity);

    /**
     * Same as {@link #empty()}. but takes in an initial capacity
     */
    MutableFloatBooleanMap withInitialCapacity(int capacity);

    /**
     * Same as {@link #withAll(FloatBooleanMap)}.
     */
    MutableFloatBooleanMap ofAll(FloatBooleanMap map);

    MutableFloatBooleanMap withAll(FloatBooleanMap map);

    /**
     * Creates an {@code MutableFloatBooleanMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T> MutableFloatBooleanMap from(Iterable<T> iterable, FloatFunction<? super T> keyFunction, BooleanFunction<? super T> valueFunction);
}
