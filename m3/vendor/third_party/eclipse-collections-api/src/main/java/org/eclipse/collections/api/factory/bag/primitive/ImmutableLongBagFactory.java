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

import org.eclipse.collections.api.LongIterable;
import org.eclipse.collections.api.bag.primitive.ImmutableLongBag;
import java.util.stream.LongStream;

/**
 * A factory which creates instances of type {@link ImmutableLongBag}.
 * This file was automatically generated from template file immutablePrimitiveBagFactory.stg.
 *
 * @since 4.0.
 */
public interface ImmutableLongBagFactory
{
    /**
     * @since 6.0
     */
    ImmutableLongBag empty();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableLongBag of();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableLongBag with();

    /**
     * Same as {@link #with(long)}.
     */
    ImmutableLongBag of(long one);

    ImmutableLongBag with(long one);

    /**
     * Same as {@link #with(long[])}.
     */
    ImmutableLongBag of(long... items);

    ImmutableLongBag with(long... items);

    /**
     * Same as {@link #withAll(LongIterable)}.
     */
    ImmutableLongBag ofAll(LongIterable items);

    ImmutableLongBag withAll(LongIterable items);

    /**
     * Same as {@link #withAll(Iterable)}.
     */
    ImmutableLongBag ofAll(Iterable<Long> iterable);

    ImmutableLongBag withAll(Iterable<Long> iterable);

    /**
     * @since 9.0
     */
    ImmutableLongBag ofAll(LongStream items);

    /**
     * @since 9.0
     */
    ImmutableLongBag withAll(LongStream items);
}
