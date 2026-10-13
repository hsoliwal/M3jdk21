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
import org.eclipse.collections.api.block.function.primitive.IntFunction;
import org.eclipse.collections.api.map.primitive.MutableCharIntMap;
import org.eclipse.collections.api.map.primitive.CharIntMap;

/**
 * A factory which creates instances of type {@link MutableCharIntMap}.
 * This file was automatically generated from template file mutablePrimitivePrimitiveMapFactory.stg.
 *
 * @since 6.0.
 */
public interface MutableCharIntMapFactory
{
    MutableCharIntMap empty();

    /**
     * Same as {@link #empty()}.
     */
    MutableCharIntMap of();

    /**
     * Same as {@link #empty()}.
     */
    MutableCharIntMap with();

    /**
     * @since 11.1.
     */
    default MutableCharIntMap of(char key, int value)
    {
        return this.with(key, value);
    }

    /**
     * @since 11.1.
     */
    default MutableCharIntMap with(char key, int value)
    {
        return this.with().withKeyValue(key, value);
    }

    /**
     * @since 11.1.
     */
    default MutableCharIntMap of(char key1, int value1, char key2, int value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    /**
     * @since 11.1.
     */
    default MutableCharIntMap with(char key1, int value1, char key2, int value2)
    {
        return this.with(key1, value1).withKeyValue(key1, value2);
    }

    /**
     * @since 11.1.
     */
    default MutableCharIntMap of(char key1, int value1, char key2, int value2, char key3, int value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    /**
     * @since 11.1.
     */
    default MutableCharIntMap with(char key1, int value1, char key2, int value2, char key3, int value3)
    {
        return this.with(key1, value1, key2, value2).withKeyValue(key3, value3);
    }

    /**
     * @since 11.1.
     */
    default MutableCharIntMap of(char key1, int value1, char key2, int value2, char key3, int value3, char key4, int value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    /**
     * @since 11.1.
     */
    default MutableCharIntMap with(char key1, int value1, char key2, int value2, char key3, int value3, char key4, int value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3).withKeyValue(key4, value4);
    }

    /**
     * Same as {@link #empty()}. but takes in an initial capacity
     */
    MutableCharIntMap ofInitialCapacity(int capacity);

    /**
     * Same as {@link #empty()}. but takes in an initial capacity
     */
    MutableCharIntMap withInitialCapacity(int capacity);

    /**
     * Same as {@link #withAll(CharIntMap)}.
     */
    MutableCharIntMap ofAll(CharIntMap map);

    MutableCharIntMap withAll(CharIntMap map);

    /**
     * Creates an {@code MutableCharIntMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T> MutableCharIntMap from(Iterable<T> iterable, CharFunction<? super T> keyFunction, IntFunction<? super T> valueFunction);
}
