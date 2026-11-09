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
import org.eclipse.collections.api.block.function.primitive.BooleanFunction;
import org.eclipse.collections.api.map.primitive.MutableObjectBooleanMap;
import org.eclipse.collections.api.map.primitive.ObjectBooleanMap;

/**
 * A factory which creates instances of type {@link MutableObjectBooleanMap}.
 * This file was automatically generated from template file mutableObjectPrimitiveMapFactory.stg.
 *
 * @since 6.0.
 */
public interface MutableObjectBooleanMapFactory
{
    <K> MutableObjectBooleanMap<K> empty();

    /**
     * Same as {@link #empty()}.
     */
    <K> MutableObjectBooleanMap<K> of();

    /**
     * Same as {@link #empty()}.
     */
    <K> MutableObjectBooleanMap<K> with();

    /**
     * @since 11.1.
     */
    default <K> MutableObjectBooleanMap<K> with(K key, boolean value)
    {
        MutableObjectBooleanMap<K> map = this.with();
        map.put(key, value);
        return map;
    }

    /**
     * @since 11.1.
     */
    default <K> MutableObjectBooleanMap<K> of(K key, boolean value)
    {
        return this.with(key, value);
    }

    /**
     * @since 11.1.
     */
    default <K> MutableObjectBooleanMap<K> with(K key1, boolean value1, K key2, boolean value2)
    {
        MutableObjectBooleanMap<K> map = this.with(key1, value1);
        map.put(key2, value2);
        return map;
    }

    /**
     * @since 11.1.
     */
    default <K> MutableObjectBooleanMap<K> of(K key1, boolean value1, K key2, boolean value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    /**
     * @since 11.1.
     */
    default <K> MutableObjectBooleanMap<K> with(K key1, boolean value1, K key2, boolean value2, K key3, boolean value3)
    {
        MutableObjectBooleanMap<K> map = this.with(key1, value1, key2, value2);
        map.put(key3, value3);
        return map;
    }

    /**
     * @since 11.1.
     */
    default <K> MutableObjectBooleanMap<K> of(K key1, boolean value1, K key2, boolean value2, K key3, boolean value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    /**
     * @since 11.1.
     */
    default <K> MutableObjectBooleanMap<K> with(K key1, boolean value1, K key2, boolean value2, K key3, boolean value3, K key4, boolean value4)
    {
        MutableObjectBooleanMap<K> map = this.with(key1, value1, key2, value2, key3, value3);
        map.put(key4, value4);
        return map;
    }

    /**
     * @since 11.1.
     */
    default <K> MutableObjectBooleanMap<K> of(K key1, boolean value1, K key2, boolean value2, K key3, boolean value3, K key4, boolean value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    /**
     * Same as {@link #empty()}. but takes in an initial capacity
     */
    <K> MutableObjectBooleanMap<K> ofInitialCapacity(int capacity);

    /**
     * Same as {@link #empty()}. but takes in an initial capacity
     */
    <K> MutableObjectBooleanMap<K> withInitialCapacity(int capacity);

    /**
     * Same as {@link #withAll(ObjectBooleanMap)}.
     */
    <K> MutableObjectBooleanMap<K> ofAll(ObjectBooleanMap<? extends K> map);

    <K> MutableObjectBooleanMap<K> withAll(ObjectBooleanMap<? extends K> map);

    /**
     * Creates an {@code MutableObjectBooleanMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T, K> MutableObjectBooleanMap<K> from(Iterable<T> iterable, Function<? super T, ? extends K> keyFunction, BooleanFunction<? super T> valueFunction);
}
