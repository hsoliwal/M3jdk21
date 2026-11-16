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

import jdk.m3.collections.eclipse.api.DoubleIterable;
import jdk.m3.collections.eclipse.api.stack.primitive.MutableDoubleStack;
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
