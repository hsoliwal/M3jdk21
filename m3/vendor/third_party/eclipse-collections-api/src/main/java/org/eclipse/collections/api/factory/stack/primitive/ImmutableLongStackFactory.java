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

import org.eclipse.collections.api.LongIterable;
import org.eclipse.collections.api.stack.primitive.ImmutableLongStack;
import java.util.stream.LongStream;

/**
 * A factory which creates instances of type {@link ImmutableLongStack}.
 * This file was automatically generated from template file immutablePrimitiveStackFactory.stg.
 *
 * @since 4.0.
 */
public interface ImmutableLongStackFactory
{
    /**
     * @since 6.0
     */
    ImmutableLongStack empty();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableLongStack of();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableLongStack with();

    /**
     * Same as {@link #with(long)}.
     */
    ImmutableLongStack of(long one);

    ImmutableLongStack with(long one);

    /**
     * Same as {@link #with(long[])}.
     */
    ImmutableLongStack of(long... items);

    ImmutableLongStack with(long... items);

    /**
     * Same as {@link #withAll(LongIterable)}.
     */
    ImmutableLongStack ofAll(LongIterable items);

    ImmutableLongStack withAll(LongIterable items);

    /**
     * Same as {@link #withAll(Iterable)}.
     */
    ImmutableLongStack ofAll(Iterable<Long> iterable);

    ImmutableLongStack withAll(Iterable<Long> iterable);

    /**
     * Same as {@link #withAllReversed(LongIterable)}.
     */
    ImmutableLongStack ofAllReversed(LongIterable items);

    ImmutableLongStack withAllReversed(LongIterable items);

    /**
     * @since 9.0
     */
    ImmutableLongStack ofAll(LongStream items);

    /**
     * @since 9.0
     */
    ImmutableLongStack withAll(LongStream items);
}
