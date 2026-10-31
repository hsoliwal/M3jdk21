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
import org.eclipse.collections.api.block.function.primitive.CharFunction;
import org.eclipse.collections.api.map.primitive.ShortCharMap;
import org.eclipse.collections.api.map.primitive.ImmutableShortCharMap;

/**
 * A factory which creates instances of type {@link ImmutableShortCharMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactory.stg.
 *
 * @since 4.0.
 */
public interface ImmutableShortCharMapFactory
{
    /**
     * @since 6.0
     */
    ImmutableShortCharMap empty();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableShortCharMap of();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableShortCharMap with();

    /**
     * Same as {@link #with(short, char)}.
     */
    ImmutableShortCharMap of(short key, char value);

    ImmutableShortCharMap with(short key, char value);

    /**
     * Same as {@link #withAll(ShortCharMap)}.
     */
    ImmutableShortCharMap ofAll(ShortCharMap map);

    ImmutableShortCharMap withAll(ShortCharMap map);

    /**
     * Creates an {@code ImmutableShortCharMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T> ImmutableShortCharMap from(Iterable<T> iterable, ShortFunction<? super T> keyFunction, CharFunction<? super T> valueFunction);
}
