/*
 * Copyright (c) 2026 Goldman Sachs.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.api.factory.map.primitive;

import jdk.m3.collections.eclipse.api.block.function.Function;
import jdk.m3.collections.eclipse.api.block.function.primitive.LongFunction;
import jdk.m3.collections.eclipse.api.map.primitive.ImmutableLongObjectMap;
import jdk.m3.collections.eclipse.api.map.primitive.LongObjectMap;

/**
 * A factory which creates instances of type {@link ImmutableLongObjectMap}.
 * This file was automatically generated from template file immutablePrimitiveObjectMapFactory.stg.
 *
 * @since 4.0.
 */
public interface ImmutableLongObjectMapFactory
{
    /**
     * @since 6.0
     */
    <V> ImmutableLongObjectMap<V> empty();

    /**
     * Same as {@link #empty()}.
     */
    <V> ImmutableLongObjectMap<V> of();

    /**
     * Same as {@link #empty()}.
     */
    <V> ImmutableLongObjectMap<V> with();

    /**
     * Same as {@link #with(long, Object)}.
     */
    <V> ImmutableLongObjectMap<V> of(long key, V value);

    <V> ImmutableLongObjectMap<V> with(long key, V value);

    /**
     * Same as {@link #withAll(LongObjectMap)}.
     */
    <V> ImmutableLongObjectMap<V> ofAll(LongObjectMap<? extends V> map);

    <V> ImmutableLongObjectMap<V> withAll(LongObjectMap<? extends V> map);

    /**
     * Creates an {@code ImmutableLongObjectMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T, V> ImmutableLongObjectMap<V> from(Iterable<T> iterable, LongFunction<? super T> keyFunction, Function<? super T, ? extends V> valueFunction);
}
