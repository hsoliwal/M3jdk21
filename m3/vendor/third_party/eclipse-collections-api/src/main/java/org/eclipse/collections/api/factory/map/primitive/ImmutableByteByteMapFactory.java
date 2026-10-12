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

import org.eclipse.collections.api.block.function.primitive.ByteFunction;
import org.eclipse.collections.api.map.primitive.ByteByteMap;
import org.eclipse.collections.api.map.primitive.ImmutableByteByteMap;

/**
 * A factory which creates instances of type {@link ImmutableByteByteMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactory.stg.
 *
 * @since 4.0.
 */
public interface ImmutableByteByteMapFactory
{
    /**
     * @since 6.0
     */
    ImmutableByteByteMap empty();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableByteByteMap of();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableByteByteMap with();

    /**
     * Same as {@link #with(byte, byte)}.
     */
    ImmutableByteByteMap of(byte key, byte value);

    ImmutableByteByteMap with(byte key, byte value);

    /**
     * Same as {@link #withAll(ByteByteMap)}.
     */
    ImmutableByteByteMap ofAll(ByteByteMap map);

    ImmutableByteByteMap withAll(ByteByteMap map);

    /**
     * Creates an {@code ImmutableByteByteMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T> ImmutableByteByteMap from(Iterable<T> iterable, ByteFunction<? super T> keyFunction, ByteFunction<? super T> valueFunction);
}
