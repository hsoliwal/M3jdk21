/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.api.factory.list.primitive;

import org.eclipse.collections.api.IntIterable;
import org.eclipse.collections.api.list.primitive.ImmutableIntList;
import java.util.stream.IntStream;

/**
 * A factory which creates instances of type {@link ImmutableIntList}.
 * This file was automatically generated from template file immutablePrimitiveListFactory.stg.
 *
 * @since 3.2.
 */
public interface ImmutableIntListFactory
{
    /**
     * @since 6.0
     */
    ImmutableIntList empty();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableIntList of();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableIntList with();

    /**
     * Same as {@link #with(int)}.
     */
    ImmutableIntList of(int one);

    ImmutableIntList with(int one);

    /**
     * Same as {@link #with(int[])}.
     */
    ImmutableIntList of(int... items);

    ImmutableIntList with(int... items);

    /**
     * Same as {@link #withAll(IntIterable)}.
     */
    ImmutableIntList ofAll(IntIterable items);

    ImmutableIntList withAll(IntIterable items);

    /**
     * Same as {@link #withAll(Iterable)}.
     */
    ImmutableIntList ofAll(Iterable<Integer> iterable);

    ImmutableIntList withAll(Iterable<Integer> iterable);

    /**
     * @since 9.0
     */
    ImmutableIntList ofAll(IntStream items);

    /**
     * @since 9.0
     */
    ImmutableIntList withAll(IntStream items);
}
