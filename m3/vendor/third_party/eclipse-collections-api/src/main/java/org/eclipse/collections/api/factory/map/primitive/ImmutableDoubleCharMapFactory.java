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
import org.eclipse.collections.api.block.function.primitive.CharFunction;
import org.eclipse.collections.api.map.primitive.DoubleCharMap;
import org.eclipse.collections.api.map.primitive.ImmutableDoubleCharMap;

/**
 * A factory which creates instances of type {@link ImmutableDoubleCharMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactory.stg.
 *
 * @since 4.0.
 */
public interface ImmutableDoubleCharMapFactory
{
    /**
     * @since 6.0
     */
    ImmutableDoubleCharMap empty();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableDoubleCharMap of();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableDoubleCharMap with();

    /**
     * Same as {@link #with(double, char)}.
     */
    ImmutableDoubleCharMap of(double key, char value);

    ImmutableDoubleCharMap with(double key, char value);

    /**
     * Same as {@link #withAll(DoubleCharMap)}.
     */
    ImmutableDoubleCharMap ofAll(DoubleCharMap map);

    ImmutableDoubleCharMap withAll(DoubleCharMap map);

    /**
     * Creates an {@code ImmutableDoubleCharMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T> ImmutableDoubleCharMap from(Iterable<T> iterable, DoubleFunction<? super T> keyFunction, CharFunction<? super T> valueFunction);
}
