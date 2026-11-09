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
import org.eclipse.collections.api.block.function.primitive.ByteFunction;
import org.eclipse.collections.api.map.primitive.MutableObjectByteMap;
import org.eclipse.collections.api.map.primitive.ObjectByteMap;

/**
 * A factory which creates instances of type {@link MutableObjectByteMap}.
 * This file was automatically generated from template file mutableObjectPrimitiveMapFactory.stg.
 *
 * @since 6.0.
 */
public interface MutableObjectByteMapFactory
{
    <K> MutableObjectByteMap<K> empty();

    /**
     * Same as {@link #empty()}.
     */
    <K> MutableObjectByteMap<K> of();

    /**
     * Same as {@link #empty()}.
     */
    <K> MutableObjectByteMap<K> with();

    /**
     * @since 11.1.
     */
    default <K> MutableObjectByteMap<K> with(K key, byte value)
    {
        MutableObjectByteMap<K> map = this.with();
        map.put(key, value);
        return map;
    }

    /**
     * @since 11.1.
     */
    default <K> MutableObjectByteMap<K> of(K key, byte value)
    {
        return this.with(key, value);
    }

    /**
     * @since 11.1.
     */
    default <K> MutableObjectByteMap<K> with(K key1, byte value1, K key2, byte value2)
    {
        MutableObjectByteMap<K> map = this.with(key1, value1);
        map.put(key2, value2);
        return map;
    }

    /**
     * @since 11.1.
     */
    default <K> MutableObjectByteMap<K> of(K key1, byte value1, K key2, byte value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    /**
     * @since 11.1.
     */
    default <K> MutableObjectByteMap<K> with(K key1, byte value1, K key2, byte value2, K key3, byte value3)
    {
        MutableObjectByteMap<K> map = this.with(key1, value1, key2, value2);
        map.put(key3, value3);
        return map;
    }

    /**
     * @since 11.1.
     */
    default <K> MutableObjectByteMap<K> of(K key1, byte value1, K key2, byte value2, K key3, byte value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    /**
     * @since 11.1.
     */
    default <K> MutableObjectByteMap<K> with(K key1, byte value1, K key2, byte value2, K key3, byte value3, K key4, byte value4)
    {
        MutableObjectByteMap<K> map = this.with(key1, value1, key2, value2, key3, value3);
        map.put(key4, value4);
        return map;
    }

    /**
     * @since 11.1.
     */
    default <K> MutableObjectByteMap<K> of(K key1, byte value1, K key2, byte value2, K key3, byte value3, K key4, byte value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    /**
     * Same as {@link #empty()}. but takes in an initial capacity
     */
    <K> MutableObjectByteMap<K> ofInitialCapacity(int capacity);

    /**
     * Same as {@link #empty()}. but takes in an initial capacity
     */
    <K> MutableObjectByteMap<K> withInitialCapacity(int capacity);

    /**
     * Same as {@link #withAll(ObjectByteMap)}.
     */
    <K> MutableObjectByteMap<K> ofAll(ObjectByteMap<? extends K> map);

    <K> MutableObjectByteMap<K> withAll(ObjectByteMap<? extends K> map);

    /**
     * Creates an {@code MutableObjectByteMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T, K> MutableObjectByteMap<K> from(Iterable<T> iterable, Function<? super T, ? extends K> keyFunction, ByteFunction<? super T> valueFunction);
}
