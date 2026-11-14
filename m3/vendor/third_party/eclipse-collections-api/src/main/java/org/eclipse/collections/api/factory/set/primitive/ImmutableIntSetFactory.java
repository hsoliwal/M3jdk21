/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.api.factory.set.primitive;

import org.eclipse.collections.api.IntIterable;
import org.eclipse.collections.api.set.primitive.ImmutableIntSet;
import java.util.stream.IntStream;

/**
 * A factory which creates instances of type {@link ImmutableIntSet}.
 * This file was automatically generated from template file immutablePrimitiveSetFactory.stg.
 *
 * @since 4.0.
 */
public interface ImmutableIntSetFactory
{
    /**
     * @since 6.0
     */
    ImmutableIntSet empty();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableIntSet of();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableIntSet with();

    /**
     * Same as {@link #with(int)}.
     */
    ImmutableIntSet of(int one);

    ImmutableIntSet with(int one);

    /**
     * Same as {@link #with(int[])}.
     */
    ImmutableIntSet of(int... items);

    ImmutableIntSet with(int... items);

    /**
     * Same as {@link #withAll(IntIterable)}.
     */
    ImmutableIntSet ofAll(IntIterable items);

    ImmutableIntSet withAll(IntIterable items);

    /**
     * Same as {@link #withAll(Iterable)}.
     */
    ImmutableIntSet ofAll(Iterable<Integer> iterable);

    ImmutableIntSet withAll(Iterable<Integer> iterable);

    /**
     * @since 9.0
     */
    ImmutableIntSet ofAll(IntStream items);

    /**
     * @since 9.0
     */
    ImmutableIntSet withAll(IntStream items);
}
