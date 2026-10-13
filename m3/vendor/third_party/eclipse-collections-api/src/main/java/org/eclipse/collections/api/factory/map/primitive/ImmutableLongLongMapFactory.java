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

import org.eclipse.collections.api.block.function.primitive.LongFunction;
import org.eclipse.collections.api.map.primitive.LongLongMap;
import org.eclipse.collections.api.map.primitive.ImmutableLongLongMap;

/**
 * A factory which creates instances of type {@link ImmutableLongLongMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactory.stg.
 *
 * @since 4.0.
 */
public interface ImmutableLongLongMapFactory
{
    /**
     * @since 6.0
     */
    ImmutableLongLongMap empty();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableLongLongMap of();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableLongLongMap with();

    /**
     * Same as {@link #with(long, long)}.
     */
    ImmutableLongLongMap of(long key, long value);

    ImmutableLongLongMap with(long key, long value);

    /**
     * Same as {@link #withAll(LongLongMap)}.
     */
    ImmutableLongLongMap ofAll(LongLongMap map);

    ImmutableLongLongMap withAll(LongLongMap map);

    /**
     * Creates an {@code ImmutableLongLongMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T> ImmutableLongLongMap from(Iterable<T> iterable, LongFunction<? super T> keyFunction, LongFunction<? super T> valueFunction);
}
