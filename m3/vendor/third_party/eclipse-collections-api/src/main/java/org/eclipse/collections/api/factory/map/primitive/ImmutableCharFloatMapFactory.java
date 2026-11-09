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
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.map.primitive.CharFloatMap;
import org.eclipse.collections.api.map.primitive.ImmutableCharFloatMap;

/**
 * A factory which creates instances of type {@link ImmutableCharFloatMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactory.stg.
 *
 * @since 4.0.
 */
public interface ImmutableCharFloatMapFactory
{
    /**
     * @since 6.0
     */
    ImmutableCharFloatMap empty();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableCharFloatMap of();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableCharFloatMap with();

    /**
     * Same as {@link #with(char, float)}.
     */
    ImmutableCharFloatMap of(char key, float value);

    ImmutableCharFloatMap with(char key, float value);

    /**
     * Same as {@link #withAll(CharFloatMap)}.
     */
    ImmutableCharFloatMap ofAll(CharFloatMap map);

    ImmutableCharFloatMap withAll(CharFloatMap map);

    /**
     * Creates an {@code ImmutableCharFloatMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T> ImmutableCharFloatMap from(Iterable<T> iterable, CharFunction<? super T> keyFunction, FloatFunction<? super T> valueFunction);
}
