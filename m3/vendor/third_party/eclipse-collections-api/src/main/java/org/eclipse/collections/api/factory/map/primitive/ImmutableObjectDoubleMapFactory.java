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
import org.eclipse.collections.api.map.primitive.ImmutableObjectDoubleMap;
import org.eclipse.collections.api.map.primitive.ObjectDoubleMap;

/**
 * A factory which creates instances of type {@link ImmutableObjectDoubleMap}.
 * This file was automatically generated from template file immutableObjectPrimitiveMapFactory.stg.
 *
 * @since 4.0.
 */
public interface ImmutableObjectDoubleMapFactory
{
    /**
     * @since 6.0
     */
    <K> ImmutableObjectDoubleMap<K> empty();

    /**
     * Same as {@link #empty()}.
     */
    <K> ImmutableObjectDoubleMap<K> of();

    /**
     * Same as {@link #empty()}.
     */
    <K> ImmutableObjectDoubleMap<K> with();

    /**
     * Same as {@link #with(Object, double)}.
     */
    <K> ImmutableObjectDoubleMap<K> of(K key, double value);

    <K> ImmutableObjectDoubleMap<K> with(K key, double value);

    /**
     * Same as {@link #withAll(ObjectDoubleMap)}.
     */
    <K> ImmutableObjectDoubleMap<K> ofAll(ObjectDoubleMap<? extends K> map);

    <K> ImmutableObjectDoubleMap<K> withAll(ObjectDoubleMap<? extends K> map);

    /**
     * Creates an {@code ImmutableObjectDoubleMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T, K> ImmutableObjectDoubleMap<K> from(Iterable<T> iterable, Function<? super T, ? extends K> keyFunction, DoubleFunction<? super T> valueFunction);
}
