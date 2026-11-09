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
import org.eclipse.collections.api.map.primitive.MutableObjectLongMap;
import org.eclipse.collections.api.map.primitive.ObjectLongMap;

/**
 * A factory which creates instances of type {@link MutableObjectLongMap}.
 * This file was automatically generated from template file mutableObjectPrimitiveMapFactory.stg.
 *
 * @since 6.0.
 */
public interface MutableObjectLongMapFactory
{
    <K> MutableObjectLongMap<K> empty();

    /**
     * Same as {@link #empty()}.
     */
    <K> MutableObjectLongMap<K> of();

    /**
     * Same as {@link #empty()}.
     */
    <K> MutableObjectLongMap<K> with();

    /**
     * @since 11.1.
     */
    default <K> MutableObjectLongMap<K> with(K key, long value)
    {
        MutableObjectLongMap<K> map = this.with();
        map.put(key, value);
        return map;
    }

    /**
     * @since 11.1.
     */
    default <K> MutableObjectLongMap<K> of(K key, long value)
    {
        return this.with(key, value);
    }

    /**
     * @since 11.1.
     */
    default <K> MutableObjectLongMap<K> with(K key1, long value1, K key2, long value2)
    {
        MutableObjectLongMap<K> map = this.with(key1, value1);
        map.put(key2, value2);
        return map;
    }

    /**
     * @since 11.1.
     */
    default <K> MutableObjectLongMap<K> of(K key1, long value1, K key2, long value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    /**
     * @since 11.1.
     */
    default <K> MutableObjectLongMap<K> with(K key1, long value1, K key2, long value2, K key3, long value3)
    {
        MutableObjectLongMap<K> map = this.with(key1, value1, key2, value2);
        map.put(key3, value3);
        return map;
    }

    /**
     * @since 11.1.
     */
    default <K> MutableObjectLongMap<K> of(K key1, long value1, K key2, long value2, K key3, long value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    /**
     * @since 11.1.
     */
    default <K> MutableObjectLongMap<K> with(K key1, long value1, K key2, long value2, K key3, long value3, K key4, long value4)
    {
        MutableObjectLongMap<K> map = this.with(key1, value1, key2, value2, key3, value3);
        map.put(key4, value4);
        return map;
    }

    /**
     * @since 11.1.
     */
    default <K> MutableObjectLongMap<K> of(K key1, long value1, K key2, long value2, K key3, long value3, K key4, long value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    /**
     * Same as {@link #empty()}. but takes in an initial capacity
     */
    <K> MutableObjectLongMap<K> ofInitialCapacity(int capacity);

    /**
     * Same as {@link #empty()}. but takes in an initial capacity
     */
    <K> MutableObjectLongMap<K> withInitialCapacity(int capacity);

    /**
     * Same as {@link #withAll(ObjectLongMap)}.
     */
    <K> MutableObjectLongMap<K> ofAll(ObjectLongMap<? extends K> map);

    <K> MutableObjectLongMap<K> withAll(ObjectLongMap<? extends K> map);

    /**
     * Creates an {@code MutableObjectLongMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T, K> MutableObjectLongMap<K> from(Iterable<T> iterable, Function<? super T, ? extends K> keyFunction, LongFunction<? super T> valueFunction);
}
