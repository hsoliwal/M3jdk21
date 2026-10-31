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
import org.eclipse.collections.api.block.function.primitive.BooleanFunction;
import org.eclipse.collections.api.map.primitive.MutableLongBooleanMap;
import org.eclipse.collections.api.map.primitive.LongBooleanMap;

/**
 * A factory which creates instances of type {@link MutableLongBooleanMap}.
 * This file was automatically generated from template file mutablePrimitivePrimitiveMapFactory.stg.
 *
 * @since 6.0.
 */
public interface MutableLongBooleanMapFactory
{
    MutableLongBooleanMap empty();

    /**
     * Same as {@link #empty()}.
     */
    MutableLongBooleanMap of();

    /**
     * Same as {@link #empty()}.
     */
    MutableLongBooleanMap with();

    /**
     * @since 11.1.
     */
    default MutableLongBooleanMap of(long key, boolean value)
    {
        return this.with(key, value);
    }

    /**
     * @since 11.1.
     */
    default MutableLongBooleanMap with(long key, boolean value)
    {
        return this.with().withKeyValue(key, value);
    }

    /**
     * @since 11.1.
     */
    default MutableLongBooleanMap of(long key1, boolean value1, long key2, boolean value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    /**
     * @since 11.1.
     */
    default MutableLongBooleanMap with(long key1, boolean value1, long key2, boolean value2)
    {
        return this.with(key1, value1).withKeyValue(key1, value2);
    }

    /**
     * @since 11.1.
     */
    default MutableLongBooleanMap of(long key1, boolean value1, long key2, boolean value2, long key3, boolean value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    /**
     * @since 11.1.
     */
    default MutableLongBooleanMap with(long key1, boolean value1, long key2, boolean value2, long key3, boolean value3)
    {
        return this.with(key1, value1, key2, value2).withKeyValue(key3, value3);
    }

    /**
     * @since 11.1.
     */
    default MutableLongBooleanMap of(long key1, boolean value1, long key2, boolean value2, long key3, boolean value3, long key4, boolean value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    /**
     * @since 11.1.
     */
    default MutableLongBooleanMap with(long key1, boolean value1, long key2, boolean value2, long key3, boolean value3, long key4, boolean value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3).withKeyValue(key4, value4);
    }

    /**
     * Same as {@link #empty()}. but takes in an initial capacity
     */
    MutableLongBooleanMap ofInitialCapacity(int capacity);

    /**
     * Same as {@link #empty()}. but takes in an initial capacity
     */
    MutableLongBooleanMap withInitialCapacity(int capacity);

    /**
     * Same as {@link #withAll(LongBooleanMap)}.
     */
    MutableLongBooleanMap ofAll(LongBooleanMap map);

    MutableLongBooleanMap withAll(LongBooleanMap map);

    /**
     * Creates an {@code MutableLongBooleanMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T> MutableLongBooleanMap from(Iterable<T> iterable, LongFunction<? super T> keyFunction, BooleanFunction<? super T> valueFunction);
}
