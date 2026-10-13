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

import org.eclipse.collections.api.DoubleIterable;
import org.eclipse.collections.api.set.primitive.MutableDoubleSet;
import java.util.stream.DoubleStream;

/**
 * A factory which creates instances of type {@link MutableDoubleSet}.
 * This file was automatically generated from template file mutablePrimitiveSetFactory.stg.
 *
 * @since 6.0.
 */
public interface MutableDoubleSetFactory
{
    MutableDoubleSet empty();

    /**
     * Same as {@link #empty()}.
     */
    MutableDoubleSet of();

    /**
     * Same as {@link #empty()}.
     */
    MutableDoubleSet with();

    /**
     * Same as {@link #empty()}. but takes in an initial capacity
     * @since 10.3
     */
    default MutableDoubleSet withInitialCapacity(int capacity)
    {
        throw new UnsupportedOperationException("Default method to ensure backwards compatibility");
    }

    /**
     * Same as {@link #with(double[])}.
     */
    MutableDoubleSet of(double... items);

    MutableDoubleSet with(double... items);

    /**
     * Same as {@link #withAll(DoubleIterable)}.
     */
    MutableDoubleSet ofAll(DoubleIterable items);

    MutableDoubleSet withAll(DoubleIterable items);

    /**
     * Same as {@link #withAll(Iterable)}.
     */
    MutableDoubleSet ofAll(Iterable<Double> iterable);

    MutableDoubleSet withAll(Iterable<Double> iterable);

    /**
     * @since 9.0
     */
    MutableDoubleSet ofAll(DoubleStream items);

    /**
     * @since 9.0
     */
    MutableDoubleSet withAll(DoubleStream items);
}
