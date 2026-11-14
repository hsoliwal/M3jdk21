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
import org.eclipse.collections.api.block.function.primitive.CharFunction;
import org.eclipse.collections.api.map.primitive.ImmutableCharObjectMap;
import org.eclipse.collections.api.map.primitive.CharObjectMap;

/**
 * A factory which creates instances of type {@link ImmutableCharObjectMap}.
 * This file was automatically generated from template file immutablePrimitiveObjectMapFactory.stg.
 *
 * @since 4.0.
 */
public interface ImmutableCharObjectMapFactory
{
    /**
     * @since 6.0
     */
    <V> ImmutableCharObjectMap<V> empty();

    /**
     * Same as {@link #empty()}.
     */
    <V> ImmutableCharObjectMap<V> of();

    /**
     * Same as {@link #empty()}.
     */
    <V> ImmutableCharObjectMap<V> with();

    /**
     * Same as {@link #with(char, Object)}.
     */
    <V> ImmutableCharObjectMap<V> of(char key, V value);

    <V> ImmutableCharObjectMap<V> with(char key, V value);

    /**
     * Same as {@link #withAll(CharObjectMap)}.
     */
    <V> ImmutableCharObjectMap<V> ofAll(CharObjectMap<? extends V> map);

    <V> ImmutableCharObjectMap<V> withAll(CharObjectMap<? extends V> map);

    /**
     * Creates an {@code ImmutableCharObjectMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T, V> ImmutableCharObjectMap<V> from(Iterable<T> iterable, CharFunction<? super T> keyFunction, Function<? super T, ? extends V> valueFunction);
}
