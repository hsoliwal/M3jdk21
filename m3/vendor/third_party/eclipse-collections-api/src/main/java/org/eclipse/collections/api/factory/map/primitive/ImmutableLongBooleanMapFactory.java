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
import org.eclipse.collections.api.block.function.primitive.BooleanFunction;
import org.eclipse.collections.api.map.primitive.LongBooleanMap;
import org.eclipse.collections.api.map.primitive.ImmutableLongBooleanMap;

/**
 * A factory which creates instances of type {@link ImmutableLongBooleanMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactory.stg.
 *
 * @since 4.0.
 */
public interface ImmutableLongBooleanMapFactory
{
    /**
     * @since 6.0
     */
    ImmutableLongBooleanMap empty();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableLongBooleanMap of();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableLongBooleanMap with();

    /**
     * Same as {@link #with(long, boolean)}.
     */
    ImmutableLongBooleanMap of(long key, boolean value);

    ImmutableLongBooleanMap with(long key, boolean value);

    /**
     * Same as {@link #withAll(LongBooleanMap)}.
     */
    ImmutableLongBooleanMap ofAll(LongBooleanMap map);

    ImmutableLongBooleanMap withAll(LongBooleanMap map);

    /**
     * Creates an {@code ImmutableLongBooleanMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T> ImmutableLongBooleanMap from(Iterable<T> iterable, LongFunction<? super T> keyFunction, BooleanFunction<? super T> valueFunction);
}
