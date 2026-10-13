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

import org.eclipse.collections.api.block.function.primitive.IntFunction;
import org.eclipse.collections.api.block.function.primitive.ByteFunction;
import org.eclipse.collections.api.map.primitive.IntByteMap;
import org.eclipse.collections.api.map.primitive.ImmutableIntByteMap;

/**
 * A factory which creates instances of type {@link ImmutableIntByteMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactory.stg.
 *
 * @since 4.0.
 */
public interface ImmutableIntByteMapFactory
{
    /**
     * @since 6.0
     */
    ImmutableIntByteMap empty();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableIntByteMap of();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableIntByteMap with();

    /**
     * Same as {@link #with(int, byte)}.
     */
    ImmutableIntByteMap of(int key, byte value);

    ImmutableIntByteMap with(int key, byte value);

    /**
     * Same as {@link #withAll(IntByteMap)}.
     */
    ImmutableIntByteMap ofAll(IntByteMap map);

    ImmutableIntByteMap withAll(IntByteMap map);

    /**
     * Creates an {@code ImmutableIntByteMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T> ImmutableIntByteMap from(Iterable<T> iterable, IntFunction<? super T> keyFunction, ByteFunction<? super T> valueFunction);
}
