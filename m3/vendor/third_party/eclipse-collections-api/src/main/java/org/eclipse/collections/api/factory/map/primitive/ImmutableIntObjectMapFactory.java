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
import org.eclipse.collections.api.block.function.primitive.IntFunction;
import org.eclipse.collections.api.map.primitive.ImmutableIntObjectMap;
import org.eclipse.collections.api.map.primitive.IntObjectMap;

/**
 * A factory which creates instances of type {@link ImmutableIntObjectMap}.
 * This file was automatically generated from template file immutablePrimitiveObjectMapFactory.stg.
 *
 * @since 4.0.
 */
public interface ImmutableIntObjectMapFactory
{
    /**
     * @since 6.0
     */
    <V> ImmutableIntObjectMap<V> empty();

    /**
     * Same as {@link #empty()}.
     */
    <V> ImmutableIntObjectMap<V> of();

    /**
     * Same as {@link #empty()}.
     */
    <V> ImmutableIntObjectMap<V> with();

    /**
     * Same as {@link #with(int, Object)}.
     */
    <V> ImmutableIntObjectMap<V> of(int key, V value);

    <V> ImmutableIntObjectMap<V> with(int key, V value);

    /**
     * Same as {@link #withAll(IntObjectMap)}.
     */
    <V> ImmutableIntObjectMap<V> ofAll(IntObjectMap<? extends V> map);

    <V> ImmutableIntObjectMap<V> withAll(IntObjectMap<? extends V> map);

    /**
     * Creates an {@code ImmutableIntObjectMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T, V> ImmutableIntObjectMap<V> from(Iterable<T> iterable, IntFunction<? super T> keyFunction, Function<? super T, ? extends V> valueFunction);
}
