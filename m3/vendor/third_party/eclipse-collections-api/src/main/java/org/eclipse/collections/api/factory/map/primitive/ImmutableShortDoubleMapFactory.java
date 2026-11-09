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
import org.eclipse.collections.api.block.function.primitive.DoubleFunction;
import org.eclipse.collections.api.map.primitive.ShortDoubleMap;
import org.eclipse.collections.api.map.primitive.ImmutableShortDoubleMap;

/**
 * A factory which creates instances of type {@link ImmutableShortDoubleMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactory.stg.
 *
 * @since 4.0.
 */
public interface ImmutableShortDoubleMapFactory
{
    /**
     * @since 6.0
     */
    ImmutableShortDoubleMap empty();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableShortDoubleMap of();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableShortDoubleMap with();

    /**
     * Same as {@link #with(short, double)}.
     */
    ImmutableShortDoubleMap of(short key, double value);

    ImmutableShortDoubleMap with(short key, double value);

    /**
     * Same as {@link #withAll(ShortDoubleMap)}.
     */
    ImmutableShortDoubleMap ofAll(ShortDoubleMap map);

    ImmutableShortDoubleMap withAll(ShortDoubleMap map);

    /**
     * Creates an {@code ImmutableShortDoubleMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T> ImmutableShortDoubleMap from(Iterable<T> iterable, ShortFunction<? super T> keyFunction, DoubleFunction<? super T> valueFunction);
}
