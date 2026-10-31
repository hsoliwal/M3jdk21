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

import org.eclipse.collections.api.CharIterable;
import org.eclipse.collections.api.stack.primitive.ImmutableCharStack;

/**
 * A factory which creates instances of type {@link ImmutableCharStack}.
 * This file was automatically generated from template file immutablePrimitiveStackFactory.stg.
 *
 * @since 4.0.
 */
public interface ImmutableCharStackFactory
{
    /**
     * @since 6.0
     */
    ImmutableCharStack empty();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableCharStack of();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableCharStack with();

    /**
     * Same as {@link #with(char)}.
     */
    ImmutableCharStack of(char one);

    ImmutableCharStack with(char one);

    /**
     * Same as {@link #with(char[])}.
     */
    ImmutableCharStack of(char... items);

    ImmutableCharStack with(char... items);

    /**
     * Same as {@link #withAll(CharIterable)}.
     */
    ImmutableCharStack ofAll(CharIterable items);

    ImmutableCharStack withAll(CharIterable items);

    /**
     * Same as {@link #withAll(Iterable)}.
     */
    ImmutableCharStack ofAll(Iterable<Character> iterable);

    ImmutableCharStack withAll(Iterable<Character> iterable);

    /**
     * Same as {@link #withAllReversed(CharIterable)}.
     */
    ImmutableCharStack ofAllReversed(CharIterable items);

    ImmutableCharStack withAllReversed(CharIterable items);
}
