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

import org.eclipse.collections.api.LongIterable;
import org.eclipse.collections.api.list.primitive.ImmutableLongList;
import java.util.stream.LongStream;

/**
 * A factory which creates instances of type {@link ImmutableLongList}.
 * This file was automatically generated from template file immutablePrimitiveListFactory.stg.
 *
 * @since 3.2.
 */
public interface ImmutableLongListFactory
{
    /**
     * @since 6.0
     */
    ImmutableLongList empty();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableLongList of();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableLongList with();

    /**
     * Same as {@link #with(long)}.
     */
    ImmutableLongList of(long one);

    ImmutableLongList with(long one);

    /**
     * Same as {@link #with(long[])}.
     */
    ImmutableLongList of(long... items);

    ImmutableLongList with(long... items);

    /**
     * Same as {@link #withAll(LongIterable)}.
     */
    ImmutableLongList ofAll(LongIterable items);

    ImmutableLongList withAll(LongIterable items);

    /**
     * Same as {@link #withAll(Iterable)}.
     */
    ImmutableLongList ofAll(Iterable<Long> iterable);

    ImmutableLongList withAll(Iterable<Long> iterable);

    /**
     * @since 9.0
     */
    ImmutableLongList ofAll(LongStream items);

    /**
     * @since 9.0
     */
    ImmutableLongList withAll(LongStream items);
}
