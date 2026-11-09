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
import org.eclipse.collections.api.block.function.primitive.BooleanFunction;
import org.eclipse.collections.api.map.primitive.ImmutableObjectBooleanMap;
import org.eclipse.collections.api.map.primitive.ObjectBooleanMap;

/**
 * A factory which creates instances of type {@link ImmutableObjectBooleanMap}.
 * This file was automatically generated from template file immutableObjectPrimitiveMapFactory.stg.
 *
 * @since 4.0.
 */
public interface ImmutableObjectBooleanMapFactory
{
    /**
     * @since 6.0
     */
    <K> ImmutableObjectBooleanMap<K> empty();

    /**
     * Same as {@link #empty()}.
     */
    <K> ImmutableObjectBooleanMap<K> of();

    /**
     * Same as {@link #empty()}.
     */
    <K> ImmutableObjectBooleanMap<K> with();

    /**
     * Same as {@link #with(Object, boolean)}.
     */
    <K> ImmutableObjectBooleanMap<K> of(K key, boolean value);

    <K> ImmutableObjectBooleanMap<K> with(K key, boolean value);

    /**
     * Same as {@link #withAll(ObjectBooleanMap)}.
     */
    <K> ImmutableObjectBooleanMap<K> ofAll(ObjectBooleanMap<? extends K> map);

    <K> ImmutableObjectBooleanMap<K> withAll(ObjectBooleanMap<? extends K> map);

    /**
     * Creates an {@code ImmutableObjectBooleanMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T, K> ImmutableObjectBooleanMap<K> from(Iterable<T> iterable, Function<? super T, ? extends K> keyFunction, BooleanFunction<? super T> valueFunction);
}
