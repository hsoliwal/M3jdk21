/*
 * Copyright (c) 2026 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.api.factory.stack.primitive;

import jdk.m3.collections.eclipse.api.FloatIterable;
import jdk.m3.collections.eclipse.api.stack.primitive.MutableFloatStack;

/**
 * A factory which creates instances of type {@link MutableFloatStack}.
 * This file was automatically generated from template file mutablePrimitiveStackFactory.stg.
 *
 * @since 6.0.
 */
public interface MutableFloatStackFactory
{
    MutableFloatStack empty();

    /**
     * Same as {@link #empty()}.
     */
    MutableFloatStack of();

    /**
     * Same as {@link #empty()}.
     */
    MutableFloatStack with();

    /**
     * Same as {@link #with(float[])}.
     */
    MutableFloatStack of(float... items);

    MutableFloatStack with(float... items);

    /**
     * Same as {@link #withAll(FloatIterable)}.
     */
    MutableFloatStack ofAll(FloatIterable items);

    MutableFloatStack withAll(FloatIterable items);

    /**
     * Same as {@link #withAll(Iterable)}.
     */
    MutableFloatStack ofAll(Iterable<Float> iterable);

    MutableFloatStack withAll(Iterable<Float> iterable);

    /**
     * Same as {@link #withAllReversed(FloatIterable)}.
     */
    MutableFloatStack ofAllReversed(FloatIterable items);

    MutableFloatStack withAllReversed(FloatIterable items);
}
