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
import org.eclipse.collections.api.block.function.primitive.DoubleFunction;
import org.eclipse.collections.api.map.primitive.MutableLongDoubleMap;
import org.eclipse.collections.api.map.primitive.LongDoubleMap;

/**
 * A factory which creates instances of type {@link MutableLongDoubleMap}.
 * This file was automatically generated from template file mutablePrimitivePrimitiveMapFactory.stg.
 *
 * @since 6.0.
 */
public interface MutableLongDoubleMapFactory
{
    MutableLongDoubleMap empty();

    /**
     * Same as {@link #empty()}.
     */
    MutableLongDoubleMap of();

    /**
     * Same as {@link #empty()}.
     */
    MutableLongDoubleMap with();

    /**
     * @since 11.1.
     */
    default MutableLongDoubleMap of(long key, double value)
    {
        return this.with(key, value);
    }

    /**
     * @since 11.1.
     */
    default MutableLongDoubleMap with(long key, double value)
    {
        return this.with().withKeyValue(key, value);
    }

    /**
     * @since 11.1.
     */
    default MutableLongDoubleMap of(long key1, double value1, long key2, double value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    /**
     * @since 11.1.
     */
    default MutableLongDoubleMap with(long key1, double value1, long key2, double value2)
    {
        return this.with(key1, value1).withKeyValue(key1, value2);
    }

    /**
     * @since 11.1.
     */
    default MutableLongDoubleMap of(long key1, double value1, long key2, double value2, long key3, double value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    /**
     * @since 11.1.
     */
    default MutableLongDoubleMap with(long key1, double value1, long key2, double value2, long key3, double value3)
    {
        return this.with(key1, value1, key2, value2).withKeyValue(key3, value3);
    }

    /**
     * @since 11.1.
     */
    default MutableLongDoubleMap of(long key1, double value1, long key2, double value2, long key3, double value3, long key4, double value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    /**
     * @since 11.1.
     */
    default MutableLongDoubleMap with(long key1, double value1, long key2, double value2, long key3, double value3, long key4, double value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3).withKeyValue(key4, value4);
    }

    /**
     * Same as {@link #empty()}. but takes in an initial capacity
     */
    MutableLongDoubleMap ofInitialCapacity(int capacity);

    /**
     * Same as {@link #empty()}. but takes in an initial capacity
     */
    MutableLongDoubleMap withInitialCapacity(int capacity);

    /**
     * Same as {@link #withAll(LongDoubleMap)}.
     */
    MutableLongDoubleMap ofAll(LongDoubleMap map);

    MutableLongDoubleMap withAll(LongDoubleMap map);

    /**
     * Creates an {@code MutableLongDoubleMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T> MutableLongDoubleMap from(Iterable<T> iterable, LongFunction<? super T> keyFunction, DoubleFunction<? super T> valueFunction);
}
