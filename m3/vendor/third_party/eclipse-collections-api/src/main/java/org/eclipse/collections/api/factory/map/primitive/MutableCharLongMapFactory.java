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

import org.eclipse.collections.api.block.function.primitive.CharFunction;
import org.eclipse.collections.api.block.function.primitive.LongFunction;
import org.eclipse.collections.api.map.primitive.MutableCharLongMap;
import org.eclipse.collections.api.map.primitive.CharLongMap;

/**
 * A factory which creates instances of type {@link MutableCharLongMap}.
 * This file was automatically generated from template file mutablePrimitivePrimitiveMapFactory.stg.
 *
 * @since 6.0.
 */
public interface MutableCharLongMapFactory
{
    MutableCharLongMap empty();

    /**
     * Same as {@link #empty()}.
     */
    MutableCharLongMap of();

    /**
     * Same as {@link #empty()}.
     */
    MutableCharLongMap with();

    /**
     * @since 11.1.
     */
    default MutableCharLongMap of(char key, long value)
    {
        return this.with(key, value);
    }

    /**
     * @since 11.1.
     */
    default MutableCharLongMap with(char key, long value)
    {
        return this.with().withKeyValue(key, value);
    }

    /**
     * @since 11.1.
     */
    default MutableCharLongMap of(char key1, long value1, char key2, long value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    /**
     * @since 11.1.
     */
    default MutableCharLongMap with(char key1, long value1, char key2, long value2)
    {
        return this.with(key1, value1).withKeyValue(key1, value2);
    }

    /**
     * @since 11.1.
     */
    default MutableCharLongMap of(char key1, long value1, char key2, long value2, char key3, long value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    /**
     * @since 11.1.
     */
    default MutableCharLongMap with(char key1, long value1, char key2, long value2, char key3, long value3)
    {
        return this.with(key1, value1, key2, value2).withKeyValue(key3, value3);
    }

    /**
     * @since 11.1.
     */
    default MutableCharLongMap of(char key1, long value1, char key2, long value2, char key3, long value3, char key4, long value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    /**
     * @since 11.1.
     */
    default MutableCharLongMap with(char key1, long value1, char key2, long value2, char key3, long value3, char key4, long value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3).withKeyValue(key4, value4);
    }

    /**
     * Same as {@link #empty()}. but takes in an initial capacity
     */
    MutableCharLongMap ofInitialCapacity(int capacity);

    /**
     * Same as {@link #empty()}. but takes in an initial capacity
     */
    MutableCharLongMap withInitialCapacity(int capacity);

    /**
     * Same as {@link #withAll(CharLongMap)}.
     */
    MutableCharLongMap ofAll(CharLongMap map);

    MutableCharLongMap withAll(CharLongMap map);

    /**
     * Creates an {@code MutableCharLongMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T> MutableCharLongMap from(Iterable<T> iterable, CharFunction<? super T> keyFunction, LongFunction<? super T> valueFunction);
}
