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

import org.eclipse.collections.api.block.function.primitive.LongFunction;
import org.eclipse.collections.api.block.function.primitive.ShortFunction;
import org.eclipse.collections.api.map.primitive.MutableLongShortMap;
import org.eclipse.collections.api.map.primitive.LongShortMap;

/**
 * A factory which creates instances of type {@link MutableLongShortMap}.
 * This file was automatically generated from template file mutablePrimitivePrimitiveMapFactory.stg.
 *
 * @since 6.0.
 */
public interface MutableLongShortMapFactory
{
    MutableLongShortMap empty();

    /**
     * Same as {@link #empty()}.
     */
    MutableLongShortMap of();

    /**
     * Same as {@link #empty()}.
     */
    MutableLongShortMap with();

    /**
     * @since 11.1.
     */
    default MutableLongShortMap of(long key, short value)
    {
        return this.with(key, value);
    }

    /**
     * @since 11.1.
     */
    default MutableLongShortMap with(long key, short value)
    {
        return this.with().withKeyValue(key, value);
    }

    /**
     * @since 11.1.
     */
    default MutableLongShortMap of(long key1, short value1, long key2, short value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    /**
     * @since 11.1.
     */
    default MutableLongShortMap with(long key1, short value1, long key2, short value2)
    {
        return this.with(key1, value1).withKeyValue(key1, value2);
    }

    /**
     * @since 11.1.
     */
    default MutableLongShortMap of(long key1, short value1, long key2, short value2, long key3, short value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    /**
     * @since 11.1.
     */
    default MutableLongShortMap with(long key1, short value1, long key2, short value2, long key3, short value3)
    {
        return this.with(key1, value1, key2, value2).withKeyValue(key3, value3);
    }

    /**
     * @since 11.1.
     */
    default MutableLongShortMap of(long key1, short value1, long key2, short value2, long key3, short value3, long key4, short value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    /**
     * @since 11.1.
     */
    default MutableLongShortMap with(long key1, short value1, long key2, short value2, long key3, short value3, long key4, short value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3).withKeyValue(key4, value4);
    }

    /**
     * Same as {@link #empty()}. but takes in an initial capacity
     */
    MutableLongShortMap ofInitialCapacity(int capacity);

    /**
     * Same as {@link #empty()}. but takes in an initial capacity
     */
    MutableLongShortMap withInitialCapacity(int capacity);

    /**
     * Same as {@link #withAll(LongShortMap)}.
     */
    MutableLongShortMap ofAll(LongShortMap map);

    MutableLongShortMap withAll(LongShortMap map);

    /**
     * Creates an {@code MutableLongShortMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T> MutableLongShortMap from(Iterable<T> iterable, LongFunction<? super T> keyFunction, ShortFunction<? super T> valueFunction);
}
