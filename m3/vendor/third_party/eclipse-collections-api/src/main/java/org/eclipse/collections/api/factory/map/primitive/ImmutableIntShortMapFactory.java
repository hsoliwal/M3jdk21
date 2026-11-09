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
import org.eclipse.collections.api.block.function.primitive.ShortFunction;
import org.eclipse.collections.api.map.primitive.IntShortMap;
import org.eclipse.collections.api.map.primitive.ImmutableIntShortMap;

/**
 * A factory which creates instances of type {@link ImmutableIntShortMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactory.stg.
 *
 * @since 4.0.
 */
public interface ImmutableIntShortMapFactory
{
    /**
     * @since 6.0
     */
    ImmutableIntShortMap empty();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableIntShortMap of();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableIntShortMap with();

    /**
     * Same as {@link #with(int, short)}.
     */
    ImmutableIntShortMap of(int key, short value);

    ImmutableIntShortMap with(int key, short value);

    /**
     * Same as {@link #withAll(IntShortMap)}.
     */
    ImmutableIntShortMap ofAll(IntShortMap map);

    ImmutableIntShortMap withAll(IntShortMap map);

    /**
     * Creates an {@code ImmutableIntShortMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T> ImmutableIntShortMap from(Iterable<T> iterable, IntFunction<? super T> keyFunction, ShortFunction<? super T> valueFunction);
}
