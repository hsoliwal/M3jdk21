/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.api.factory.bag.primitive;

import org.eclipse.collections.api.FloatIterable;
import org.eclipse.collections.api.bag.primitive.ImmutableFloatBag;

/**
 * A factory which creates instances of type {@link ImmutableFloatBag}.
 * This file was automatically generated from template file immutablePrimitiveBagFactory.stg.
 *
 * @since 4.0.
 */
public interface ImmutableFloatBagFactory
{
    /**
     * @since 6.0
     */
    ImmutableFloatBag empty();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableFloatBag of();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableFloatBag with();

    /**
     * Same as {@link #with(float)}.
     */
    ImmutableFloatBag of(float one);

    ImmutableFloatBag with(float one);

    /**
     * Same as {@link #with(float[])}.
     */
    ImmutableFloatBag of(float... items);

    ImmutableFloatBag with(float... items);

    /**
     * Same as {@link #withAll(FloatIterable)}.
     */
    ImmutableFloatBag ofAll(FloatIterable items);

    ImmutableFloatBag withAll(FloatIterable items);

    /**
     * Same as {@link #withAll(Iterable)}.
     */
    ImmutableFloatBag ofAll(Iterable<Float> iterable);

    ImmutableFloatBag withAll(Iterable<Float> iterable);
}
