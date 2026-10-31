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

import org.eclipse.collections.api.block.function.primitive.ShortFunction;
import org.eclipse.collections.api.map.primitive.MutableShortShortMap;
import org.eclipse.collections.api.map.primitive.ShortShortMap;

/**
 * A factory which creates instances of type {@link MutableShortShortMap}.
 * This file was automatically generated from template file mutablePrimitivePrimitiveMapFactory.stg.
 *
 * @since 6.0.
 */
public interface MutableShortShortMapFactory
{
    MutableShortShortMap empty();

    /**
     * Same as {@link #empty()}.
     */
    MutableShortShortMap of();

    /**
     * Same as {@link #empty()}.
     */
    MutableShortShortMap with();

    /**
     * @since 11.1.
     */
    default MutableShortShortMap of(short key, short value)
    {
        return this.with(key, value);
    }

    /**
     * @since 11.1.
     */
    default MutableShortShortMap with(short key, short value)
    {
        return this.with().withKeyValue(key, value);
    }

    /**
     * @since 11.1.
     */
    default MutableShortShortMap of(short key1, short value1, short key2, short value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    /**
     * @since 11.1.
     */
    default MutableShortShortMap with(short key1, short value1, short key2, short value2)
    {
        return this.with(key1, value1).withKeyValue(key1, value2);
    }

    /**
     * @since 11.1.
     */
    default MutableShortShortMap of(short key1, short value1, short key2, short value2, short key3, short value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    /**
     * @since 11.1.
     */
    default MutableShortShortMap with(short key1, short value1, short key2, short value2, short key3, short value3)
    {
        return this.with(key1, value1, key2, value2).withKeyValue(key3, value3);
    }

    /**
     * @since 11.1.
     */
    default MutableShortShortMap of(short key1, short value1, short key2, short value2, short key3, short value3, short key4, short value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    /**
     * @since 11.1.
     */
    default MutableShortShortMap with(short key1, short value1, short key2, short value2, short key3, short value3, short key4, short value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3).withKeyValue(key4, value4);
    }

    /**
     * Same as {@link #empty()}. but takes in an initial capacity
     */
    MutableShortShortMap ofInitialCapacity(int capacity);

    /**
     * Same as {@link #empty()}. but takes in an initial capacity
     */
    MutableShortShortMap withInitialCapacity(int capacity);

    /**
     * Same as {@link #withAll(ShortShortMap)}.
     */
    MutableShortShortMap ofAll(ShortShortMap map);

    MutableShortShortMap withAll(ShortShortMap map);

    /**
     * Creates an {@code MutableShortShortMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T> MutableShortShortMap from(Iterable<T> iterable, ShortFunction<? super T> keyFunction, ShortFunction<? super T> valueFunction);
}
