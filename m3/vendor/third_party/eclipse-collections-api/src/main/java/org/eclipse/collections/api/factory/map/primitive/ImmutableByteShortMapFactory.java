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
import org.eclipse.collections.api.block.function.primitive.ShortFunction;
import org.eclipse.collections.api.map.primitive.ByteShortMap;
import org.eclipse.collections.api.map.primitive.ImmutableByteShortMap;

/**
 * A factory which creates instances of type {@link ImmutableByteShortMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactory.stg.
 *
 * @since 4.0.
 */
public interface ImmutableByteShortMapFactory
{
    /**
     * @since 6.0
     */
    ImmutableByteShortMap empty();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableByteShortMap of();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableByteShortMap with();

    /**
     * Same as {@link #with(byte, short)}.
     */
    ImmutableByteShortMap of(byte key, short value);

    ImmutableByteShortMap with(byte key, short value);

    /**
     * Same as {@link #withAll(ByteShortMap)}.
     */
    ImmutableByteShortMap ofAll(ByteShortMap map);

    ImmutableByteShortMap withAll(ByteShortMap map);

    /**
     * Creates an {@code ImmutableByteShortMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T> ImmutableByteShortMap from(Iterable<T> iterable, ByteFunction<? super T> keyFunction, ShortFunction<? super T> valueFunction);
}
