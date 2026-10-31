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

import org.eclipse.collections.api.BooleanIterable;
import org.eclipse.collections.api.set.primitive.ImmutableBooleanSet;

/**
 * A factory which creates instances of type {@link ImmutableBooleanSet}.
 * This file was automatically generated from template file immutablePrimitiveSetFactory.stg.
 *
 * @since 4.0.
 */
public interface ImmutableBooleanSetFactory
{
    /**
     * @since 6.0
     */
    ImmutableBooleanSet empty();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableBooleanSet of();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableBooleanSet with();

    /**
     * Same as {@link #with(boolean)}.
     */
    ImmutableBooleanSet of(boolean one);

    ImmutableBooleanSet with(boolean one);

    /**
     * Same as {@link #with(boolean[])}.
     */
    ImmutableBooleanSet of(boolean... items);

    ImmutableBooleanSet with(boolean... items);

    /**
     * Same as {@link #withAll(BooleanIterable)}.
     */
    ImmutableBooleanSet ofAll(BooleanIterable items);

    ImmutableBooleanSet withAll(BooleanIterable items);

    /**
     * Same as {@link #withAll(Iterable)}.
     */
    ImmutableBooleanSet ofAll(Iterable<Boolean> iterable);

    ImmutableBooleanSet withAll(Iterable<Boolean> iterable);
}
