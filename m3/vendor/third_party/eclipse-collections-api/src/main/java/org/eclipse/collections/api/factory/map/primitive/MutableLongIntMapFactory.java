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
import org.eclipse.collections.api.block.function.primitive.IntFunction;
import org.eclipse.collections.api.map.primitive.MutableLongIntMap;
import org.eclipse.collections.api.map.primitive.LongIntMap;

/**
 * A factory which creates instances of type {@link MutableLongIntMap}.
 * This file was automatically generated from template file mutablePrimitivePrimitiveMapFactory.stg.
 *
 * @since 6.0.
 */
public interface MutableLongIntMapFactory
{
    MutableLongIntMap empty();

    /**
     * Same as {@link #empty()}.
     */
    MutableLongIntMap of();

    /**
     * Same as {@link #empty()}.
     */
    MutableLongIntMap with();

    /**
     * @since 11.1.
     */
    default MutableLongIntMap of(long key, int value)
    {
        return this.with(key, value);
    }

    /**
     * @since 11.1.
     */
    default MutableLongIntMap with(long key, int value)
    {
        return this.with().withKeyValue(key, value);
    }

    /**
     * @since 11.1.
     */
    default MutableLongIntMap of(long key1, int value1, long key2, int value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    /**
     * @since 11.1.
     */
    default MutableLongIntMap with(long key1, int value1, long key2, int value2)
    {
        return this.with(key1, value1).withKeyValue(key1, value2);
    }

    /**
     * @since 11.1.
     */
    default MutableLongIntMap of(long key1, int value1, long key2, int value2, long key3, int value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    /**
     * @since 11.1.
     */
    default MutableLongIntMap with(long key1, int value1, long key2, int value2, long key3, int value3)
    {
        return this.with(key1, value1, key2, value2).withKeyValue(key3, value3);
    }

    /**
     * @since 11.1.
     */
    default MutableLongIntMap of(long key1, int value1, long key2, int value2, long key3, int value3, long key4, int value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    /**
     * @since 11.1.
     */
    default MutableLongIntMap with(long key1, int value1, long key2, int value2, long key3, int value3, long key4, int value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3).withKeyValue(key4, value4);
    }

    /**
     * Same as {@link #empty()}. but takes in an initial capacity
     */
    MutableLongIntMap ofInitialCapacity(int capacity);

    /**
     * Same as {@link #empty()}. but takes in an initial capacity
     */
    MutableLongIntMap withInitialCapacity(int capacity);

    /**
     * Same as {@link #withAll(LongIntMap)}.
     */
    MutableLongIntMap ofAll(LongIntMap map);

    MutableLongIntMap withAll(LongIntMap map);

    /**
     * Creates an {@code MutableLongIntMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T> MutableLongIntMap from(Iterable<T> iterable, LongFunction<? super T> keyFunction, IntFunction<? super T> valueFunction);
}
