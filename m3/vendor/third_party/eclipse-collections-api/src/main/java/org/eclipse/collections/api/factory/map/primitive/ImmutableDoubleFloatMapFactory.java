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
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.map.primitive.DoubleFloatMap;
import org.eclipse.collections.api.map.primitive.ImmutableDoubleFloatMap;

/**
 * A factory which creates instances of type {@link ImmutableDoubleFloatMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactory.stg.
 *
 * @since 4.0.
 */
public interface ImmutableDoubleFloatMapFactory
{
    /**
     * @since 6.0
     */
    ImmutableDoubleFloatMap empty();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableDoubleFloatMap of();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableDoubleFloatMap with();

    /**
     * Same as {@link #with(double, float)}.
     */
    ImmutableDoubleFloatMap of(double key, float value);

    ImmutableDoubleFloatMap with(double key, float value);

    /**
     * Same as {@link #withAll(DoubleFloatMap)}.
     */
    ImmutableDoubleFloatMap ofAll(DoubleFloatMap map);

    ImmutableDoubleFloatMap withAll(DoubleFloatMap map);

    /**
     * Creates an {@code ImmutableDoubleFloatMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T> ImmutableDoubleFloatMap from(Iterable<T> iterable, DoubleFunction<? super T> keyFunction, FloatFunction<? super T> valueFunction);
}
