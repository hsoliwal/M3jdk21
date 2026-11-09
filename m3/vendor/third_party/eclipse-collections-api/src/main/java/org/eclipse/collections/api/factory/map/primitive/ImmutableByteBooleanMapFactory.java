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
import org.eclipse.collections.api.block.function.primitive.BooleanFunction;
import org.eclipse.collections.api.map.primitive.ByteBooleanMap;
import org.eclipse.collections.api.map.primitive.ImmutableByteBooleanMap;

/**
 * A factory which creates instances of type {@link ImmutableByteBooleanMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactory.stg.
 *
 * @since 4.0.
 */
public interface ImmutableByteBooleanMapFactory
{
    /**
     * @since 6.0
     */
    ImmutableByteBooleanMap empty();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableByteBooleanMap of();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableByteBooleanMap with();

    /**
     * Same as {@link #with(byte, boolean)}.
     */
    ImmutableByteBooleanMap of(byte key, boolean value);

    ImmutableByteBooleanMap with(byte key, boolean value);

    /**
     * Same as {@link #withAll(ByteBooleanMap)}.
     */
    ImmutableByteBooleanMap ofAll(ByteBooleanMap map);

    ImmutableByteBooleanMap withAll(ByteBooleanMap map);

    /**
     * Creates an {@code ImmutableByteBooleanMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T> ImmutableByteBooleanMap from(Iterable<T> iterable, ByteFunction<? super T> keyFunction, BooleanFunction<? super T> valueFunction);
}
