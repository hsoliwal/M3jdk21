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
import org.eclipse.collections.api.map.primitive.ShortShortMap;
import org.eclipse.collections.api.map.primitive.ImmutableShortShortMap;

/**
 * A factory which creates instances of type {@link ImmutableShortShortMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactory.stg.
 *
 * @since 4.0.
 */
public interface ImmutableShortShortMapFactory
{
    /**
     * @since 6.0
     */
    ImmutableShortShortMap empty();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableShortShortMap of();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableShortShortMap with();

    /**
     * Same as {@link #with(short, short)}.
     */
    ImmutableShortShortMap of(short key, short value);

    ImmutableShortShortMap with(short key, short value);

    /**
     * Same as {@link #withAll(ShortShortMap)}.
     */
    ImmutableShortShortMap ofAll(ShortShortMap map);

    ImmutableShortShortMap withAll(ShortShortMap map);

    /**
     * Creates an {@code ImmutableShortShortMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T> ImmutableShortShortMap from(Iterable<T> iterable, ShortFunction<? super T> keyFunction, ShortFunction<? super T> valueFunction);
}
