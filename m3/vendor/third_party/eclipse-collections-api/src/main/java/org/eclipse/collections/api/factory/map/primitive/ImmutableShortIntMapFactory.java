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
import org.eclipse.collections.api.block.function.primitive.IntFunction;
import org.eclipse.collections.api.map.primitive.ShortIntMap;
import org.eclipse.collections.api.map.primitive.ImmutableShortIntMap;

/**
 * A factory which creates instances of type {@link ImmutableShortIntMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactory.stg.
 *
 * @since 4.0.
 */
public interface ImmutableShortIntMapFactory
{
    /**
     * @since 6.0
     */
    ImmutableShortIntMap empty();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableShortIntMap of();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableShortIntMap with();

    /**
     * Same as {@link #with(short, int)}.
     */
    ImmutableShortIntMap of(short key, int value);

    ImmutableShortIntMap with(short key, int value);

    /**
     * Same as {@link #withAll(ShortIntMap)}.
     */
    ImmutableShortIntMap ofAll(ShortIntMap map);

    ImmutableShortIntMap withAll(ShortIntMap map);

    /**
     * Creates an {@code ImmutableShortIntMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T> ImmutableShortIntMap from(Iterable<T> iterable, ShortFunction<? super T> keyFunction, IntFunction<? super T> valueFunction);
}
