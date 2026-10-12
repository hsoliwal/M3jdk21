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
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.map.primitive.ShortFloatMap;
import org.eclipse.collections.api.map.primitive.ImmutableShortFloatMap;

/**
 * A factory which creates instances of type {@link ImmutableShortFloatMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactory.stg.
 *
 * @since 4.0.
 */
public interface ImmutableShortFloatMapFactory
{
    /**
     * @since 6.0
     */
    ImmutableShortFloatMap empty();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableShortFloatMap of();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableShortFloatMap with();

    /**
     * Same as {@link #with(short, float)}.
     */
    ImmutableShortFloatMap of(short key, float value);

    ImmutableShortFloatMap with(short key, float value);

    /**
     * Same as {@link #withAll(ShortFloatMap)}.
     */
    ImmutableShortFloatMap ofAll(ShortFloatMap map);

    ImmutableShortFloatMap withAll(ShortFloatMap map);

    /**
     * Creates an {@code ImmutableShortFloatMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T> ImmutableShortFloatMap from(Iterable<T> iterable, ShortFunction<? super T> keyFunction, FloatFunction<? super T> valueFunction);
}
