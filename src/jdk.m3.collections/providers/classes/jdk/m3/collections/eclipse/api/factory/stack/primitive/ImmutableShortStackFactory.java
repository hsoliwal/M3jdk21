/*
 * Copyright (c) 2026 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.api.factory.stack.primitive;

import jdk.m3.collections.eclipse.api.ShortIterable;
import jdk.m3.collections.eclipse.api.stack.primitive.ImmutableShortStack;

/**
 * A factory which creates instances of type {@link ImmutableShortStack}.
 * This file was automatically generated from template file immutablePrimitiveStackFactory.stg.
 *
 * @since 4.0.
 */
public interface ImmutableShortStackFactory
{
    /**
     * @since 6.0
     */
    ImmutableShortStack empty();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableShortStack of();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableShortStack with();

    /**
     * Same as {@link #with(short)}.
     */
    ImmutableShortStack of(short one);

    ImmutableShortStack with(short one);

    /**
     * Same as {@link #with(short[])}.
     */
    ImmutableShortStack of(short... items);

    ImmutableShortStack with(short... items);

    /**
     * Same as {@link #withAll(ShortIterable)}.
     */
    ImmutableShortStack ofAll(ShortIterable items);

    ImmutableShortStack withAll(ShortIterable items);

    /**
     * Same as {@link #withAll(Iterable)}.
     */
    ImmutableShortStack ofAll(Iterable<Short> iterable);

    ImmutableShortStack withAll(Iterable<Short> iterable);

    /**
     * Same as {@link #withAllReversed(ShortIterable)}.
     */
    ImmutableShortStack ofAllReversed(ShortIterable items);

    ImmutableShortStack withAllReversed(ShortIterable items);
}
