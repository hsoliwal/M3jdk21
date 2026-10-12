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
import org.eclipse.collections.api.block.function.primitive.BooleanFunction;
import org.eclipse.collections.api.map.primitive.IntBooleanMap;
import org.eclipse.collections.api.map.primitive.ImmutableIntBooleanMap;

/**
 * A factory which creates instances of type {@link ImmutableIntBooleanMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactory.stg.
 *
 * @since 4.0.
 */
public interface ImmutableIntBooleanMapFactory
{
    /**
     * @since 6.0
     */
    ImmutableIntBooleanMap empty();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableIntBooleanMap of();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableIntBooleanMap with();

    /**
     * Same as {@link #with(int, boolean)}.
     */
    ImmutableIntBooleanMap of(int key, boolean value);

    ImmutableIntBooleanMap with(int key, boolean value);

    /**
     * Same as {@link #withAll(IntBooleanMap)}.
     */
    ImmutableIntBooleanMap ofAll(IntBooleanMap map);

    ImmutableIntBooleanMap withAll(IntBooleanMap map);

    /**
     * Creates an {@code ImmutableIntBooleanMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T> ImmutableIntBooleanMap from(Iterable<T> iterable, IntFunction<? super T> keyFunction, BooleanFunction<? super T> valueFunction);
}
