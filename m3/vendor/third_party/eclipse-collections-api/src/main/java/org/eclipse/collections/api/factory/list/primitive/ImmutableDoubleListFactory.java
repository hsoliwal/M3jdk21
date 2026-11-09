/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.api.factory.list.primitive;

import org.eclipse.collections.api.DoubleIterable;
import org.eclipse.collections.api.list.primitive.ImmutableDoubleList;
import java.util.stream.DoubleStream;

/**
 * A factory which creates instances of type {@link ImmutableDoubleList}.
 * This file was automatically generated from template file immutablePrimitiveListFactory.stg.
 *
 * @since 3.2.
 */
public interface ImmutableDoubleListFactory
{
    /**
     * @since 6.0
     */
    ImmutableDoubleList empty();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableDoubleList of();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableDoubleList with();

    /**
     * Same as {@link #with(double)}.
     */
    ImmutableDoubleList of(double one);

    ImmutableDoubleList with(double one);

    /**
     * Same as {@link #with(double[])}.
     */
    ImmutableDoubleList of(double... items);

    ImmutableDoubleList with(double... items);

    /**
     * Same as {@link #withAll(DoubleIterable)}.
     */
    ImmutableDoubleList ofAll(DoubleIterable items);

    ImmutableDoubleList withAll(DoubleIterable items);

    /**
     * Same as {@link #withAll(Iterable)}.
     */
    ImmutableDoubleList ofAll(Iterable<Double> iterable);

    ImmutableDoubleList withAll(Iterable<Double> iterable);

    /**
     * @since 9.0
     */
    ImmutableDoubleList ofAll(DoubleStream items);

    /**
     * @since 9.0
     */
    ImmutableDoubleList withAll(DoubleStream items);
}
