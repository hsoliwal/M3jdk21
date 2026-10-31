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
import org.eclipse.collections.api.block.function.primitive.ShortFunction;
import org.eclipse.collections.api.map.primitive.ImmutableShortObjectMap;
import org.eclipse.collections.api.map.primitive.ShortObjectMap;

/**
 * A factory which creates instances of type {@link ImmutableShortObjectMap}.
 * This file was automatically generated from template file immutablePrimitiveObjectMapFactory.stg.
 *
 * @since 4.0.
 */
public interface ImmutableShortObjectMapFactory
{
    /**
     * @since 6.0
     */
    <V> ImmutableShortObjectMap<V> empty();

    /**
     * Same as {@link #empty()}.
     */
    <V> ImmutableShortObjectMap<V> of();

    /**
     * Same as {@link #empty()}.
     */
    <V> ImmutableShortObjectMap<V> with();

    /**
     * Same as {@link #with(short, Object)}.
     */
    <V> ImmutableShortObjectMap<V> of(short key, V value);

    <V> ImmutableShortObjectMap<V> with(short key, V value);

    /**
     * Same as {@link #withAll(ShortObjectMap)}.
     */
    <V> ImmutableShortObjectMap<V> ofAll(ShortObjectMap<? extends V> map);

    <V> ImmutableShortObjectMap<V> withAll(ShortObjectMap<? extends V> map);

    /**
     * Creates an {@code ImmutableShortObjectMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T, V> ImmutableShortObjectMap<V> from(Iterable<T> iterable, ShortFunction<? super T> keyFunction, Function<? super T, ? extends V> valueFunction);
}
