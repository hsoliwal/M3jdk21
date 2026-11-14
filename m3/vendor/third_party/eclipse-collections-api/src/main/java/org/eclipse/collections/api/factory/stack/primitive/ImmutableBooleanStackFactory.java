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

import org.eclipse.collections.api.BooleanIterable;
import org.eclipse.collections.api.stack.primitive.ImmutableBooleanStack;

/**
 * A factory which creates instances of type {@link ImmutableBooleanStack}.
 * This file was automatically generated from template file immutablePrimitiveStackFactory.stg.
 *
 * @since 4.0.
 */
public interface ImmutableBooleanStackFactory
{
    /**
     * @since 6.0
     */
    ImmutableBooleanStack empty();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableBooleanStack of();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableBooleanStack with();

    /**
     * Same as {@link #with(boolean)}.
     */
    ImmutableBooleanStack of(boolean one);

    ImmutableBooleanStack with(boolean one);

    /**
     * Same as {@link #with(boolean[])}.
     */
    ImmutableBooleanStack of(boolean... items);

    ImmutableBooleanStack with(boolean... items);

    /**
     * Same as {@link #withAll(BooleanIterable)}.
     */
    ImmutableBooleanStack ofAll(BooleanIterable items);

    ImmutableBooleanStack withAll(BooleanIterable items);

    /**
     * Same as {@link #withAll(Iterable)}.
     */
    ImmutableBooleanStack ofAll(Iterable<Boolean> iterable);

    ImmutableBooleanStack withAll(Iterable<Boolean> iterable);

    /**
     * Same as {@link #withAllReversed(BooleanIterable)}.
     */
    ImmutableBooleanStack ofAllReversed(BooleanIterable items);

    ImmutableBooleanStack withAllReversed(BooleanIterable items);
}
