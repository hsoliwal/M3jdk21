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
import org.eclipse.collections.api.bag.primitive.MutableShortBag;

/**
 * A factory which creates instances of type {@link MutableShortBag}.
 * This file was automatically generated from template file mutablePrimitiveBagFactory.stg.
 *
 * @since 6.0.
 */
public interface MutableShortBagFactory
{
    MutableShortBag empty();

    /**
     * Same as {@link #empty()}.
     */
    MutableShortBag of();

    /**
     * Same as {@link #empty()}.
     */
    MutableShortBag with();

    /**
     * Same as {@link #with(short[])}.
     */
    MutableShortBag of(short... items);

    MutableShortBag with(short... items);

    /**
     * Same as {@link #withAll(ShortIterable)}.
     */
    MutableShortBag ofAll(ShortIterable items);

    MutableShortBag withAll(ShortIterable items);

    /**
     * Same as {@link #withAll(Iterable)}.
     */
    MutableShortBag ofAll(Iterable<Short> iterable);

    MutableShortBag withAll(Iterable<Short> iterable);
}
