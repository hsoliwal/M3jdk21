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
import org.eclipse.collections.api.block.function.primitive.IntFunction;
import org.eclipse.collections.api.map.primitive.DoubleIntMap;
import org.eclipse.collections.api.map.primitive.ImmutableDoubleIntMap;

/**
 * A factory which creates instances of type {@link ImmutableDoubleIntMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactory.stg.
 *
 * @since 4.0.
 */
public interface ImmutableDoubleIntMapFactory
{
    /**
     * @since 6.0
     */
    ImmutableDoubleIntMap empty();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableDoubleIntMap of();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableDoubleIntMap with();

    /**
     * Same as {@link #with(double, int)}.
     */
    ImmutableDoubleIntMap of(double key, int value);

    ImmutableDoubleIntMap with(double key, int value);

    /**
     * Same as {@link #withAll(DoubleIntMap)}.
     */
    ImmutableDoubleIntMap ofAll(DoubleIntMap map);

    ImmutableDoubleIntMap withAll(DoubleIntMap map);

    /**
     * Creates an {@code ImmutableDoubleIntMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T> ImmutableDoubleIntMap from(Iterable<T> iterable, DoubleFunction<? super T> keyFunction, IntFunction<? super T> valueFunction);
}
