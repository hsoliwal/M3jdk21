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
import org.eclipse.collections.api.set.primitive.ImmutableDoubleSet;
import java.util.stream.DoubleStream;

/**
 * A factory which creates instances of type {@link ImmutableDoubleSet}.
 * This file was automatically generated from template file immutablePrimitiveSetFactory.stg.
 *
 * @since 4.0.
 */
public interface ImmutableDoubleSetFactory
{
    /**
     * @since 6.0
     */
    ImmutableDoubleSet empty();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableDoubleSet of();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableDoubleSet with();

    /**
     * Same as {@link #with(double)}.
     */
    ImmutableDoubleSet of(double one);

    ImmutableDoubleSet with(double one);

    /**
     * Same as {@link #with(double[])}.
     */
    ImmutableDoubleSet of(double... items);

    ImmutableDoubleSet with(double... items);

    /**
     * Same as {@link #withAll(DoubleIterable)}.
     */
    ImmutableDoubleSet ofAll(DoubleIterable items);

    ImmutableDoubleSet withAll(DoubleIterable items);

    /**
     * Same as {@link #withAll(Iterable)}.
     */
    ImmutableDoubleSet ofAll(Iterable<Double> iterable);

    ImmutableDoubleSet withAll(Iterable<Double> iterable);

    /**
     * @since 9.0
     */
    ImmutableDoubleSet ofAll(DoubleStream items);

    /**
     * @since 9.0
     */
    ImmutableDoubleSet withAll(DoubleStream items);
}
