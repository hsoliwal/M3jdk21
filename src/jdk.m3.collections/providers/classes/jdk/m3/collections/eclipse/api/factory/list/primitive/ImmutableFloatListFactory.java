/*
 * Copyright (c) 2026 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.api.factory.list.primitive;

import jdk.m3.collections.eclipse.api.FloatIterable;
import jdk.m3.collections.eclipse.api.list.primitive.ImmutableFloatList;

/**
 * A factory which creates instances of type {@link ImmutableFloatList}.
 * This file was automatically generated from template file immutablePrimitiveListFactory.stg.
 *
 * @since 3.2.
 */
public interface ImmutableFloatListFactory
{
    /**
     * @since 6.0
     */
    ImmutableFloatList empty();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableFloatList of();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableFloatList with();

    /**
     * Same as {@link #with(float)}.
     */
    ImmutableFloatList of(float one);

    ImmutableFloatList with(float one);

    /**
     * Same as {@link #with(float[])}.
     */
    ImmutableFloatList of(float... items);

    ImmutableFloatList with(float... items);

    /**
     * Same as {@link #withAll(FloatIterable)}.
     */
    ImmutableFloatList ofAll(FloatIterable items);

    ImmutableFloatList withAll(FloatIterable items);

    /**
     * Same as {@link #withAll(Iterable)}.
     */
    ImmutableFloatList ofAll(Iterable<Float> iterable);

    ImmutableFloatList withAll(Iterable<Float> iterable);
}
