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

import org.eclipse.collections.api.BooleanIterable;
import org.eclipse.collections.api.list.primitive.ImmutableBooleanList;

/**
 * A factory which creates instances of type {@link ImmutableBooleanList}.
 * This file was automatically generated from template file immutablePrimitiveListFactory.stg.
 *
 * @since 3.2.
 */
public interface ImmutableBooleanListFactory
{
    /**
     * @since 6.0
     */
    ImmutableBooleanList empty();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableBooleanList of();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableBooleanList with();

    /**
     * Same as {@link #with(boolean)}.
     */
    ImmutableBooleanList of(boolean one);

    ImmutableBooleanList with(boolean one);

    /**
     * Same as {@link #with(boolean[])}.
     */
    ImmutableBooleanList of(boolean... items);

    ImmutableBooleanList with(boolean... items);

    /**
     * Same as {@link #withAll(BooleanIterable)}.
     */
    ImmutableBooleanList ofAll(BooleanIterable items);

    ImmutableBooleanList withAll(BooleanIterable items);

    /**
     * Same as {@link #withAll(Iterable)}.
     */
    ImmutableBooleanList ofAll(Iterable<Boolean> iterable);

    ImmutableBooleanList withAll(Iterable<Boolean> iterable);
}
