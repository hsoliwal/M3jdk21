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
import org.eclipse.collections.api.block.function.primitive.CharFunction;
import org.eclipse.collections.api.map.primitive.IntCharMap;
import org.eclipse.collections.api.map.primitive.ImmutableIntCharMap;

/**
 * A factory which creates instances of type {@link ImmutableIntCharMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactory.stg.
 *
 * @since 4.0.
 */
public interface ImmutableIntCharMapFactory
{
    /**
     * @since 6.0
     */
    ImmutableIntCharMap empty();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableIntCharMap of();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableIntCharMap with();

    /**
     * Same as {@link #with(int, char)}.
     */
    ImmutableIntCharMap of(int key, char value);

    ImmutableIntCharMap with(int key, char value);

    /**
     * Same as {@link #withAll(IntCharMap)}.
     */
    ImmutableIntCharMap ofAll(IntCharMap map);

    ImmutableIntCharMap withAll(IntCharMap map);

    /**
     * Creates an {@code ImmutableIntCharMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T> ImmutableIntCharMap from(Iterable<T> iterable, IntFunction<? super T> keyFunction, CharFunction<? super T> valueFunction);
}
