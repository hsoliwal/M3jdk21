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

import org.eclipse.collections.api.IntIterable;
import org.eclipse.collections.api.bag.primitive.MutableIntBag;
import java.util.stream.IntStream;

/**
 * A factory which creates instances of type {@link MutableIntBag}.
 * This file was automatically generated from template file mutablePrimitiveBagFactory.stg.
 *
 * @since 6.0.
 */
public interface MutableIntBagFactory
{
    MutableIntBag empty();

    /**
     * Same as {@link #empty()}.
     */
    MutableIntBag of();

    /**
     * Same as {@link #empty()}.
     */
    MutableIntBag with();

    /**
     * Same as {@link #with(int[])}.
     */
    MutableIntBag of(int... items);

    MutableIntBag with(int... items);

    /**
     * Same as {@link #withAll(IntIterable)}.
     */
    MutableIntBag ofAll(IntIterable items);

    MutableIntBag withAll(IntIterable items);

    /**
     * Same as {@link #withAll(Iterable)}.
     */
    MutableIntBag ofAll(Iterable<Integer> iterable);

    MutableIntBag withAll(Iterable<Integer> iterable);

    /**
     * @since 9.0
     */
    MutableIntBag ofAll(IntStream items);

    /**
     * @since 9.0
     */
    MutableIntBag withAll(IntStream items);
}
