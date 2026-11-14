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
import org.eclipse.collections.api.map.primitive.CharCharMap;
import org.eclipse.collections.api.map.primitive.ImmutableCharCharMap;

/**
 * A factory which creates instances of type {@link ImmutableCharCharMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactory.stg.
 *
 * @since 4.0.
 */
public interface ImmutableCharCharMapFactory
{
    /**
     * @since 6.0
     */
    ImmutableCharCharMap empty();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableCharCharMap of();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableCharCharMap with();

    /**
     * Same as {@link #with(char, char)}.
     */
    ImmutableCharCharMap of(char key, char value);

    ImmutableCharCharMap with(char key, char value);

    /**
     * Same as {@link #withAll(CharCharMap)}.
     */
    ImmutableCharCharMap ofAll(CharCharMap map);

    ImmutableCharCharMap withAll(CharCharMap map);

    /**
     * Creates an {@code ImmutableCharCharMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T> ImmutableCharCharMap from(Iterable<T> iterable, CharFunction<? super T> keyFunction, CharFunction<? super T> valueFunction);
}
