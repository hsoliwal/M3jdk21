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
import org.eclipse.collections.api.map.primitive.ImmutableObjectCharMap;
import org.eclipse.collections.api.map.primitive.ObjectCharMap;

/**
 * A factory which creates instances of type {@link ImmutableObjectCharMap}.
 * This file was automatically generated from template file immutableObjectPrimitiveMapFactory.stg.
 *
 * @since 4.0.
 */
public interface ImmutableObjectCharMapFactory
{
    /**
     * @since 6.0
     */
    <K> ImmutableObjectCharMap<K> empty();

    /**
     * Same as {@link #empty()}.
     */
    <K> ImmutableObjectCharMap<K> of();

    /**
     * Same as {@link #empty()}.
     */
    <K> ImmutableObjectCharMap<K> with();

    /**
     * Same as {@link #with(Object, char)}.
     */
    <K> ImmutableObjectCharMap<K> of(K key, char value);

    <K> ImmutableObjectCharMap<K> with(K key, char value);

    /**
     * Same as {@link #withAll(ObjectCharMap)}.
     */
    <K> ImmutableObjectCharMap<K> ofAll(ObjectCharMap<? extends K> map);

    <K> ImmutableObjectCharMap<K> withAll(ObjectCharMap<? extends K> map);

    /**
     * Creates an {@code ImmutableObjectCharMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T, K> ImmutableObjectCharMap<K> from(Iterable<T> iterable, Function<? super T, ? extends K> keyFunction, CharFunction<? super T> valueFunction);
}
