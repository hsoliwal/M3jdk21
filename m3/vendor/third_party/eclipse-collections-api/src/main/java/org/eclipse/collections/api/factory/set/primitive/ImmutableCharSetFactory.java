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
import org.eclipse.collections.api.set.primitive.ImmutableCharSet;

/**
 * A factory which creates instances of type {@link ImmutableCharSet}.
 * This file was automatically generated from template file immutablePrimitiveSetFactory.stg.
 *
 * @since 4.0.
 */
public interface ImmutableCharSetFactory
{
    /**
     * @since 6.0
     */
    ImmutableCharSet empty();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableCharSet of();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableCharSet with();

    /**
     * Same as {@link #with(char)}.
     */
    ImmutableCharSet of(char one);

    ImmutableCharSet with(char one);

    /**
     * Same as {@link #with(char[])}.
     */
    ImmutableCharSet of(char... items);

    ImmutableCharSet with(char... items);

    /**
     * Same as {@link #withAll(CharIterable)}.
     */
    ImmutableCharSet ofAll(CharIterable items);

    ImmutableCharSet withAll(CharIterable items);

    /**
     * Same as {@link #withAll(Iterable)}.
     */
    ImmutableCharSet ofAll(Iterable<Character> iterable);

    ImmutableCharSet withAll(Iterable<Character> iterable);
}
