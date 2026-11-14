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

import org.eclipse.collections.api.ShortIterable;
import org.eclipse.collections.api.set.primitive.ImmutableShortSet;

/**
 * A factory which creates instances of type {@link ImmutableShortSet}.
 * This file was automatically generated from template file immutablePrimitiveSetFactory.stg.
 *
 * @since 4.0.
 */
public interface ImmutableShortSetFactory
{
    /**
     * @since 6.0
     */
    ImmutableShortSet empty();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableShortSet of();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableShortSet with();

    /**
     * Same as {@link #with(short)}.
     */
    ImmutableShortSet of(short one);

    ImmutableShortSet with(short one);

    /**
     * Same as {@link #with(short[])}.
     */
    ImmutableShortSet of(short... items);

    ImmutableShortSet with(short... items);

    /**
     * Same as {@link #withAll(ShortIterable)}.
     */
    ImmutableShortSet ofAll(ShortIterable items);

    ImmutableShortSet withAll(ShortIterable items);

    /**
     * Same as {@link #withAll(Iterable)}.
     */
    ImmutableShortSet ofAll(Iterable<Short> iterable);

    ImmutableShortSet withAll(Iterable<Short> iterable);
}
