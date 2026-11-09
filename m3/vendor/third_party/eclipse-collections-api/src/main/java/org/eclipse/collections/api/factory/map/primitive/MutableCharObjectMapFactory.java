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
import org.eclipse.collections.api.map.primitive.MutableCharObjectMap;
import org.eclipse.collections.api.map.primitive.CharObjectMap;

/**
 * A factory which creates instances of type {@link MutableCharObjectMap}.
 * This file was automatically generated from template file mutablePrimitiveObjectMapFactory.stg.
 *
 * @since 6.0.
 */
public interface MutableCharObjectMapFactory
{
    <V> MutableCharObjectMap<V> empty();

    /**
     * Same as {@link #empty()}.
     */
    <V> MutableCharObjectMap<V> of();

    /**
     * Same as {@link #empty()}.
     */
    <V> MutableCharObjectMap<V> with();

    /**
     * @since 11.1.
     */
    default <V> MutableCharObjectMap<V> of(char key, V value)
    {
        return this.with(key, value);
    }

    /**
     * @since 11.1.
     */
    default <V> MutableCharObjectMap<V> with(char key, V value)
    {
        return this.<V>with().withKeyValue(key, value);
    }

    /**
     * @since 11.1.
     */
    default <V> MutableCharObjectMap<V> of(char key1, V value1, char key2, V value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    /**
     * @since 11.1.
     */
    default <V> MutableCharObjectMap<V> with(char key1, V value1, char key2, V value2)
    {
        return this.with(key1, value1).withKeyValue(key2, value2);
    }

    /**
     * @since 11.1.
     */
    default <V> MutableCharObjectMap<V> of(char key1, V value1, char key2, V value2, char key3, V value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    /**
     * @since 11.1.
     */
    default <V> MutableCharObjectMap<V> with(char key1, V value1, char key2, V value2, char key3, V value3)
    {
        return this.with(key1, value1, key2, value2).withKeyValue(key3, value3);
    }

    /**
     * @since 11.1.
     */
    default <V> MutableCharObjectMap<V> of(char key1, V value1, char key2, V value2, char key3, V value3, char key4, V value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    /**
     * @since 11.1.
     */
    default <V> MutableCharObjectMap<V> with(char key1, V value1, char key2, V value2, char key3, V value3, char key4, V value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3).withKeyValue(key4, value4);
    }

    /**
     * Same as {@link #empty()}. but takes in an initial capacity
     */
    <V> MutableCharObjectMap<V> ofInitialCapacity(int capacity);

    /**
     * Same as {@link #empty()}. but takes in an initial capacity
     */
    <V> MutableCharObjectMap<V> withInitialCapacity(int capacity);

    /**
     * Same as {@link #withAll(CharObjectMap)}.
     */
    <V> MutableCharObjectMap<V> ofAll(CharObjectMap<? extends V> map);

    <V> MutableCharObjectMap<V> withAll(CharObjectMap<? extends V> map);

    /**
     * Creates an {@code MutableCharObjectMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T, V> MutableCharObjectMap<V> from(Iterable<T> iterable, CharFunction<? super T> keyFunction, Function<? super T, ? extends V> valueFunction);
}
