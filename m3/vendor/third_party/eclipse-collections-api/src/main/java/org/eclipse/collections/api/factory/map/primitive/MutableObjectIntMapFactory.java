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
import org.eclipse.collections.api.map.primitive.MutableObjectIntMap;
import org.eclipse.collections.api.map.primitive.ObjectIntMap;

/**
 * A factory which creates instances of type {@link MutableObjectIntMap}.
 * This file was automatically generated from template file mutableObjectPrimitiveMapFactory.stg.
 *
 * @since 6.0.
 */
public interface MutableObjectIntMapFactory
{
    <K> MutableObjectIntMap<K> empty();

    /**
     * Same as {@link #empty()}.
     */
    <K> MutableObjectIntMap<K> of();

    /**
     * Same as {@link #empty()}.
     */
    <K> MutableObjectIntMap<K> with();

    /**
     * @since 11.1.
     */
    default <K> MutableObjectIntMap<K> with(K key, int value)
    {
        MutableObjectIntMap<K> map = this.with();
        map.put(key, value);
        return map;
    }

    /**
     * @since 11.1.
     */
    default <K> MutableObjectIntMap<K> of(K key, int value)
    {
        return this.with(key, value);
    }

    /**
     * @since 11.1.
     */
    default <K> MutableObjectIntMap<K> with(K key1, int value1, K key2, int value2)
    {
        MutableObjectIntMap<K> map = this.with(key1, value1);
        map.put(key2, value2);
        return map;
    }

    /**
     * @since 11.1.
     */
    default <K> MutableObjectIntMap<K> of(K key1, int value1, K key2, int value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    /**
     * @since 11.1.
     */
    default <K> MutableObjectIntMap<K> with(K key1, int value1, K key2, int value2, K key3, int value3)
    {
        MutableObjectIntMap<K> map = this.with(key1, value1, key2, value2);
        map.put(key3, value3);
        return map;
    }

    /**
     * @since 11.1.
     */
    default <K> MutableObjectIntMap<K> of(K key1, int value1, K key2, int value2, K key3, int value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    /**
     * @since 11.1.
     */
    default <K> MutableObjectIntMap<K> with(K key1, int value1, K key2, int value2, K key3, int value3, K key4, int value4)
    {
        MutableObjectIntMap<K> map = this.with(key1, value1, key2, value2, key3, value3);
        map.put(key4, value4);
        return map;
    }

    /**
     * @since 11.1.
     */
    default <K> MutableObjectIntMap<K> of(K key1, int value1, K key2, int value2, K key3, int value3, K key4, int value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    /**
     * Same as {@link #empty()}. but takes in an initial capacity
     */
    <K> MutableObjectIntMap<K> ofInitialCapacity(int capacity);

    /**
     * Same as {@link #empty()}. but takes in an initial capacity
     */
    <K> MutableObjectIntMap<K> withInitialCapacity(int capacity);

    /**
     * Same as {@link #withAll(ObjectIntMap)}.
     */
    <K> MutableObjectIntMap<K> ofAll(ObjectIntMap<? extends K> map);

    <K> MutableObjectIntMap<K> withAll(ObjectIntMap<? extends K> map);

    /**
     * Creates an {@code MutableObjectIntMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T, K> MutableObjectIntMap<K> from(Iterable<T> iterable, Function<? super T, ? extends K> keyFunction, IntFunction<? super T> valueFunction);
}
