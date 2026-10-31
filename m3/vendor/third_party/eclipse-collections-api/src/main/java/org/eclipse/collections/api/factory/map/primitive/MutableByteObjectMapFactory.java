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
import org.eclipse.collections.api.map.primitive.MutableByteObjectMap;
import org.eclipse.collections.api.map.primitive.ByteObjectMap;

/**
 * A factory which creates instances of type {@link MutableByteObjectMap}.
 * This file was automatically generated from template file mutablePrimitiveObjectMapFactory.stg.
 *
 * @since 6.0.
 */
public interface MutableByteObjectMapFactory
{
    <V> MutableByteObjectMap<V> empty();

    /**
     * Same as {@link #empty()}.
     */
    <V> MutableByteObjectMap<V> of();

    /**
     * Same as {@link #empty()}.
     */
    <V> MutableByteObjectMap<V> with();

    /**
     * @since 11.1.
     */
    default <V> MutableByteObjectMap<V> of(byte key, V value)
    {
        return this.with(key, value);
    }

    /**
     * @since 11.1.
     */
    default <V> MutableByteObjectMap<V> with(byte key, V value)
    {
        return this.<V>with().withKeyValue(key, value);
    }

    /**
     * @since 11.1.
     */
    default <V> MutableByteObjectMap<V> of(byte key1, V value1, byte key2, V value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    /**
     * @since 11.1.
     */
    default <V> MutableByteObjectMap<V> with(byte key1, V value1, byte key2, V value2)
    {
        return this.with(key1, value1).withKeyValue(key2, value2);
    }

    /**
     * @since 11.1.
     */
    default <V> MutableByteObjectMap<V> of(byte key1, V value1, byte key2, V value2, byte key3, V value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    /**
     * @since 11.1.
     */
    default <V> MutableByteObjectMap<V> with(byte key1, V value1, byte key2, V value2, byte key3, V value3)
    {
        return this.with(key1, value1, key2, value2).withKeyValue(key3, value3);
    }

    /**
     * @since 11.1.
     */
    default <V> MutableByteObjectMap<V> of(byte key1, V value1, byte key2, V value2, byte key3, V value3, byte key4, V value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    /**
     * @since 11.1.
     */
    default <V> MutableByteObjectMap<V> with(byte key1, V value1, byte key2, V value2, byte key3, V value3, byte key4, V value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3).withKeyValue(key4, value4);
    }

    /**
     * Same as {@link #empty()}. but takes in an initial capacity
     */
    <V> MutableByteObjectMap<V> ofInitialCapacity(int capacity);

    /**
     * Same as {@link #empty()}. but takes in an initial capacity
     */
    <V> MutableByteObjectMap<V> withInitialCapacity(int capacity);

    /**
     * Same as {@link #withAll(ByteObjectMap)}.
     */
    <V> MutableByteObjectMap<V> ofAll(ByteObjectMap<? extends V> map);

    <V> MutableByteObjectMap<V> withAll(ByteObjectMap<? extends V> map);

    /**
     * Creates an {@code MutableByteObjectMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T, V> MutableByteObjectMap<V> from(Iterable<T> iterable, ByteFunction<? super T> keyFunction, Function<? super T, ? extends V> valueFunction);
}
