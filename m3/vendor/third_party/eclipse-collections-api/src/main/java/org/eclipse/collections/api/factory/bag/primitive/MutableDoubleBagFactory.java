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
import org.eclipse.collections.api.bag.primitive.MutableDoubleBag;
import java.util.stream.DoubleStream;

/**
 * A factory which creates instances of type {@link MutableDoubleBag}.
 * This file was automatically generated from template file mutablePrimitiveBagFactory.stg.
 *
 * @since 6.0.
 */
public interface MutableDoubleBagFactory
{
    MutableDoubleBag empty();

    /**
     * Same as {@link #empty()}.
     */
    MutableDoubleBag of();

    /**
     * Same as {@link #empty()}.
     */
    MutableDoubleBag with();

    /**
     * Same as {@link #with(double[])}.
     */
    MutableDoubleBag of(double... items);

    MutableDoubleBag with(double... items);

    /**
     * Same as {@link #withAll(DoubleIterable)}.
     */
    MutableDoubleBag ofAll(DoubleIterable items);

    MutableDoubleBag withAll(DoubleIterable items);

    /**
     * Same as {@link #withAll(Iterable)}.
     */
    MutableDoubleBag ofAll(Iterable<Double> iterable);

    MutableDoubleBag withAll(Iterable<Double> iterable);

    /**
     * @since 9.0
     */
    MutableDoubleBag ofAll(DoubleStream items);

    /**
     * @since 9.0
     */
    MutableDoubleBag withAll(DoubleStream items);
}
