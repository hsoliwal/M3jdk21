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

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.primitive.DoubleFunction;
import org.eclipse.collections.api.map.primitive.ImmutableDoubleObjectMap;
import org.eclipse.collections.api.map.primitive.DoubleObjectMap;

/**
 * A factory which creates instances of type {@link ImmutableDoubleObjectMap}.
 * This file was automatically generated from template file immutablePrimitiveObjectMapFactory.stg.
 *
 * @since 4.0.
 */
public interface ImmutableDoubleObjectMapFactory
{
    /**
     * @since 6.0
     */
    <V> ImmutableDoubleObjectMap<V> empty();

    /**
     * Same as {@link #empty()}.
     */
    <V> ImmutableDoubleObjectMap<V> of();

    /**
     * Same as {@link #empty()}.
     */
    <V> ImmutableDoubleObjectMap<V> with();

    /**
     * Same as {@link #with(double, Object)}.
     */
    <V> ImmutableDoubleObjectMap<V> of(double key, V value);

    <V> ImmutableDoubleObjectMap<V> with(double key, V value);

    /**
     * Same as {@link #withAll(DoubleObjectMap)}.
     */
    <V> ImmutableDoubleObjectMap<V> ofAll(DoubleObjectMap<? extends V> map);

    <V> ImmutableDoubleObjectMap<V> withAll(DoubleObjectMap<? extends V> map);

    /**
     * Creates an {@code ImmutableDoubleObjectMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T, V> ImmutableDoubleObjectMap<V> from(Iterable<T> iterable, DoubleFunction<? super T> keyFunction, Function<? super T, ? extends V> valueFunction);
}
