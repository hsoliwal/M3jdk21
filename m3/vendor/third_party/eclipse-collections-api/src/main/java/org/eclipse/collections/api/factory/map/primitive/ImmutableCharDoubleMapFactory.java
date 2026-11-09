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
import org.eclipse.collections.api.block.function.primitive.DoubleFunction;
import org.eclipse.collections.api.map.primitive.CharDoubleMap;
import org.eclipse.collections.api.map.primitive.ImmutableCharDoubleMap;

/**
 * A factory which creates instances of type {@link ImmutableCharDoubleMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactory.stg.
 *
 * @since 4.0.
 */
public interface ImmutableCharDoubleMapFactory
{
    /**
     * @since 6.0
     */
    ImmutableCharDoubleMap empty();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableCharDoubleMap of();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableCharDoubleMap with();

    /**
     * Same as {@link #with(char, double)}.
     */
    ImmutableCharDoubleMap of(char key, double value);

    ImmutableCharDoubleMap with(char key, double value);

    /**
     * Same as {@link #withAll(CharDoubleMap)}.
     */
    ImmutableCharDoubleMap ofAll(CharDoubleMap map);

    ImmutableCharDoubleMap withAll(CharDoubleMap map);

    /**
     * Creates an {@code ImmutableCharDoubleMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T> ImmutableCharDoubleMap from(Iterable<T> iterable, CharFunction<? super T> keyFunction, DoubleFunction<? super T> valueFunction);
}
