/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.api.factory.stack.primitive;

import org.eclipse.collections.api.FloatIterable;
import org.eclipse.collections.api.stack.primitive.ImmutableFloatStack;

/**
 * A factory which creates instances of type {@link ImmutableFloatStack}.
 * This file was automatically generated from template file immutablePrimitiveStackFactory.stg.
 *
 * @since 4.0.
 */
public interface ImmutableFloatStackFactory
{
    /**
     * @since 6.0
     */
    ImmutableFloatStack empty();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableFloatStack of();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableFloatStack with();

    /**
     * Same as {@link #with(float)}.
     */
    ImmutableFloatStack of(float one);

    ImmutableFloatStack with(float one);

    /**
     * Same as {@link #with(float[])}.
     */
    ImmutableFloatStack of(float... items);

    ImmutableFloatStack with(float... items);

    /**
     * Same as {@link #withAll(FloatIterable)}.
     */
    ImmutableFloatStack ofAll(FloatIterable items);

    ImmutableFloatStack withAll(FloatIterable items);

    /**
     * Same as {@link #withAll(Iterable)}.
     */
    ImmutableFloatStack ofAll(Iterable<Float> iterable);

    ImmutableFloatStack withAll(Iterable<Float> iterable);

    /**
     * Same as {@link #withAllReversed(FloatIterable)}.
     */
    ImmutableFloatStack ofAllReversed(FloatIterable items);

    ImmutableFloatStack withAllReversed(FloatIterable items);
}
