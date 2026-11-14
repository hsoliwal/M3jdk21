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
import org.eclipse.collections.api.map.primitive.DoubleDoubleMap;
import org.eclipse.collections.api.map.primitive.ImmutableDoubleDoubleMap;

/**
 * A factory which creates instances of type {@link ImmutableDoubleDoubleMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactory.stg.
 *
 * @since 4.0.
 */
public interface ImmutableDoubleDoubleMapFactory
{
    /**
     * @since 6.0
     */
    ImmutableDoubleDoubleMap empty();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableDoubleDoubleMap of();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableDoubleDoubleMap with();

    /**
     * Same as {@link #with(double, double)}.
     */
    ImmutableDoubleDoubleMap of(double key, double value);

    ImmutableDoubleDoubleMap with(double key, double value);

    /**
     * Same as {@link #withAll(DoubleDoubleMap)}.
     */
    ImmutableDoubleDoubleMap ofAll(DoubleDoubleMap map);

    ImmutableDoubleDoubleMap withAll(DoubleDoubleMap map);

    /**
     * Creates an {@code ImmutableDoubleDoubleMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T> ImmutableDoubleDoubleMap from(Iterable<T> iterable, DoubleFunction<? super T> keyFunction, DoubleFunction<? super T> valueFunction);
}
