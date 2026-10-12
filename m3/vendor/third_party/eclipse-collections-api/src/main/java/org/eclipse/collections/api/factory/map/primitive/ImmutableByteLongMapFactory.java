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
import org.eclipse.collections.api.block.function.primitive.LongFunction;
import org.eclipse.collections.api.map.primitive.ByteLongMap;
import org.eclipse.collections.api.map.primitive.ImmutableByteLongMap;

/**
 * A factory which creates instances of type {@link ImmutableByteLongMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactory.stg.
 *
 * @since 4.0.
 */
public interface ImmutableByteLongMapFactory
{
    /**
     * @since 6.0
     */
    ImmutableByteLongMap empty();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableByteLongMap of();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableByteLongMap with();

    /**
     * Same as {@link #with(byte, long)}.
     */
    ImmutableByteLongMap of(byte key, long value);

    ImmutableByteLongMap with(byte key, long value);

    /**
     * Same as {@link #withAll(ByteLongMap)}.
     */
    ImmutableByteLongMap ofAll(ByteLongMap map);

    ImmutableByteLongMap withAll(ByteLongMap map);

    /**
     * Creates an {@code ImmutableByteLongMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T> ImmutableByteLongMap from(Iterable<T> iterable, ByteFunction<? super T> keyFunction, LongFunction<? super T> valueFunction);
}
