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

import org.eclipse.collections.api.block.function.primitive.DoubleFunction;
import org.eclipse.collections.api.block.function.primitive.ShortFunction;
import org.eclipse.collections.api.map.primitive.DoubleShortMap;
import org.eclipse.collections.api.map.primitive.ImmutableDoubleShortMap;

/**
 * A factory which creates instances of type {@link ImmutableDoubleShortMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactory.stg.
 *
 * @since 4.0.
 */
public interface ImmutableDoubleShortMapFactory
{
    /**
     * @since 6.0
     */
    ImmutableDoubleShortMap empty();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableDoubleShortMap of();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableDoubleShortMap with();

    /**
     * Same as {@link #with(double, short)}.
     */
    ImmutableDoubleShortMap of(double key, short value);

    ImmutableDoubleShortMap with(double key, short value);

    /**
     * Same as {@link #withAll(DoubleShortMap)}.
     */
    ImmutableDoubleShortMap ofAll(DoubleShortMap map);

    ImmutableDoubleShortMap withAll(DoubleShortMap map);

    /**
     * Creates an {@code ImmutableDoubleShortMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T> ImmutableDoubleShortMap from(Iterable<T> iterable, DoubleFunction<? super T> keyFunction, ShortFunction<? super T> valueFunction);
}
