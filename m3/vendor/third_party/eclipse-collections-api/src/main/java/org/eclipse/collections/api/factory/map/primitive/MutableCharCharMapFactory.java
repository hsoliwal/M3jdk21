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
import org.eclipse.collections.api.map.primitive.MutableCharCharMap;
import org.eclipse.collections.api.map.primitive.CharCharMap;

/**
 * A factory which creates instances of type {@link MutableCharCharMap}.
 * This file was automatically generated from template file mutablePrimitivePrimitiveMapFactory.stg.
 *
 * @since 6.0.
 */
public interface MutableCharCharMapFactory
{
    MutableCharCharMap empty();

    /**
     * Same as {@link #empty()}.
     */
    MutableCharCharMap of();

    /**
     * Same as {@link #empty()}.
     */
    MutableCharCharMap with();

    /**
     * @since 11.1.
     */
    default MutableCharCharMap of(char key, char value)
    {
        return this.with(key, value);
    }

    /**
     * @since 11.1.
     */
    default MutableCharCharMap with(char key, char value)
    {
        return this.with().withKeyValue(key, value);
    }

    /**
     * @since 11.1.
     */
    default MutableCharCharMap of(char key1, char value1, char key2, char value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    /**
     * @since 11.1.
     */
    default MutableCharCharMap with(char key1, char value1, char key2, char value2)
    {
        return this.with(key1, value1).withKeyValue(key1, value2);
    }

    /**
     * @since 11.1.
     */
    default MutableCharCharMap of(char key1, char value1, char key2, char value2, char key3, char value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    /**
     * @since 11.1.
     */
    default MutableCharCharMap with(char key1, char value1, char key2, char value2, char key3, char value3)
    {
        return this.with(key1, value1, key2, value2).withKeyValue(key3, value3);
    }

    /**
     * @since 11.1.
     */
    default MutableCharCharMap of(char key1, char value1, char key2, char value2, char key3, char value3, char key4, char value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    /**
     * @since 11.1.
     */
    default MutableCharCharMap with(char key1, char value1, char key2, char value2, char key3, char value3, char key4, char value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3).withKeyValue(key4, value4);
    }

    /**
     * Same as {@link #empty()}. but takes in an initial capacity
     */
    MutableCharCharMap ofInitialCapacity(int capacity);

    /**
     * Same as {@link #empty()}. but takes in an initial capacity
     */
    MutableCharCharMap withInitialCapacity(int capacity);

    /**
     * Same as {@link #withAll(CharCharMap)}.
     */
    MutableCharCharMap ofAll(CharCharMap map);

    MutableCharCharMap withAll(CharCharMap map);

    /**
     * Creates an {@code MutableCharCharMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T> MutableCharCharMap from(Iterable<T> iterable, CharFunction<? super T> keyFunction, CharFunction<? super T> valueFunction);
}
