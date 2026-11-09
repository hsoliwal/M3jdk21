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

import org.eclipse.collections.api.CharIterable;
import org.eclipse.collections.api.set.primitive.MutableCharSet;

/**
 * A factory which creates instances of type {@link MutableCharSet}.
 * This file was automatically generated from template file mutablePrimitiveSetFactory.stg.
 *
 * @since 6.0.
 */
public interface MutableCharSetFactory
{
    MutableCharSet empty();

    /**
     * Same as {@link #empty()}.
     */
    MutableCharSet of();

    /**
     * Same as {@link #empty()}.
     */
    MutableCharSet with();

    /**
     * Same as {@link #empty()}. but takes in an initial capacity
     * @since 10.3
     */
    default MutableCharSet withInitialCapacity(int capacity)
    {
        throw new UnsupportedOperationException("Default method to ensure backwards compatibility");
    }

    /**
     * Same as {@link #with(char[])}.
     */
    MutableCharSet of(char... items);

    MutableCharSet with(char... items);

    /**
     * Same as {@link #withAll(CharIterable)}.
     */
    MutableCharSet ofAll(CharIterable items);

    MutableCharSet withAll(CharIterable items);

    /**
     * Same as {@link #withAll(Iterable)}.
     */
    MutableCharSet ofAll(Iterable<Character> iterable);

    MutableCharSet withAll(Iterable<Character> iterable);
}
