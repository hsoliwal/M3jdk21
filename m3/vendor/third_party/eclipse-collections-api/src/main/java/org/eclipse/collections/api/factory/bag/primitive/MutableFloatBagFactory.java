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
import org.eclipse.collections.api.bag.primitive.MutableFloatBag;

/**
 * A factory which creates instances of type {@link MutableFloatBag}.
 * This file was automatically generated from template file mutablePrimitiveBagFactory.stg.
 *
 * @since 6.0.
 */
public interface MutableFloatBagFactory
{
    MutableFloatBag empty();

    /**
     * Same as {@link #empty()}.
     */
    MutableFloatBag of();

    /**
     * Same as {@link #empty()}.
     */
    MutableFloatBag with();

    /**
     * Same as {@link #with(float[])}.
     */
    MutableFloatBag of(float... items);

    MutableFloatBag with(float... items);

    /**
     * Same as {@link #withAll(FloatIterable)}.
     */
    MutableFloatBag ofAll(FloatIterable items);

    MutableFloatBag withAll(FloatIterable items);

    /**
     * Same as {@link #withAll(Iterable)}.
     */
    MutableFloatBag ofAll(Iterable<Float> iterable);

    MutableFloatBag withAll(Iterable<Float> iterable);
}
