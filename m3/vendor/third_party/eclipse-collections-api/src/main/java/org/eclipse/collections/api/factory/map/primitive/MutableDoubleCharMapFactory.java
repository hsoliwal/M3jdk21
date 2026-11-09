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
import org.eclipse.collections.api.block.function.primitive.CharFunction;
import org.eclipse.collections.api.map.primitive.MutableDoubleCharMap;
import org.eclipse.collections.api.map.primitive.DoubleCharMap;

/**
 * A factory which creates instances of type {@link MutableDoubleCharMap}.
 * This file was automatically generated from template file mutablePrimitivePrimitiveMapFactory.stg.
 *
 * @since 6.0.
 */
public interface MutableDoubleCharMapFactory
{
    MutableDoubleCharMap empty();

    /**
     * Same as {@link #empty()}.
     */
    MutableDoubleCharMap of();

    /**
     * Same as {@link #empty()}.
     */
    MutableDoubleCharMap with();

    /**
     * @since 11.1.
     */
    default MutableDoubleCharMap of(double key, char value)
    {
        return this.with(key, value);
    }

    /**
     * @since 11.1.
     */
    default MutableDoubleCharMap with(double key, char value)
    {
        return this.with().withKeyValue(key, value);
    }

    /**
     * @since 11.1.
     */
    default MutableDoubleCharMap of(double key1, char value1, double key2, char value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    /**
     * @since 11.1.
     */
    default MutableDoubleCharMap with(double key1, char value1, double key2, char value2)
    {
        return this.with(key1, value1).withKeyValue(key1, value2);
    }

    /**
     * @since 11.1.
     */
    default MutableDoubleCharMap of(double key1, char value1, double key2, char value2, double key3, char value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    /**
     * @since 11.1.
     */
    default MutableDoubleCharMap with(double key1, char value1, double key2, char value2, double key3, char value3)
    {
        return this.with(key1, value1, key2, value2).withKeyValue(key3, value3);
    }

    /**
     * @since 11.1.
     */
    default MutableDoubleCharMap of(double key1, char value1, double key2, char value2, double key3, char value3, double key4, char value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    /**
     * @since 11.1.
     */
    default MutableDoubleCharMap with(double key1, char value1, double key2, char value2, double key3, char value3, double key4, char value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3).withKeyValue(key4, value4);
    }

    /**
     * Same as {@link #empty()}. but takes in an initial capacity
     */
    MutableDoubleCharMap ofInitialCapacity(int capacity);

    /**
     * Same as {@link #empty()}. but takes in an initial capacity
     */
    MutableDoubleCharMap withInitialCapacity(int capacity);

    /**
     * Same as {@link #withAll(DoubleCharMap)}.
     */
    MutableDoubleCharMap ofAll(DoubleCharMap map);

    MutableDoubleCharMap withAll(DoubleCharMap map);

    /**
     * Creates an {@code MutableDoubleCharMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T> MutableDoubleCharMap from(Iterable<T> iterable, DoubleFunction<? super T> keyFunction, CharFunction<? super T> valueFunction);
}
