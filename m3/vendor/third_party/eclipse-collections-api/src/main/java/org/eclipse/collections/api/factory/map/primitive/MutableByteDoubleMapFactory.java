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

import org.eclipse.collections.api.block.function.primitive.ByteFunction;
import org.eclipse.collections.api.block.function.primitive.DoubleFunction;
import org.eclipse.collections.api.map.primitive.MutableByteDoubleMap;
import org.eclipse.collections.api.map.primitive.ByteDoubleMap;

/**
 * A factory which creates instances of type {@link MutableByteDoubleMap}.
 * This file was automatically generated from template file mutablePrimitivePrimitiveMapFactory.stg.
 *
 * @since 6.0.
 */
public interface MutableByteDoubleMapFactory
{
    MutableByteDoubleMap empty();

    /**
     * Same as {@link #empty()}.
     */
    MutableByteDoubleMap of();

    /**
     * Same as {@link #empty()}.
     */
    MutableByteDoubleMap with();

    /**
     * @since 11.1.
     */
    default MutableByteDoubleMap of(byte key, double value)
    {
        return this.with(key, value);
    }

    /**
     * @since 11.1.
     */
    default MutableByteDoubleMap with(byte key, double value)
    {
        return this.with().withKeyValue(key, value);
    }

    /**
     * @since 11.1.
     */
    default MutableByteDoubleMap of(byte key1, double value1, byte key2, double value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    /**
     * @since 11.1.
     */
    default MutableByteDoubleMap with(byte key1, double value1, byte key2, double value2)
    {
        return this.with(key1, value1).withKeyValue(key1, value2);
    }

    /**
     * @since 11.1.
     */
    default MutableByteDoubleMap of(byte key1, double value1, byte key2, double value2, byte key3, double value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    /**
     * @since 11.1.
     */
    default MutableByteDoubleMap with(byte key1, double value1, byte key2, double value2, byte key3, double value3)
    {
        return this.with(key1, value1, key2, value2).withKeyValue(key3, value3);
    }

    /**
     * @since 11.1.
     */
    default MutableByteDoubleMap of(byte key1, double value1, byte key2, double value2, byte key3, double value3, byte key4, double value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    /**
     * @since 11.1.
     */
    default MutableByteDoubleMap with(byte key1, double value1, byte key2, double value2, byte key3, double value3, byte key4, double value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3).withKeyValue(key4, value4);
    }

    /**
     * Same as {@link #empty()}. but takes in an initial capacity
     */
    MutableByteDoubleMap ofInitialCapacity(int capacity);

    /**
     * Same as {@link #empty()}. but takes in an initial capacity
     */
    MutableByteDoubleMap withInitialCapacity(int capacity);

    /**
     * Same as {@link #withAll(ByteDoubleMap)}.
     */
    MutableByteDoubleMap ofAll(ByteDoubleMap map);

    MutableByteDoubleMap withAll(ByteDoubleMap map);

    /**
     * Creates an {@code MutableByteDoubleMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T> MutableByteDoubleMap from(Iterable<T> iterable, ByteFunction<? super T> keyFunction, DoubleFunction<? super T> valueFunction);
}
