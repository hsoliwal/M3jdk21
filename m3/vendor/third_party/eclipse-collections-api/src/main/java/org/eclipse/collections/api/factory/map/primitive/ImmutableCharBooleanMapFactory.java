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
import org.eclipse.collections.api.block.function.primitive.BooleanFunction;
import org.eclipse.collections.api.map.primitive.CharBooleanMap;
import org.eclipse.collections.api.map.primitive.ImmutableCharBooleanMap;

/**
 * A factory which creates instances of type {@link ImmutableCharBooleanMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactory.stg.
 *
 * @since 4.0.
 */
public interface ImmutableCharBooleanMapFactory
{
    /**
     * @since 6.0
     */
    ImmutableCharBooleanMap empty();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableCharBooleanMap of();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableCharBooleanMap with();

    /**
     * Same as {@link #with(char, boolean)}.
     */
    ImmutableCharBooleanMap of(char key, boolean value);

    ImmutableCharBooleanMap with(char key, boolean value);

    /**
     * Same as {@link #withAll(CharBooleanMap)}.
     */
    ImmutableCharBooleanMap ofAll(CharBooleanMap map);

    ImmutableCharBooleanMap withAll(CharBooleanMap map);

    /**
     * Creates an {@code ImmutableCharBooleanMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T> ImmutableCharBooleanMap from(Iterable<T> iterable, CharFunction<? super T> keyFunction, BooleanFunction<? super T> valueFunction);
}
