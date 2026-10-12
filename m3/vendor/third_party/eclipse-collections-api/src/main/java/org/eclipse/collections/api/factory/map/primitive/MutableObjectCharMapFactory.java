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
import org.eclipse.collections.api.block.function.primitive.CharFunction;
import org.eclipse.collections.api.map.primitive.MutableObjectCharMap;
import org.eclipse.collections.api.map.primitive.ObjectCharMap;

/**
 * A factory which creates instances of type {@link MutableObjectCharMap}.
 * This file was automatically generated from template file mutableObjectPrimitiveMapFactory.stg.
 *
 * @since 6.0.
 */
public interface MutableObjectCharMapFactory
{
    <K> MutableObjectCharMap<K> empty();

    /**
     * Same as {@link #empty()}.
     */
    <K> MutableObjectCharMap<K> of();

    /**
     * Same as {@link #empty()}.
     */
    <K> MutableObjectCharMap<K> with();

    /**
     * @since 11.1.
     */
    default <K> MutableObjectCharMap<K> with(K key, char value)
    {
        MutableObjectCharMap<K> map = this.with();
        map.put(key, value);
        return map;
    }

    /**
     * @since 11.1.
     */
    default <K> MutableObjectCharMap<K> of(K key, char value)
    {
        return this.with(key, value);
    }

    /**
     * @since 11.1.
     */
    default <K> MutableObjectCharMap<K> with(K key1, char value1, K key2, char value2)
    {
        MutableObjectCharMap<K> map = this.with(key1, value1);
        map.put(key2, value2);
        return map;
    }

    /**
     * @since 11.1.
     */
    default <K> MutableObjectCharMap<K> of(K key1, char value1, K key2, char value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    /**
     * @since 11.1.
     */
    default <K> MutableObjectCharMap<K> with(K key1, char value1, K key2, char value2, K key3, char value3)
    {
        MutableObjectCharMap<K> map = this.with(key1, value1, key2, value2);
        map.put(key3, value3);
        return map;
    }

    /**
     * @since 11.1.
     */
    default <K> MutableObjectCharMap<K> of(K key1, char value1, K key2, char value2, K key3, char value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    /**
     * @since 11.1.
     */
    default <K> MutableObjectCharMap<K> with(K key1, char value1, K key2, char value2, K key3, char value3, K key4, char value4)
    {
        MutableObjectCharMap<K> map = this.with(key1, value1, key2, value2, key3, value3);
        map.put(key4, value4);
        return map;
    }

    /**
     * @since 11.1.
     */
    default <K> MutableObjectCharMap<K> of(K key1, char value1, K key2, char value2, K key3, char value3, K key4, char value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    /**
     * Same as {@link #empty()}. but takes in an initial capacity
     */
    <K> MutableObjectCharMap<K> ofInitialCapacity(int capacity);

    /**
     * Same as {@link #empty()}. but takes in an initial capacity
     */
    <K> MutableObjectCharMap<K> withInitialCapacity(int capacity);

    /**
     * Same as {@link #withAll(ObjectCharMap)}.
     */
    <K> MutableObjectCharMap<K> ofAll(ObjectCharMap<? extends K> map);

    <K> MutableObjectCharMap<K> withAll(ObjectCharMap<? extends K> map);

    /**
     * Creates an {@code MutableObjectCharMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T, K> MutableObjectCharMap<K> from(Iterable<T> iterable, Function<? super T, ? extends K> keyFunction, CharFunction<? super T> valueFunction);
}
