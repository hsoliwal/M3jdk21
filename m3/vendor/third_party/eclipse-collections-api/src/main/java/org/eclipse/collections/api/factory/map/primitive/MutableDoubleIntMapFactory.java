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

import org.eclipse.collections.api.block.function.primitive.DoubleFunction;
import org.eclipse.collections.api.block.function.primitive.IntFunction;
import org.eclipse.collections.api.map.primitive.MutableDoubleIntMap;
import org.eclipse.collections.api.map.primitive.DoubleIntMap;

/**
 * A factory which creates instances of type {@link MutableDoubleIntMap}.
 * This file was automatically generated from template file mutablePrimitivePrimitiveMapFactory.stg.
 *
 * @since 6.0.
 */
public interface MutableDoubleIntMapFactory
{
    MutableDoubleIntMap empty();

    /**
     * Same as {@link #empty()}.
     */
    MutableDoubleIntMap of();

    /**
     * Same as {@link #empty()}.
     */
    MutableDoubleIntMap with();

    /**
     * @since 11.1.
     */
    default MutableDoubleIntMap of(double key, int value)
    {
        return this.with(key, value);
    }

    /**
     * @since 11.1.
     */
    default MutableDoubleIntMap with(double key, int value)
    {
        return this.with().withKeyValue(key, value);
    }

    /**
     * @since 11.1.
     */
    default MutableDoubleIntMap of(double key1, int value1, double key2, int value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    /**
     * @since 11.1.
     */
    default MutableDoubleIntMap with(double key1, int value1, double key2, int value2)
    {
        return this.with(key1, value1).withKeyValue(key1, value2);
    }

    /**
     * @since 11.1.
     */
    default MutableDoubleIntMap of(double key1, int value1, double key2, int value2, double key3, int value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    /**
     * @since 11.1.
     */
    default MutableDoubleIntMap with(double key1, int value1, double key2, int value2, double key3, int value3)
    {
        return this.with(key1, value1, key2, value2).withKeyValue(key3, value3);
    }

    /**
     * @since 11.1.
     */
    default MutableDoubleIntMap of(double key1, int value1, double key2, int value2, double key3, int value3, double key4, int value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    /**
     * @since 11.1.
     */
    default MutableDoubleIntMap with(double key1, int value1, double key2, int value2, double key3, int value3, double key4, int value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3).withKeyValue(key4, value4);
    }

    /**
     * Same as {@link #empty()}. but takes in an initial capacity
     */
    MutableDoubleIntMap ofInitialCapacity(int capacity);

    /**
     * Same as {@link #empty()}. but takes in an initial capacity
     */
    MutableDoubleIntMap withInitialCapacity(int capacity);

    /**
     * Same as {@link #withAll(DoubleIntMap)}.
     */
    MutableDoubleIntMap ofAll(DoubleIntMap map);

    MutableDoubleIntMap withAll(DoubleIntMap map);

    /**
     * Creates an {@code MutableDoubleIntMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T> MutableDoubleIntMap from(Iterable<T> iterable, DoubleFunction<? super T> keyFunction, IntFunction<? super T> valueFunction);
}
