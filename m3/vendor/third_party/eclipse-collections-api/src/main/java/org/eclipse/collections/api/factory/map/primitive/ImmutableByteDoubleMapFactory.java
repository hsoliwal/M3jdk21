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
import org.eclipse.collections.api.block.function.primitive.DoubleFunction;
import org.eclipse.collections.api.map.primitive.ByteDoubleMap;
import org.eclipse.collections.api.map.primitive.ImmutableByteDoubleMap;

/**
 * A factory which creates instances of type {@link ImmutableByteDoubleMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactory.stg.
 *
 * @since 4.0.
 */
public interface ImmutableByteDoubleMapFactory
{
    /**
     * @since 6.0
     */
    ImmutableByteDoubleMap empty();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableByteDoubleMap of();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableByteDoubleMap with();

    /**
     * Same as {@link #with(byte, double)}.
     */
    ImmutableByteDoubleMap of(byte key, double value);

    ImmutableByteDoubleMap with(byte key, double value);

    /**
     * Same as {@link #withAll(ByteDoubleMap)}.
     */
    ImmutableByteDoubleMap ofAll(ByteDoubleMap map);

    ImmutableByteDoubleMap withAll(ByteDoubleMap map);

    /**
     * Creates an {@code ImmutableByteDoubleMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T> ImmutableByteDoubleMap from(Iterable<T> iterable, ByteFunction<? super T> keyFunction, DoubleFunction<? super T> valueFunction);
}
