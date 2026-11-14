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

import org.eclipse.collections.api.BooleanIterable;
import org.eclipse.collections.api.set.primitive.MutableBooleanSet;

/**
 * A factory which creates instances of type {@link MutableBooleanSet}.
 * This file was automatically generated from template file mutablePrimitiveSetFactory.stg.
 *
 * @since 6.0.
 */
public interface MutableBooleanSetFactory
{
    MutableBooleanSet empty();

    /**
     * Same as {@link #empty()}.
     */
    MutableBooleanSet of();

    /**
     * Same as {@link #empty()}.
     */
    MutableBooleanSet with();

    /**
     * Same as {@link #with(boolean[])}.
     */
    MutableBooleanSet of(boolean... items);

    MutableBooleanSet with(boolean... items);

    /**
     * Same as {@link #withAll(BooleanIterable)}.
     */
    MutableBooleanSet ofAll(BooleanIterable items);

    MutableBooleanSet withAll(BooleanIterable items);

    /**
     * Same as {@link #withAll(Iterable)}.
     */
    MutableBooleanSet ofAll(Iterable<Boolean> iterable);

    MutableBooleanSet withAll(Iterable<Boolean> iterable);
}
