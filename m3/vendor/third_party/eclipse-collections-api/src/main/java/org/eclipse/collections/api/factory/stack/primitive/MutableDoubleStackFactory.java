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
import org.eclipse.collections.api.stack.primitive.MutableDoubleStack;
import java.util.stream.DoubleStream;

/**
 * A factory which creates instances of type {@link MutableDoubleStack}.
 * This file was automatically generated from template file mutablePrimitiveStackFactory.stg.
 *
 * @since 6.0.
 */
public interface MutableDoubleStackFactory
{
    MutableDoubleStack empty();

    /**
     * Same as {@link #empty()}.
     */
    MutableDoubleStack of();

    /**
     * Same as {@link #empty()}.
     */
    MutableDoubleStack with();

    /**
     * Same as {@link #with(double[])}.
     */
    MutableDoubleStack of(double... items);

    MutableDoubleStack with(double... items);

    /**
     * Same as {@link #withAll(DoubleIterable)}.
     */
    MutableDoubleStack ofAll(DoubleIterable items);

    MutableDoubleStack withAll(DoubleIterable items);

    /**
     * Same as {@link #withAll(Iterable)}.
     */
    MutableDoubleStack ofAll(Iterable<Double> iterable);

    MutableDoubleStack withAll(Iterable<Double> iterable);

    /**
     * Same as {@link #withAllReversed(DoubleIterable)}.
     */
    MutableDoubleStack ofAllReversed(DoubleIterable items);

    MutableDoubleStack withAllReversed(DoubleIterable items);

    /**
     * @since 9.0
     */
    MutableDoubleStack ofAll(DoubleStream items);

    /**
     * @since 9.0
     */
    MutableDoubleStack withAll(DoubleStream items);
}
