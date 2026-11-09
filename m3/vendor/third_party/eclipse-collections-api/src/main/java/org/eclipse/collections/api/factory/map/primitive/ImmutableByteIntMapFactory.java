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
import org.eclipse.collections.api.block.function.primitive.IntFunction;
import org.eclipse.collections.api.map.primitive.ByteIntMap;
import org.eclipse.collections.api.map.primitive.ImmutableByteIntMap;

/**
 * A factory which creates instances of type {@link ImmutableByteIntMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactory.stg.
 *
 * @since 4.0.
 */
public interface ImmutableByteIntMapFactory
{
    /**
     * @since 6.0
     */
    ImmutableByteIntMap empty();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableByteIntMap of();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableByteIntMap with();

    /**
     * Same as {@link #with(byte, int)}.
     */
    ImmutableByteIntMap of(byte key, int value);

    ImmutableByteIntMap with(byte key, int value);

    /**
     * Same as {@link #withAll(ByteIntMap)}.
     */
    ImmutableByteIntMap ofAll(ByteIntMap map);

    ImmutableByteIntMap withAll(ByteIntMap map);

    /**
     * Creates an {@code ImmutableByteIntMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T> ImmutableByteIntMap from(Iterable<T> iterable, ByteFunction<? super T> keyFunction, IntFunction<? super T> valueFunction);
}
