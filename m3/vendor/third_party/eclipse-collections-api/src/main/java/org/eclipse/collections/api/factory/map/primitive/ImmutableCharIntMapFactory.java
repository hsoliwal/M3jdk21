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
import org.eclipse.collections.api.block.function.primitive.IntFunction;
import org.eclipse.collections.api.map.primitive.CharIntMap;
import org.eclipse.collections.api.map.primitive.ImmutableCharIntMap;

/**
 * A factory which creates instances of type {@link ImmutableCharIntMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactory.stg.
 *
 * @since 4.0.
 */
public interface ImmutableCharIntMapFactory
{
    /**
     * @since 6.0
     */
    ImmutableCharIntMap empty();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableCharIntMap of();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableCharIntMap with();

    /**
     * Same as {@link #with(char, int)}.
     */
    ImmutableCharIntMap of(char key, int value);

    ImmutableCharIntMap with(char key, int value);

    /**
     * Same as {@link #withAll(CharIntMap)}.
     */
    ImmutableCharIntMap ofAll(CharIntMap map);

    ImmutableCharIntMap withAll(CharIntMap map);

    /**
     * Creates an {@code ImmutableCharIntMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T> ImmutableCharIntMap from(Iterable<T> iterable, CharFunction<? super T> keyFunction, IntFunction<? super T> valueFunction);
}
