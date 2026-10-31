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

import org.eclipse.collections.api.LongIterable;
import org.eclipse.collections.api.set.primitive.MutableLongSet;
import java.util.stream.LongStream;

/**
 * A factory which creates instances of type {@link MutableLongSet}.
 * This file was automatically generated from template file mutablePrimitiveSetFactory.stg.
 *
 * @since 6.0.
 */
public interface MutableLongSetFactory
{
    MutableLongSet empty();

    /**
     * Same as {@link #empty()}.
     */
    MutableLongSet of();

    /**
     * Same as {@link #empty()}.
     */
    MutableLongSet with();

    /**
     * Same as {@link #empty()}. but takes in an initial capacity
     * @since 10.3
     */
    default MutableLongSet withInitialCapacity(int capacity)
    {
        throw new UnsupportedOperationException("Default method to ensure backwards compatibility");
    }

    /**
     * Same as {@link #with(long[])}.
     */
    MutableLongSet of(long... items);

    MutableLongSet with(long... items);

    /**
     * Same as {@link #withAll(LongIterable)}.
     */
    MutableLongSet ofAll(LongIterable items);

    MutableLongSet withAll(LongIterable items);

    /**
     * Same as {@link #withAll(Iterable)}.
     */
    MutableLongSet ofAll(Iterable<Long> iterable);

    MutableLongSet withAll(Iterable<Long> iterable);

    /**
     * @since 9.0
     */
    MutableLongSet ofAll(LongStream items);

    /**
     * @since 9.0
     */
    MutableLongSet withAll(LongStream items);
}
