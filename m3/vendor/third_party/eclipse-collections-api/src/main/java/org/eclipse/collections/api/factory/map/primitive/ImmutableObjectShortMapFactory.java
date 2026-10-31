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
import org.eclipse.collections.api.map.primitive.ImmutableObjectShortMap;
import org.eclipse.collections.api.map.primitive.ObjectShortMap;

/**
 * A factory which creates instances of type {@link ImmutableObjectShortMap}.
 * This file was automatically generated from template file immutableObjectPrimitiveMapFactory.stg.
 *
 * @since 4.0.
 */
public interface ImmutableObjectShortMapFactory
{
    /**
     * @since 6.0
     */
    <K> ImmutableObjectShortMap<K> empty();

    /**
     * Same as {@link #empty()}.
     */
    <K> ImmutableObjectShortMap<K> of();

    /**
     * Same as {@link #empty()}.
     */
    <K> ImmutableObjectShortMap<K> with();

    /**
     * Same as {@link #with(Object, short)}.
     */
    <K> ImmutableObjectShortMap<K> of(K key, short value);

    <K> ImmutableObjectShortMap<K> with(K key, short value);

    /**
     * Same as {@link #withAll(ObjectShortMap)}.
     */
    <K> ImmutableObjectShortMap<K> ofAll(ObjectShortMap<? extends K> map);

    <K> ImmutableObjectShortMap<K> withAll(ObjectShortMap<? extends K> map);

    /**
     * Creates an {@code ImmutableObjectShortMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T, K> ImmutableObjectShortMap<K> from(Iterable<T> iterable, Function<? super T, ? extends K> keyFunction, ShortFunction<? super T> valueFunction);
}
