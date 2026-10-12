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
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.map.primitive.IntFloatMap;
import org.eclipse.collections.api.map.primitive.ImmutableIntFloatMap;

/**
 * A factory which creates instances of type {@link ImmutableIntFloatMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactory.stg.
 *
 * @since 4.0.
 */
public interface ImmutableIntFloatMapFactory
{
    /**
     * @since 6.0
     */
    ImmutableIntFloatMap empty();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableIntFloatMap of();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableIntFloatMap with();

    /**
     * Same as {@link #with(int, float)}.
     */
    ImmutableIntFloatMap of(int key, float value);

    ImmutableIntFloatMap with(int key, float value);

    /**
     * Same as {@link #withAll(IntFloatMap)}.
     */
    ImmutableIntFloatMap ofAll(IntFloatMap map);

    ImmutableIntFloatMap withAll(IntFloatMap map);

    /**
     * Creates an {@code ImmutableIntFloatMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T> ImmutableIntFloatMap from(Iterable<T> iterable, IntFunction<? super T> keyFunction, FloatFunction<? super T> valueFunction);
}
