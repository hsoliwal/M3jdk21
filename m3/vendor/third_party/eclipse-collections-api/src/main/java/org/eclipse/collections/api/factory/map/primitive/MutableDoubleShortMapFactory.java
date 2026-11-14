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
import org.eclipse.collections.api.block.function.primitive.ShortFunction;
import org.eclipse.collections.api.map.primitive.MutableDoubleShortMap;
import org.eclipse.collections.api.map.primitive.DoubleShortMap;

/**
 * A factory which creates instances of type {@link MutableDoubleShortMap}.
 * This file was automatically generated from template file mutablePrimitivePrimitiveMapFactory.stg.
 *
 * @since 6.0.
 */
public interface MutableDoubleShortMapFactory
{
    MutableDoubleShortMap empty();

    /**
     * Same as {@link #empty()}.
     */
    MutableDoubleShortMap of();

    /**
     * Same as {@link #empty()}.
     */
    MutableDoubleShortMap with();

    /**
     * @since 11.1.
     */
    default MutableDoubleShortMap of(double key, short value)
    {
        return this.with(key, value);
    }

    /**
     * @since 11.1.
     */
    default MutableDoubleShortMap with(double key, short value)
    {
        return this.with().withKeyValue(key, value);
    }

    /**
     * @since 11.1.
     */
    default MutableDoubleShortMap of(double key1, short value1, double key2, short value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    /**
     * @since 11.1.
     */
    default MutableDoubleShortMap with(double key1, short value1, double key2, short value2)
    {
        return this.with(key1, value1).withKeyValue(key1, value2);
    }

    /**
     * @since 11.1.
     */
    default MutableDoubleShortMap of(double key1, short value1, double key2, short value2, double key3, short value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    /**
     * @since 11.1.
     */
    default MutableDoubleShortMap with(double key1, short value1, double key2, short value2, double key3, short value3)
    {
        return this.with(key1, value1, key2, value2).withKeyValue(key3, value3);
    }

    /**
     * @since 11.1.
     */
    default MutableDoubleShortMap of(double key1, short value1, double key2, short value2, double key3, short value3, double key4, short value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    /**
     * @since 11.1.
     */
    default MutableDoubleShortMap with(double key1, short value1, double key2, short value2, double key3, short value3, double key4, short value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3).withKeyValue(key4, value4);
    }

    /**
     * Same as {@link #empty()}. but takes in an initial capacity
     */
    MutableDoubleShortMap ofInitialCapacity(int capacity);

    /**
     * Same as {@link #empty()}. but takes in an initial capacity
     */
    MutableDoubleShortMap withInitialCapacity(int capacity);

    /**
     * Same as {@link #withAll(DoubleShortMap)}.
     */
    MutableDoubleShortMap ofAll(DoubleShortMap map);

    MutableDoubleShortMap withAll(DoubleShortMap map);

    /**
     * Creates an {@code MutableDoubleShortMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T> MutableDoubleShortMap from(Iterable<T> iterable, DoubleFunction<? super T> keyFunction, ShortFunction<? super T> valueFunction);
}
