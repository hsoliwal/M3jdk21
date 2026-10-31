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

import org.eclipse.collections.api.ShortIterable;
import org.eclipse.collections.api.list.primitive.ImmutableShortList;

/**
 * A factory which creates instances of type {@link ImmutableShortList}.
 * This file was automatically generated from template file immutablePrimitiveListFactory.stg.
 *
 * @since 3.2.
 */
public interface ImmutableShortListFactory
{
    /**
     * @since 6.0
     */
    ImmutableShortList empty();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableShortList of();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableShortList with();

    /**
     * Same as {@link #with(short)}.
     */
    ImmutableShortList of(short one);

    ImmutableShortList with(short one);

    /**
     * Same as {@link #with(short[])}.
     */
    ImmutableShortList of(short... items);

    ImmutableShortList with(short... items);

    /**
     * Same as {@link #withAll(ShortIterable)}.
     */
    ImmutableShortList ofAll(ShortIterable items);

    ImmutableShortList withAll(ShortIterable items);

    /**
     * Same as {@link #withAll(Iterable)}.
     */
    ImmutableShortList ofAll(Iterable<Short> iterable);

    ImmutableShortList withAll(Iterable<Short> iterable);
}
