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
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.map.primitive.MutableObjectFloatMap;
import org.eclipse.collections.api.map.primitive.ObjectFloatMap;

/**
 * A factory which creates instances of type {@link MutableObjectFloatMap}.
 * This file was automatically generated from template file mutableObjectPrimitiveMapFactory.stg.
 *
 * @since 6.0.
 */
public interface MutableObjectFloatMapFactory
{
    <K> MutableObjectFloatMap<K> empty();

    /**
     * Same as {@link #empty()}.
     */
    <K> MutableObjectFloatMap<K> of();

    /**
     * Same as {@link #empty()}.
     */
    <K> MutableObjectFloatMap<K> with();

    /**
     * @since 11.1.
     */
    default <K> MutableObjectFloatMap<K> with(K key, float value)
    {
        MutableObjectFloatMap<K> map = this.with();
        map.put(key, value);
        return map;
    }

    /**
     * @since 11.1.
     */
    default <K> MutableObjectFloatMap<K> of(K key, float value)
    {
        return this.with(key, value);
    }

    /**
     * @since 11.1.
     */
    default <K> MutableObjectFloatMap<K> with(K key1, float value1, K key2, float value2)
    {
        MutableObjectFloatMap<K> map = this.with(key1, value1);
        map.put(key2, value2);
        return map;
    }

    /**
     * @since 11.1.
     */
    default <K> MutableObjectFloatMap<K> of(K key1, float value1, K key2, float value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    /**
     * @since 11.1.
     */
    default <K> MutableObjectFloatMap<K> with(K key1, float value1, K key2, float value2, K key3, float value3)
    {
        MutableObjectFloatMap<K> map = this.with(key1, value1, key2, value2);
        map.put(key3, value3);
        return map;
    }

    /**
     * @since 11.1.
     */
    default <K> MutableObjectFloatMap<K> of(K key1, float value1, K key2, float value2, K key3, float value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    /**
     * @since 11.1.
     */
    default <K> MutableObjectFloatMap<K> with(K key1, float value1, K key2, float value2, K key3, float value3, K key4, float value4)
    {
        MutableObjectFloatMap<K> map = this.with(key1, value1, key2, value2, key3, value3);
        map.put(key4, value4);
        return map;
    }

    /**
     * @since 11.1.
     */
    default <K> MutableObjectFloatMap<K> of(K key1, float value1, K key2, float value2, K key3, float value3, K key4, float value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    /**
     * Same as {@link #empty()}. but takes in an initial capacity
     */
    <K> MutableObjectFloatMap<K> ofInitialCapacity(int capacity);

    /**
     * Same as {@link #empty()}. but takes in an initial capacity
     */
    <K> MutableObjectFloatMap<K> withInitialCapacity(int capacity);

    /**
     * Same as {@link #withAll(ObjectFloatMap)}.
     */
    <K> MutableObjectFloatMap<K> ofAll(ObjectFloatMap<? extends K> map);

    <K> MutableObjectFloatMap<K> withAll(ObjectFloatMap<? extends K> map);

    /**
     * Creates an {@code MutableObjectFloatMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T, K> MutableObjectFloatMap<K> from(Iterable<T> iterable, Function<? super T, ? extends K> keyFunction, FloatFunction<? super T> valueFunction);
}
