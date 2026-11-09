/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.api.factory.bag.primitive;

import org.eclipse.collections.api.BooleanIterable;
import org.eclipse.collections.api.bag.primitive.ImmutableBooleanBag;

/**
 * A factory which creates instances of type {@link ImmutableBooleanBag}.
 * This file was automatically generated from template file immutablePrimitiveBagFactory.stg.
 *
 * @since 4.0.
 */
public interface ImmutableBooleanBagFactory
{
    /**
     * @since 6.0
     */
    ImmutableBooleanBag empty();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableBooleanBag of();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableBooleanBag with();

    /**
     * Same as {@link #with(boolean)}.
     */
    ImmutableBooleanBag of(boolean one);

    ImmutableBooleanBag with(boolean one);

    /**
     * Same as {@link #with(boolean[])}.
     */
    ImmutableBooleanBag of(boolean... items);

    ImmutableBooleanBag with(boolean... items);

    /**
     * Same as {@link #withAll(BooleanIterable)}.
     */
    ImmutableBooleanBag ofAll(BooleanIterable items);

    ImmutableBooleanBag withAll(BooleanIterable items);

    /**
     * Same as {@link #withAll(Iterable)}.
     */
    ImmutableBooleanBag ofAll(Iterable<Boolean> iterable);

    ImmutableBooleanBag withAll(Iterable<Boolean> iterable);
}
