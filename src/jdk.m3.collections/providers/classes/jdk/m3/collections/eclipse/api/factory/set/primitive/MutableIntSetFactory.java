/*
 * Copyright (c) 2026 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.api.factory.set.primitive;

import jdk.m3.collections.eclipse.api.IntIterable;
import jdk.m3.collections.eclipse.api.set.primitive.MutableIntSet;
import java.util.stream.IntStream;

/**
 * A factory which creates instances of type {@link MutableIntSet}.
 * This file was automatically generated from template file mutablePrimitiveSetFactory.stg.
 *
 * @since 6.0.
 */
public interface MutableIntSetFactory
{
    MutableIntSet empty();

    /**
     * Same as {@link #empty()}.
     */
    MutableIntSet of();

    /**
     * Same as {@link #empty()}.
     */
    MutableIntSet with();

    /**
     * Same as {@link #empty()}. but takes in an initial capacity
     * @since 10.3
     */
    default MutableIntSet withInitialCapacity(int capacity)
    {
        throw new UnsupportedOperationException("Default method to ensure backwards compatibility");
    }

    /**
     * Same as {@link #with(int[])}.
     */
    MutableIntSet of(int... items);

    MutableIntSet with(int... items);

    /**
     * Same as {@link #withAll(IntIterable)}.
     */
    MutableIntSet ofAll(IntIterable items);

    MutableIntSet withAll(IntIterable items);

    /**
     * Same as {@link #withAll(Iterable)}.
     */
    MutableIntSet ofAll(Iterable<Integer> iterable);

    MutableIntSet withAll(Iterable<Integer> iterable);

    /**
     * @since 9.0
     */
    MutableIntSet ofAll(IntStream items);

    /**
     * @since 9.0
     */
    MutableIntSet withAll(IntStream items);
}
