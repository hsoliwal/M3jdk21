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

import org.eclipse.collections.api.ShortIterable;
import org.eclipse.collections.api.bag.primitive.ImmutableShortBag;

/**
 * A factory which creates instances of type {@link ImmutableShortBag}.
 * This file was automatically generated from template file immutablePrimitiveBagFactory.stg.
 *
 * @since 4.0.
 */
public interface ImmutableShortBagFactory
{
    /**
     * @since 6.0
     */
    ImmutableShortBag empty();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableShortBag of();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableShortBag with();

    /**
     * Same as {@link #with(short)}.
     */
    ImmutableShortBag of(short one);

    ImmutableShortBag with(short one);

    /**
     * Same as {@link #with(short[])}.
     */
    ImmutableShortBag of(short... items);

    ImmutableShortBag with(short... items);

    /**
     * Same as {@link #withAll(ShortIterable)}.
     */
    ImmutableShortBag ofAll(ShortIterable items);

    ImmutableShortBag withAll(ShortIterable items);

    /**
     * Same as {@link #withAll(Iterable)}.
     */
    ImmutableShortBag ofAll(Iterable<Short> iterable);

    ImmutableShortBag withAll(Iterable<Short> iterable);
}
