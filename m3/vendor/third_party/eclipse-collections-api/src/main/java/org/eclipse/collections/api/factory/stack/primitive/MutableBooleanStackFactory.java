/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.api.factory.stack.primitive;

import org.eclipse.collections.api.BooleanIterable;
import org.eclipse.collections.api.stack.primitive.MutableBooleanStack;

/**
 * A factory which creates instances of type {@link MutableBooleanStack}.
 * This file was automatically generated from template file mutablePrimitiveStackFactory.stg.
 *
 * @since 6.0.
 */
public interface MutableBooleanStackFactory
{
    MutableBooleanStack empty();

    /**
     * Same as {@link #empty()}.
     */
    MutableBooleanStack of();

    /**
     * Same as {@link #empty()}.
     */
    MutableBooleanStack with();

    /**
     * Same as {@link #with(boolean[])}.
     */
    MutableBooleanStack of(boolean... items);

    MutableBooleanStack with(boolean... items);

    /**
     * Same as {@link #withAll(BooleanIterable)}.
     */
    MutableBooleanStack ofAll(BooleanIterable items);

    MutableBooleanStack withAll(BooleanIterable items);

    /**
     * Same as {@link #withAll(Iterable)}.
     */
    MutableBooleanStack ofAll(Iterable<Boolean> iterable);

    MutableBooleanStack withAll(Iterable<Boolean> iterable);

    /**
     * Same as {@link #withAllReversed(BooleanIterable)}.
     */
    MutableBooleanStack ofAllReversed(BooleanIterable items);

    MutableBooleanStack withAllReversed(BooleanIterable items);
}
