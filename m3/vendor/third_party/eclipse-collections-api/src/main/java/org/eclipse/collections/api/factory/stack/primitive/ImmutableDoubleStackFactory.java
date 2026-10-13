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

import org.eclipse.collections.api.DoubleIterable;
import org.eclipse.collections.api.stack.primitive.ImmutableDoubleStack;
import java.util.stream.DoubleStream;

/**
 * A factory which creates instances of type {@link ImmutableDoubleStack}.
 * This file was automatically generated from template file immutablePrimitiveStackFactory.stg.
 *
 * @since 4.0.
 */
public interface ImmutableDoubleStackFactory
{
    /**
     * @since 6.0
     */
    ImmutableDoubleStack empty();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableDoubleStack of();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableDoubleStack with();

    /**
     * Same as {@link #with(double)}.
     */
    ImmutableDoubleStack of(double one);

    ImmutableDoubleStack with(double one);

    /**
     * Same as {@link #with(double[])}.
     */
    ImmutableDoubleStack of(double... items);

    ImmutableDoubleStack with(double... items);

    /**
     * Same as {@link #withAll(DoubleIterable)}.
     */
    ImmutableDoubleStack ofAll(DoubleIterable items);

    ImmutableDoubleStack withAll(DoubleIterable items);

    /**
     * Same as {@link #withAll(Iterable)}.
     */
    ImmutableDoubleStack ofAll(Iterable<Double> iterable);

    ImmutableDoubleStack withAll(Iterable<Double> iterable);

    /**
     * Same as {@link #withAllReversed(DoubleIterable)}.
     */
    ImmutableDoubleStack ofAllReversed(DoubleIterable items);

    ImmutableDoubleStack withAllReversed(DoubleIterable items);

    /**
     * @since 9.0
     */
    ImmutableDoubleStack ofAll(DoubleStream items);

    /**
     * @since 9.0
     */
    ImmutableDoubleStack withAll(DoubleStream items);
}
