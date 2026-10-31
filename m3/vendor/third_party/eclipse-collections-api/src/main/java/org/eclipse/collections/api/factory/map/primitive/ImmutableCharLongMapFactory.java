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
import org.eclipse.collections.api.block.function.primitive.LongFunction;
import org.eclipse.collections.api.map.primitive.CharLongMap;
import org.eclipse.collections.api.map.primitive.ImmutableCharLongMap;

/**
 * A factory which creates instances of type {@link ImmutableCharLongMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactory.stg.
 *
 * @since 4.0.
 */
public interface ImmutableCharLongMapFactory
{
    /**
     * @since 6.0
     */
    ImmutableCharLongMap empty();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableCharLongMap of();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableCharLongMap with();

    /**
     * Same as {@link #with(char, long)}.
     */
    ImmutableCharLongMap of(char key, long value);

    ImmutableCharLongMap with(char key, long value);

    /**
     * Same as {@link #withAll(CharLongMap)}.
     */
    ImmutableCharLongMap ofAll(CharLongMap map);

    ImmutableCharLongMap withAll(CharLongMap map);

    /**
     * Creates an {@code ImmutableCharLongMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T> ImmutableCharLongMap from(Iterable<T> iterable, CharFunction<? super T> keyFunction, LongFunction<? super T> valueFunction);
}
