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
import jdk.m3.collections.eclipse.api.stack.primitive.MutableShortStack;

/**
 * A factory which creates instances of type {@link MutableShortStack}.
 * This file was automatically generated from template file mutablePrimitiveStackFactory.stg.
 *
 * @since 6.0.
 */
public interface MutableShortStackFactory
{
    MutableShortStack empty();

    /**
     * Same as {@link #empty()}.
     */
    MutableShortStack of();

    /**
     * Same as {@link #empty()}.
     */
    MutableShortStack with();

    /**
     * Same as {@link #with(short[])}.
     */
    MutableShortStack of(short... items);

    MutableShortStack with(short... items);

    /**
     * Same as {@link #withAll(ShortIterable)}.
     */
    MutableShortStack ofAll(ShortIterable items);

    MutableShortStack withAll(ShortIterable items);

    /**
     * Same as {@link #withAll(Iterable)}.
     */
    MutableShortStack ofAll(Iterable<Short> iterable);

    MutableShortStack withAll(Iterable<Short> iterable);

    /**
     * Same as {@link #withAllReversed(ShortIterable)}.
     */
    MutableShortStack ofAllReversed(ShortIterable items);

    MutableShortStack withAllReversed(ShortIterable items);
}
