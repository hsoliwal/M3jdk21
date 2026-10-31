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
import org.eclipse.collections.api.map.primitive.ImmutableObjectByteMap;
import org.eclipse.collections.api.map.primitive.ObjectByteMap;

/**
 * A factory which creates instances of type {@link ImmutableObjectByteMap}.
 * This file was automatically generated from template file immutableObjectPrimitiveMapFactory.stg.
 *
 * @since 4.0.
 */
public interface ImmutableObjectByteMapFactory
{
    /**
     * @since 6.0
     */
    <K> ImmutableObjectByteMap<K> empty();

    /**
     * Same as {@link #empty()}.
     */
    <K> ImmutableObjectByteMap<K> of();

    /**
     * Same as {@link #empty()}.
     */
    <K> ImmutableObjectByteMap<K> with();

    /**
     * Same as {@link #with(Object, byte)}.
     */
    <K> ImmutableObjectByteMap<K> of(K key, byte value);

    <K> ImmutableObjectByteMap<K> with(K key, byte value);

    /**
     * Same as {@link #withAll(ObjectByteMap)}.
     */
    <K> ImmutableObjectByteMap<K> ofAll(ObjectByteMap<? extends K> map);

    <K> ImmutableObjectByteMap<K> withAll(ObjectByteMap<? extends K> map);

    /**
     * Creates an {@code ImmutableObjectByteMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T, K> ImmutableObjectByteMap<K> from(Iterable<T> iterable, Function<? super T, ? extends K> keyFunction, ByteFunction<? super T> valueFunction);
}
