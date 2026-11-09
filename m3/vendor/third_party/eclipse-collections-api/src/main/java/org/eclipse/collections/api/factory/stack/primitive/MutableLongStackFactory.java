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
import org.eclipse.collections.api.stack.primitive.MutableLongStack;
import java.util.stream.LongStream;

/**
 * A factory which creates instances of type {@link MutableLongStack}.
 * This file was automatically generated from template file mutablePrimitiveStackFactory.stg.
 *
 * @since 6.0.
 */
public interface MutableLongStackFactory
{
    MutableLongStack empty();

    /**
     * Same as {@link #empty()}.
     */
    MutableLongStack of();

    /**
     * Same as {@link #empty()}.
     */
    MutableLongStack with();

    /**
     * Same as {@link #with(long[])}.
     */
    MutableLongStack of(long... items);

    MutableLongStack with(long... items);

    /**
     * Same as {@link #withAll(LongIterable)}.
     */
    MutableLongStack ofAll(LongIterable items);

    MutableLongStack withAll(LongIterable items);

    /**
     * Same as {@link #withAll(Iterable)}.
     */
    MutableLongStack ofAll(Iterable<Long> iterable);

    MutableLongStack withAll(Iterable<Long> iterable);

    /**
     * Same as {@link #withAllReversed(LongIterable)}.
     */
    MutableLongStack ofAllReversed(LongIterable items);

    MutableLongStack withAllReversed(LongIterable items);

    /**
     * @since 9.0
     */
    MutableLongStack ofAll(LongStream items);

    /**
     * @since 9.0
     */
    MutableLongStack withAll(LongStream items);
}
