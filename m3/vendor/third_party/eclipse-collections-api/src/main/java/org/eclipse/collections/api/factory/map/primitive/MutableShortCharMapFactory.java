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
import org.eclipse.collections.api.block.function.primitive.CharFunction;
import org.eclipse.collections.api.map.primitive.MutableShortCharMap;
import org.eclipse.collections.api.map.primitive.ShortCharMap;

/**
 * A factory which creates instances of type {@link MutableShortCharMap}.
 * This file was automatically generated from template file mutablePrimitivePrimitiveMapFactory.stg.
 *
 * @since 6.0.
 */
public interface MutableShortCharMapFactory
{
    MutableShortCharMap empty();

    /**
     * Same as {@link #empty()}.
     */
    MutableShortCharMap of();

    /**
     * Same as {@link #empty()}.
     */
    MutableShortCharMap with();

    /**
     * @since 11.1.
     */
    default MutableShortCharMap of(short key, char value)
    {
        return this.with(key, value);
    }

    /**
     * @since 11.1.
     */
    default MutableShortCharMap with(short key, char value)
    {
        return this.with().withKeyValue(key, value);
    }

    /**
     * @since 11.1.
     */
    default MutableShortCharMap of(short key1, char value1, short key2, char value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    /**
     * @since 11.1.
     */
    default MutableShortCharMap with(short key1, char value1, short key2, char value2)
    {
        return this.with(key1, value1).withKeyValue(key1, value2);
    }

    /**
     * @since 11.1.
     */
    default MutableShortCharMap of(short key1, char value1, short key2, char value2, short key3, char value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    /**
     * @since 11.1.
     */
    default MutableShortCharMap with(short key1, char value1, short key2, char value2, short key3, char value3)
    {
        return this.with(key1, value1, key2, value2).withKeyValue(key3, value3);
    }

    /**
     * @since 11.1.
     */
    default MutableShortCharMap of(short key1, char value1, short key2, char value2, short key3, char value3, short key4, char value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    /**
     * @since 11.1.
     */
    default MutableShortCharMap with(short key1, char value1, short key2, char value2, short key3, char value3, short key4, char value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3).withKeyValue(key4, value4);
    }

    /**
     * Same as {@link #empty()}. but takes in an initial capacity
     */
    MutableShortCharMap ofInitialCapacity(int capacity);

    /**
     * Same as {@link #empty()}. but takes in an initial capacity
     */
    MutableShortCharMap withInitialCapacity(int capacity);

    /**
     * Same as {@link #withAll(ShortCharMap)}.
     */
    MutableShortCharMap ofAll(ShortCharMap map);

    MutableShortCharMap withAll(ShortCharMap map);

    /**
     * Creates an {@code MutableShortCharMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T> MutableShortCharMap from(Iterable<T> iterable, ShortFunction<? super T> keyFunction, CharFunction<? super T> valueFunction);
}
