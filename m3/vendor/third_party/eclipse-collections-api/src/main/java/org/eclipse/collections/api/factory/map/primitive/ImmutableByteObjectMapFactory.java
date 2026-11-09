/*
 * Copyright (c) 2022 Goldman Sachs.
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
import org.eclipse.collections.api.map.primitive.ImmutableByteObjectMap;
import org.eclipse.collections.api.map.primitive.ByteObjectMap;

/**
 * A factory which creates instances of type {@link ImmutableByteObjectMap}.
 * This file was automatically generated from template file immutablePrimitiveObjectMapFactory.stg.
 *
 * @since 4.0.
 */
public interface ImmutableByteObjectMapFactory
{
    /**
     * @since 6.0
     */
    <V> ImmutableByteObjectMap<V> empty();

    /**
     * Same as {@link #empty()}.
     */
    <V> ImmutableByteObjectMap<V> of();

    /**
     * Same as {@link #empty()}.
     */
    <V> ImmutableByteObjectMap<V> with();

    /**
     * Same as {@link #with(byte, Object)}.
     */
    <V> ImmutableByteObjectMap<V> of(byte key, V value);

    <V> ImmutableByteObjectMap<V> with(byte key, V value);

    /**
     * Same as {@link #withAll(ByteObjectMap)}.
     */
    <V> ImmutableByteObjectMap<V> ofAll(ByteObjectMap<? extends V> map);

    <V> ImmutableByteObjectMap<V> withAll(ByteObjectMap<? extends V> map);

    /**
     * Creates an {@code ImmutableByteObjectMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T, V> ImmutableByteObjectMap<V> from(Iterable<T> iterable, ByteFunction<? super T> keyFunction, Function<? super T, ? extends V> valueFunction);
}
