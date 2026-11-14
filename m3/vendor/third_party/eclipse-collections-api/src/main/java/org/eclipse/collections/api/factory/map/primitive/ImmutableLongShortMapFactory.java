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

import org.eclipse.collections.api.block.function.primitive.LongFunction;
import org.eclipse.collections.api.block.function.primitive.ShortFunction;
import org.eclipse.collections.api.map.primitive.LongShortMap;
import org.eclipse.collections.api.map.primitive.ImmutableLongShortMap;

/**
 * A factory which creates instances of type {@link ImmutableLongShortMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactory.stg.
 *
 * @since 4.0.
 */
public interface ImmutableLongShortMapFactory
{
    /**
     * @since 6.0
     */
    ImmutableLongShortMap empty();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableLongShortMap of();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableLongShortMap with();

    /**
     * Same as {@link #with(long, short)}.
     */
    ImmutableLongShortMap of(long key, short value);

    ImmutableLongShortMap with(long key, short value);

    /**
     * Same as {@link #withAll(LongShortMap)}.
     */
    ImmutableLongShortMap ofAll(LongShortMap map);

    ImmutableLongShortMap withAll(LongShortMap map);

    /**
     * Creates an {@code ImmutableLongShortMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T> ImmutableLongShortMap from(Iterable<T> iterable, LongFunction<? super T> keyFunction, ShortFunction<? super T> valueFunction);
}
