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

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.primitive.IntFunction;
import org.eclipse.collections.api.map.primitive.MutableIntObjectMap;
import org.eclipse.collections.api.map.primitive.IntObjectMap;

/**
 * A factory which creates instances of type {@link MutableIntObjectMap}.
 * This file was automatically generated from template file mutablePrimitiveObjectMapFactory.stg.
 *
 * @since 6.0.
 */
public interface MutableIntObjectMapFactory
{
    <V> MutableIntObjectMap<V> empty();

    /**
     * Same as {@link #empty()}.
     */
    <V> MutableIntObjectMap<V> of();

    /**
     * Same as {@link #empty()}.
     */
    <V> MutableIntObjectMap<V> with();

    /**
     * @since 11.1.
     */
    default <V> MutableIntObjectMap<V> of(int key, V value)
    {
        return this.with(key, value);
    }

    /**
     * @since 11.1.
     */
    default <V> MutableIntObjectMap<V> with(int key, V value)
    {
        return this.<V>with().withKeyValue(key, value);
    }

    /**
     * @since 11.1.
     */
    default <V> MutableIntObjectMap<V> of(int key1, V value1, int key2, V value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    /**
     * @since 11.1.
     */
    default <V> MutableIntObjectMap<V> with(int key1, V value1, int key2, V value2)
    {
        return this.with(key1, value1).withKeyValue(key2, value2);
    }

    /**
     * @since 11.1.
     */
    default <V> MutableIntObjectMap<V> of(int key1, V value1, int key2, V value2, int key3, V value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    /**
     * @since 11.1.
     */
    default <V> MutableIntObjectMap<V> with(int key1, V value1, int key2, V value2, int key3, V value3)
    {
        return this.with(key1, value1, key2, value2).withKeyValue(key3, value3);
    }

    /**
     * @since 11.1.
     */
    default <V> MutableIntObjectMap<V> of(int key1, V value1, int key2, V value2, int key3, V value3, int key4, V value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    /**
     * @since 11.1.
     */
    default <V> MutableIntObjectMap<V> with(int key1, V value1, int key2, V value2, int key3, V value3, int key4, V value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3).withKeyValue(key4, value4);
    }

    /**
     * Same as {@link #empty()}. but takes in an initial capacity
     */
    <V> MutableIntObjectMap<V> ofInitialCapacity(int capacity);

    /**
     * Same as {@link #empty()}. but takes in an initial capacity
     */
    <V> MutableIntObjectMap<V> withInitialCapacity(int capacity);

    /**
     * Same as {@link #withAll(IntObjectMap)}.
     */
    <V> MutableIntObjectMap<V> ofAll(IntObjectMap<? extends V> map);

    <V> MutableIntObjectMap<V> withAll(IntObjectMap<? extends V> map);

    /**
     * Creates an {@code MutableIntObjectMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T, V> MutableIntObjectMap<V> from(Iterable<T> iterable, IntFunction<? super T> keyFunction, Function<? super T, ? extends V> valueFunction);
}
