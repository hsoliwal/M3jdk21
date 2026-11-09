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

import org.eclipse.collections.api.block.function.primitive.ShortFunction;
import org.eclipse.collections.api.block.function.primitive.LongFunction;
import org.eclipse.collections.api.map.primitive.ShortLongMap;
import org.eclipse.collections.api.map.primitive.ImmutableShortLongMap;

/**
 * A factory which creates instances of type {@link ImmutableShortLongMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactory.stg.
 *
 * @since 4.0.
 */
public interface ImmutableShortLongMapFactory
{
    /**
     * @since 6.0
     */
    ImmutableShortLongMap empty();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableShortLongMap of();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableShortLongMap with();

    /**
     * Same as {@link #with(short, long)}.
     */
    ImmutableShortLongMap of(short key, long value);

    ImmutableShortLongMap with(short key, long value);

    /**
     * Same as {@link #withAll(ShortLongMap)}.
     */
    ImmutableShortLongMap ofAll(ShortLongMap map);

    ImmutableShortLongMap withAll(ShortLongMap map);

    /**
     * Creates an {@code ImmutableShortLongMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T> ImmutableShortLongMap from(Iterable<T> iterable, ShortFunction<? super T> keyFunction, LongFunction<? super T> valueFunction);
}
