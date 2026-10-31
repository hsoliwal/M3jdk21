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
import org.eclipse.collections.api.bag.primitive.ImmutableIntBag;
import java.util.stream.IntStream;

/**
 * A factory which creates instances of type {@link ImmutableIntBag}.
 * This file was automatically generated from template file immutablePrimitiveBagFactory.stg.
 *
 * @since 4.0.
 */
public interface ImmutableIntBagFactory
{
    /**
     * @since 6.0
     */
    ImmutableIntBag empty();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableIntBag of();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableIntBag with();

    /**
     * Same as {@link #with(int)}.
     */
    ImmutableIntBag of(int one);

    ImmutableIntBag with(int one);

    /**
     * Same as {@link #with(int[])}.
     */
    ImmutableIntBag of(int... items);

    ImmutableIntBag with(int... items);

    /**
     * Same as {@link #withAll(IntIterable)}.
     */
    ImmutableIntBag ofAll(IntIterable items);

    ImmutableIntBag withAll(IntIterable items);

    /**
     * Same as {@link #withAll(Iterable)}.
     */
    ImmutableIntBag ofAll(Iterable<Integer> iterable);

    ImmutableIntBag withAll(Iterable<Integer> iterable);

    /**
     * @since 9.0
     */
    ImmutableIntBag ofAll(IntStream items);

    /**
     * @since 9.0
     */
    ImmutableIntBag withAll(IntStream items);
}
