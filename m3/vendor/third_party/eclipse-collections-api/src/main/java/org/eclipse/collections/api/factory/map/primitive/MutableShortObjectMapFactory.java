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
import org.eclipse.collections.api.block.function.primitive.ShortFunction;
import org.eclipse.collections.api.map.primitive.MutableShortObjectMap;
import org.eclipse.collections.api.map.primitive.ShortObjectMap;

/**
 * A factory which creates instances of type {@link MutableShortObjectMap}.
 * This file was automatically generated from template file mutablePrimitiveObjectMapFactory.stg.
 *
 * @since 6.0.
 */
public interface MutableShortObjectMapFactory
{
    <V> MutableShortObjectMap<V> empty();

    /**
     * Same as {@link #empty()}.
     */
    <V> MutableShortObjectMap<V> of();

    /**
     * Same as {@link #empty()}.
     */
    <V> MutableShortObjectMap<V> with();

    /**
     * @since 11.1.
     */
    default <V> MutableShortObjectMap<V> of(short key, V value)
    {
        return this.with(key, value);
    }

    /**
     * @since 11.1.
     */
    default <V> MutableShortObjectMap<V> with(short key, V value)
    {
        return this.<V>with().withKeyValue(key, value);
    }

    /**
     * @since 11.1.
     */
    default <V> MutableShortObjectMap<V> of(short key1, V value1, short key2, V value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    /**
     * @since 11.1.
     */
    default <V> MutableShortObjectMap<V> with(short key1, V value1, short key2, V value2)
    {
        return this.with(key1, value1).withKeyValue(key2, value2);
    }

    /**
     * @since 11.1.
     */
    default <V> MutableShortObjectMap<V> of(short key1, V value1, short key2, V value2, short key3, V value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    /**
     * @since 11.1.
     */
    default <V> MutableShortObjectMap<V> with(short key1, V value1, short key2, V value2, short key3, V value3)
    {
        return this.with(key1, value1, key2, value2).withKeyValue(key3, value3);
    }

    /**
     * @since 11.1.
     */
    default <V> MutableShortObjectMap<V> of(short key1, V value1, short key2, V value2, short key3, V value3, short key4, V value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    /**
     * @since 11.1.
     */
    default <V> MutableShortObjectMap<V> with(short key1, V value1, short key2, V value2, short key3, V value3, short key4, V value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3).withKeyValue(key4, value4);
    }

    /**
     * Same as {@link #empty()}. but takes in an initial capacity
     */
    <V> MutableShortObjectMap<V> ofInitialCapacity(int capacity);

    /**
     * Same as {@link #empty()}. but takes in an initial capacity
     */
    <V> MutableShortObjectMap<V> withInitialCapacity(int capacity);

    /**
     * Same as {@link #withAll(ShortObjectMap)}.
     */
    <V> MutableShortObjectMap<V> ofAll(ShortObjectMap<? extends V> map);

    <V> MutableShortObjectMap<V> withAll(ShortObjectMap<? extends V> map);

    /**
     * Creates an {@code MutableShortObjectMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T, V> MutableShortObjectMap<V> from(Iterable<T> iterable, ShortFunction<? super T> keyFunction, Function<? super T, ? extends V> valueFunction);
}
