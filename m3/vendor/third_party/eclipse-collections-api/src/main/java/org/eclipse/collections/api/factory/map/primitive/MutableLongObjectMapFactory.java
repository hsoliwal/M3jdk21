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
import org.eclipse.collections.api.block.function.primitive.LongFunction;
import org.eclipse.collections.api.map.primitive.MutableLongObjectMap;
import org.eclipse.collections.api.map.primitive.LongObjectMap;

/**
 * A factory which creates instances of type {@link MutableLongObjectMap}.
 * This file was automatically generated from template file mutablePrimitiveObjectMapFactory.stg.
 *
 * @since 6.0.
 */
public interface MutableLongObjectMapFactory
{
    <V> MutableLongObjectMap<V> empty();

    /**
     * Same as {@link #empty()}.
     */
    <V> MutableLongObjectMap<V> of();

    /**
     * Same as {@link #empty()}.
     */
    <V> MutableLongObjectMap<V> with();

    /**
     * @since 11.1.
     */
    default <V> MutableLongObjectMap<V> of(long key, V value)
    {
        return this.with(key, value);
    }

    /**
     * @since 11.1.
     */
    default <V> MutableLongObjectMap<V> with(long key, V value)
    {
        return this.<V>with().withKeyValue(key, value);
    }

    /**
     * @since 11.1.
     */
    default <V> MutableLongObjectMap<V> of(long key1, V value1, long key2, V value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    /**
     * @since 11.1.
     */
    default <V> MutableLongObjectMap<V> with(long key1, V value1, long key2, V value2)
    {
        return this.with(key1, value1).withKeyValue(key2, value2);
    }

    /**
     * @since 11.1.
     */
    default <V> MutableLongObjectMap<V> of(long key1, V value1, long key2, V value2, long key3, V value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    /**
     * @since 11.1.
     */
    default <V> MutableLongObjectMap<V> with(long key1, V value1, long key2, V value2, long key3, V value3)
    {
        return this.with(key1, value1, key2, value2).withKeyValue(key3, value3);
    }

    /**
     * @since 11.1.
     */
    default <V> MutableLongObjectMap<V> of(long key1, V value1, long key2, V value2, long key3, V value3, long key4, V value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    /**
     * @since 11.1.
     */
    default <V> MutableLongObjectMap<V> with(long key1, V value1, long key2, V value2, long key3, V value3, long key4, V value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3).withKeyValue(key4, value4);
    }

    /**
     * Same as {@link #empty()}. but takes in an initial capacity
     */
    <V> MutableLongObjectMap<V> ofInitialCapacity(int capacity);

    /**
     * Same as {@link #empty()}. but takes in an initial capacity
     */
    <V> MutableLongObjectMap<V> withInitialCapacity(int capacity);

    /**
     * Same as {@link #withAll(LongObjectMap)}.
     */
    <V> MutableLongObjectMap<V> ofAll(LongObjectMap<? extends V> map);

    <V> MutableLongObjectMap<V> withAll(LongObjectMap<? extends V> map);

    /**
     * Creates an {@code MutableLongObjectMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T, V> MutableLongObjectMap<V> from(Iterable<T> iterable, LongFunction<? super T> keyFunction, Function<? super T, ? extends V> valueFunction);
}
