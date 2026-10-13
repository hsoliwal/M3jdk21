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
import org.eclipse.collections.api.block.function.primitive.ShortFunction;
import org.eclipse.collections.api.map.primitive.CharShortMap;
import org.eclipse.collections.api.map.primitive.ImmutableCharShortMap;

/**
 * A factory which creates instances of type {@link ImmutableCharShortMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactory.stg.
 *
 * @since 4.0.
 */
public interface ImmutableCharShortMapFactory
{
    /**
     * @since 6.0
     */
    ImmutableCharShortMap empty();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableCharShortMap of();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableCharShortMap with();

    /**
     * Same as {@link #with(char, short)}.
     */
    ImmutableCharShortMap of(char key, short value);

    ImmutableCharShortMap with(char key, short value);

    /**
     * Same as {@link #withAll(CharShortMap)}.
     */
    ImmutableCharShortMap ofAll(CharShortMap map);

    ImmutableCharShortMap withAll(CharShortMap map);

    /**
     * Creates an {@code ImmutableCharShortMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T> ImmutableCharShortMap from(Iterable<T> iterable, CharFunction<? super T> keyFunction, ShortFunction<? super T> valueFunction);
}
