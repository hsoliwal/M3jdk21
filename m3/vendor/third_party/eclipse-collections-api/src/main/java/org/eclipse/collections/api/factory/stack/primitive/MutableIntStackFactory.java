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
import org.eclipse.collections.api.stack.primitive.MutableIntStack;
import java.util.stream.IntStream;

/**
 * A factory which creates instances of type {@link MutableIntStack}.
 * This file was automatically generated from template file mutablePrimitiveStackFactory.stg.
 *
 * @since 6.0.
 */
public interface MutableIntStackFactory
{
    MutableIntStack empty();

    /**
     * Same as {@link #empty()}.
     */
    MutableIntStack of();

    /**
     * Same as {@link #empty()}.
     */
    MutableIntStack with();

    /**
     * Same as {@link #with(int[])}.
     */
    MutableIntStack of(int... items);

    MutableIntStack with(int... items);

    /**
     * Same as {@link #withAll(IntIterable)}.
     */
    MutableIntStack ofAll(IntIterable items);

    MutableIntStack withAll(IntIterable items);

    /**
     * Same as {@link #withAll(Iterable)}.
     */
    MutableIntStack ofAll(Iterable<Integer> iterable);

    MutableIntStack withAll(Iterable<Integer> iterable);

    /**
     * Same as {@link #withAllReversed(IntIterable)}.
     */
    MutableIntStack ofAllReversed(IntIterable items);

    MutableIntStack withAllReversed(IntIterable items);

    /**
     * @since 9.0
     */
    MutableIntStack ofAll(IntStream items);

    /**
     * @since 9.0
     */
    MutableIntStack withAll(IntStream items);
}
