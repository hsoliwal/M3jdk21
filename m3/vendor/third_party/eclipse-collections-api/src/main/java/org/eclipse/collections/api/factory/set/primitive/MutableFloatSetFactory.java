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
import org.eclipse.collections.api.set.primitive.MutableFloatSet;

/**
 * A factory which creates instances of type {@link MutableFloatSet}.
 * This file was automatically generated from template file mutablePrimitiveSetFactory.stg.
 *
 * @since 6.0.
 */
public interface MutableFloatSetFactory
{
    MutableFloatSet empty();

    /**
     * Same as {@link #empty()}.
     */
    MutableFloatSet of();

    /**
     * Same as {@link #empty()}.
     */
    MutableFloatSet with();

    /**
     * Same as {@link #empty()}. but takes in an initial capacity
     * @since 10.3
     */
    default MutableFloatSet withInitialCapacity(int capacity)
    {
        throw new UnsupportedOperationException("Default method to ensure backwards compatibility");
    }

    /**
     * Same as {@link #with(float[])}.
     */
    MutableFloatSet of(float... items);

    MutableFloatSet with(float... items);

    /**
     * Same as {@link #withAll(FloatIterable)}.
     */
    MutableFloatSet ofAll(FloatIterable items);

    MutableFloatSet withAll(FloatIterable items);

    /**
     * Same as {@link #withAll(Iterable)}.
     */
    MutableFloatSet ofAll(Iterable<Float> iterable);

    MutableFloatSet withAll(Iterable<Float> iterable);
}
