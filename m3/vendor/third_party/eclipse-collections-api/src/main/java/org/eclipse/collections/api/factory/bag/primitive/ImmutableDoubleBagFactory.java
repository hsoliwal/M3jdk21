/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.api.factory.bag.primitive;

import org.eclipse.collections.api.DoubleIterable;
import org.eclipse.collections.api.bag.primitive.ImmutableDoubleBag;
import java.util.stream.DoubleStream;

/**
 * A factory which creates instances of type {@link ImmutableDoubleBag}.
 * This file was automatically generated from template file immutablePrimitiveBagFactory.stg.
 *
 * @since 4.0.
 */
public interface ImmutableDoubleBagFactory
{
    /**
     * @since 6.0
     */
    ImmutableDoubleBag empty();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableDoubleBag of();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableDoubleBag with();

    /**
     * Same as {@link #with(double)}.
     */
    ImmutableDoubleBag of(double one);

    ImmutableDoubleBag with(double one);

    /**
     * Same as {@link #with(double[])}.
     */
    ImmutableDoubleBag of(double... items);

    ImmutableDoubleBag with(double... items);

    /**
     * Same as {@link #withAll(DoubleIterable)}.
     */
    ImmutableDoubleBag ofAll(DoubleIterable items);

    ImmutableDoubleBag withAll(DoubleIterable items);

    /**
     * Same as {@link #withAll(Iterable)}.
     */
    ImmutableDoubleBag ofAll(Iterable<Double> iterable);

    ImmutableDoubleBag withAll(Iterable<Double> iterable);

    /**
     * @since 9.0
     */
    ImmutableDoubleBag ofAll(DoubleStream items);

    /**
     * @since 9.0
     */
    ImmutableDoubleBag withAll(DoubleStream items);
}
