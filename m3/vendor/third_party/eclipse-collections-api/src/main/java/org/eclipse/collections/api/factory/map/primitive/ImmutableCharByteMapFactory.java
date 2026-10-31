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

import org.eclipse.collections.api.block.function.primitive.CharFunction;
import org.eclipse.collections.api.block.function.primitive.ByteFunction;
import org.eclipse.collections.api.map.primitive.CharByteMap;
import org.eclipse.collections.api.map.primitive.ImmutableCharByteMap;

/**
 * A factory which creates instances of type {@link ImmutableCharByteMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactory.stg.
 *
 * @since 4.0.
 */
public interface ImmutableCharByteMapFactory
{
    /**
     * @since 6.0
     */
    ImmutableCharByteMap empty();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableCharByteMap of();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableCharByteMap with();

    /**
     * Same as {@link #with(char, byte)}.
     */
    ImmutableCharByteMap of(char key, byte value);

    ImmutableCharByteMap with(char key, byte value);

    /**
     * Same as {@link #withAll(CharByteMap)}.
     */
    ImmutableCharByteMap ofAll(CharByteMap map);

    ImmutableCharByteMap withAll(CharByteMap map);

    /**
     * Creates an {@code ImmutableCharByteMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T> ImmutableCharByteMap from(Iterable<T> iterable, CharFunction<? super T> keyFunction, ByteFunction<? super T> valueFunction);
}
