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

import org.eclipse.collections.api.IntIterable;
import org.eclipse.collections.api.stack.primitive.ImmutableIntStack;
import java.util.stream.IntStream;

/**
 * A factory which creates instances of type {@link ImmutableIntStack}.
 * This file was automatically generated from template file immutablePrimitiveStackFactory.stg.
 *
 * @since 4.0.
 */
public interface ImmutableIntStackFactory
{
    /**
     * @since 6.0
     */
    ImmutableIntStack empty();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableIntStack of();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableIntStack with();

    /**
     * Same as {@link #with(int)}.
     */
    ImmutableIntStack of(int one);

    ImmutableIntStack with(int one);

    /**
     * Same as {@link #with(int[])}.
     */
    ImmutableIntStack of(int... items);

    ImmutableIntStack with(int... items);

    /**
     * Same as {@link #withAll(IntIterable)}.
     */
    ImmutableIntStack ofAll(IntIterable items);

    ImmutableIntStack withAll(IntIterable items);

    /**
     * Same as {@link #withAll(Iterable)}.
     */
    ImmutableIntStack ofAll(Iterable<Integer> iterable);

    ImmutableIntStack withAll(Iterable<Integer> iterable);

    /**
     * Same as {@link #withAllReversed(IntIterable)}.
     */
    ImmutableIntStack ofAllReversed(IntIterable items);

    ImmutableIntStack withAllReversed(IntIterable items);

    /**
     * @since 9.0
     */
    ImmutableIntStack ofAll(IntStream items);

    /**
     * @since 9.0
     */
    ImmutableIntStack withAll(IntStream items);
}
