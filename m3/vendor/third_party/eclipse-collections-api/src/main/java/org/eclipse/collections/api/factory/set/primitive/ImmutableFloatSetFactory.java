/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.api.factory.set.primitive;

import org.eclipse.collections.api.FloatIterable;
import org.eclipse.collections.api.set.primitive.ImmutableFloatSet;

/**
 * A factory which creates instances of type {@link ImmutableFloatSet}.
 * This file was automatically generated from template file immutablePrimitiveSetFactory.stg.
 *
 * @since 4.0.
 */
public interface ImmutableFloatSetFactory
{
    /**
     * @since 6.0
     */
    ImmutableFloatSet empty();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableFloatSet of();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableFloatSet with();

    /**
     * Same as {@link #with(float)}.
     */
    ImmutableFloatSet of(float one);

    ImmutableFloatSet with(float one);

    /**
     * Same as {@link #with(float[])}.
     */
    ImmutableFloatSet of(float... items);

    ImmutableFloatSet with(float... items);

    /**
     * Same as {@link #withAll(FloatIterable)}.
     */
    ImmutableFloatSet ofAll(FloatIterable items);

    ImmutableFloatSet withAll(FloatIterable items);

    /**
     * Same as {@link #withAll(Iterable)}.
     */
    ImmutableFloatSet ofAll(Iterable<Float> iterable);

    ImmutableFloatSet withAll(Iterable<Float> iterable);
}
