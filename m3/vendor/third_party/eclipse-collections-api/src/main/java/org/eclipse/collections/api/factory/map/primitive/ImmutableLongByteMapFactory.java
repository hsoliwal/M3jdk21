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

import org.eclipse.collections.api.block.function.primitive.LongFunction;
import org.eclipse.collections.api.block.function.primitive.ByteFunction;
import org.eclipse.collections.api.map.primitive.LongByteMap;
import org.eclipse.collections.api.map.primitive.ImmutableLongByteMap;

/**
 * A factory which creates instances of type {@link ImmutableLongByteMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactory.stg.
 *
 * @since 4.0.
 */
public interface ImmutableLongByteMapFactory
{
    /**
     * @since 6.0
     */
    ImmutableLongByteMap empty();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableLongByteMap of();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableLongByteMap with();

    /**
     * Same as {@link #with(long, byte)}.
     */
    ImmutableLongByteMap of(long key, byte value);

    ImmutableLongByteMap with(long key, byte value);

    /**
     * Same as {@link #withAll(LongByteMap)}.
     */
    ImmutableLongByteMap ofAll(LongByteMap map);

    ImmutableLongByteMap withAll(LongByteMap map);

    /**
     * Creates an {@code ImmutableLongByteMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T> ImmutableLongByteMap from(Iterable<T> iterable, LongFunction<? super T> keyFunction, ByteFunction<? super T> valueFunction);
}
