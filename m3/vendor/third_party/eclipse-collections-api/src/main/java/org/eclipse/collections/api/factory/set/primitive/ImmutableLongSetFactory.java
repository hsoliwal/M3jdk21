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
import org.eclipse.collections.api.set.primitive.ImmutableLongSet;
import java.util.stream.LongStream;

/**
 * A factory which creates instances of type {@link ImmutableLongSet}.
 * This file was automatically generated from template file immutablePrimitiveSetFactory.stg.
 *
 * @since 4.0.
 */
public interface ImmutableLongSetFactory
{
    /**
     * @since 6.0
     */
    ImmutableLongSet empty();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableLongSet of();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableLongSet with();

    /**
     * Same as {@link #with(long)}.
     */
    ImmutableLongSet of(long one);

    ImmutableLongSet with(long one);

    /**
     * Same as {@link #with(long[])}.
     */
    ImmutableLongSet of(long... items);

    ImmutableLongSet with(long... items);

    /**
     * Same as {@link #withAll(LongIterable)}.
     */
    ImmutableLongSet ofAll(LongIterable items);

    ImmutableLongSet withAll(LongIterable items);

    /**
     * Same as {@link #withAll(Iterable)}.
     */
    ImmutableLongSet ofAll(Iterable<Long> iterable);

    ImmutableLongSet withAll(Iterable<Long> iterable);

    /**
     * @since 9.0
     */
    ImmutableLongSet ofAll(LongStream items);

    /**
     * @since 9.0
     */
    ImmutableLongSet withAll(LongStream items);
}
