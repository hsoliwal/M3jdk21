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

import org.eclipse.collections.api.block.function.primitive.IntFunction;
import org.eclipse.collections.api.block.function.primitive.DoubleFunction;
import org.eclipse.collections.api.map.primitive.MutableIntDoubleMap;
import org.eclipse.collections.api.map.primitive.IntDoubleMap;

/**
 * A factory which creates instances of type {@link MutableIntDoubleMap}.
 * This file was automatically generated from template file mutablePrimitivePrimitiveMapFactory.stg.
 *
 * @since 6.0.
 */
public interface MutableIntDoubleMapFactory
{
    MutableIntDoubleMap empty();

    /**
     * Same as {@link #empty()}.
     */
    MutableIntDoubleMap of();

    /**
     * Same as {@link #empty()}.
     */
    MutableIntDoubleMap with();

    /**
     * @since 11.1.
     */
    default MutableIntDoubleMap of(int key, double value)
    {
        return this.with(key, value);
    }

    /**
     * @since 11.1.
     */
    default MutableIntDoubleMap with(int key, double value)
    {
        return this.with().withKeyValue(key, value);
    }

    /**
     * @since 11.1.
     */
    default MutableIntDoubleMap of(int key1, double value1, int key2, double value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    /**
     * @since 11.1.
     */
    default MutableIntDoubleMap with(int key1, double value1, int key2, double value2)
    {
        return this.with(key1, value1).withKeyValue(key1, value2);
    }

    /**
     * @since 11.1.
     */
    default MutableIntDoubleMap of(int key1, double value1, int key2, double value2, int key3, double value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    /**
     * @since 11.1.
     */
    default MutableIntDoubleMap with(int key1, double value1, int key2, double value2, int key3, double value3)
    {
        return this.with(key1, value1, key2, value2).withKeyValue(key3, value3);
    }

    /**
     * @since 11.1.
     */
    default MutableIntDoubleMap of(int key1, double value1, int key2, double value2, int key3, double value3, int key4, double value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    /**
     * @since 11.1.
     */
    default MutableIntDoubleMap with(int key1, double value1, int key2, double value2, int key3, double value3, int key4, double value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3).withKeyValue(key4, value4);
    }

    /**
     * Same as {@link #empty()}. but takes in an initial capacity
     */
    MutableIntDoubleMap ofInitialCapacity(int capacity);

    /**
     * Same as {@link #empty()}. but takes in an initial capacity
     */
    MutableIntDoubleMap withInitialCapacity(int capacity);

    /**
     * Same as {@link #withAll(IntDoubleMap)}.
     */
    MutableIntDoubleMap ofAll(IntDoubleMap map);

    MutableIntDoubleMap withAll(IntDoubleMap map);

    /**
     * Creates an {@code MutableIntDoubleMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T> MutableIntDoubleMap from(Iterable<T> iterable, IntFunction<? super T> keyFunction, DoubleFunction<? super T> valueFunction);
}
