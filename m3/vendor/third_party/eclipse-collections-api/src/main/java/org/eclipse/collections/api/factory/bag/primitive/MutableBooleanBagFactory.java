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
import org.eclipse.collections.api.bag.primitive.MutableBooleanBag;

/**
 * A factory which creates instances of type {@link MutableBooleanBag}.
 * This file was automatically generated from template file mutablePrimitiveBagFactory.stg.
 *
 * @since 6.0.
 */
public interface MutableBooleanBagFactory
{
    MutableBooleanBag empty();

    /**
     * Same as {@link #empty()}.
     */
    MutableBooleanBag of();

    /**
     * Same as {@link #empty()}.
     */
    MutableBooleanBag with();

    /**
     * Same as {@link #with(boolean[])}.
     */
    MutableBooleanBag of(boolean... items);

    MutableBooleanBag with(boolean... items);

    /**
     * Same as {@link #withAll(BooleanIterable)}.
     */
    MutableBooleanBag ofAll(BooleanIterable items);

    MutableBooleanBag withAll(BooleanIterable items);

    /**
     * Same as {@link #withAll(Iterable)}.
     */
    MutableBooleanBag ofAll(Iterable<Boolean> iterable);

    MutableBooleanBag withAll(Iterable<Boolean> iterable);
}
