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

import org.eclipse.collections.api.block.function.primitive.DoubleFunction;
import org.eclipse.collections.api.block.function.primitive.BooleanFunction;
import org.eclipse.collections.api.map.primitive.DoubleBooleanMap;
import org.eclipse.collections.api.map.primitive.ImmutableDoubleBooleanMap;

/**
 * A factory which creates instances of type {@link ImmutableDoubleBooleanMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactory.stg.
 *
 * @since 4.0.
 */
public interface ImmutableDoubleBooleanMapFactory
{
    /**
     * @since 6.0
     */
    ImmutableDoubleBooleanMap empty();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableDoubleBooleanMap of();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableDoubleBooleanMap with();

    /**
     * Same as {@link #with(double, boolean)}.
     */
    ImmutableDoubleBooleanMap of(double key, boolean value);

    ImmutableDoubleBooleanMap with(double key, boolean value);

    /**
     * Same as {@link #withAll(DoubleBooleanMap)}.
     */
    ImmutableDoubleBooleanMap ofAll(DoubleBooleanMap map);

    ImmutableDoubleBooleanMap withAll(DoubleBooleanMap map);

    /**
     * Creates an {@code ImmutableDoubleBooleanMap} from an {@code Iterable<T>} by applying {@code keyFunction} and {@code valueFunction}.
     *
     * @since 10.0
     */
    <T> ImmutableDoubleBooleanMap from(Iterable<T> iterable, DoubleFunction<? super T> keyFunction, BooleanFunction<? super T> valueFunction);
}
