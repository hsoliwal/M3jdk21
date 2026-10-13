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
import org.eclipse.collections.api.bag.primitive.MutableLongBag;
import java.util.stream.LongStream;

/**
 * A factory which creates instances of type {@link MutableLongBag}.
 * This file was automatically generated from template file mutablePrimitiveBagFactory.stg.
 *
 * @since 6.0.
 */
public interface MutableLongBagFactory
{
    MutableLongBag empty();

    /**
     * Same as {@link #empty()}.
     */
    MutableLongBag of();

    /**
     * Same as {@link #empty()}.
     */
    MutableLongBag with();

    /**
     * Same as {@link #with(long[])}.
     */
    MutableLongBag of(long... items);

    MutableLongBag with(long... items);

    /**
     * Same as {@link #withAll(LongIterable)}.
     */
    MutableLongBag ofAll(LongIterable items);

    MutableLongBag withAll(LongIterable items);

    /**
     * Same as {@link #withAll(Iterable)}.
     */
    MutableLongBag ofAll(Iterable<Long> iterable);

    MutableLongBag withAll(Iterable<Long> iterable);

    /**
     * @since 9.0
     */
    MutableLongBag ofAll(LongStream items);

    /**
     * @since 9.0
     */
    MutableLongBag withAll(LongStream items);
}
