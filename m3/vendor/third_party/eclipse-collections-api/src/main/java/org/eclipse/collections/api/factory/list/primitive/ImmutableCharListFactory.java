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

import org.eclipse.collections.api.CharIterable;
import org.eclipse.collections.api.list.primitive.ImmutableCharList;

/**
 * A factory which creates instances of type {@link ImmutableCharList}.
 * This file was automatically generated from template file immutablePrimitiveListFactory.stg.
 *
 * @since 3.2.
 */
public interface ImmutableCharListFactory
{
    /**
     * @since 6.0
     */
    ImmutableCharList empty();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableCharList of();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableCharList with();

    /**
     * Same as {@link #with(char)}.
     */
    ImmutableCharList of(char one);

    ImmutableCharList with(char one);

    /**
     * Same as {@link #with(char[])}.
     */
    ImmutableCharList of(char... items);

    ImmutableCharList with(char... items);

    /**
     * Same as {@link #withAll(CharIterable)}.
     */
    ImmutableCharList ofAll(CharIterable items);

    ImmutableCharList withAll(CharIterable items);

    /**
     * Same as {@link #withAll(Iterable)}.
     */
    ImmutableCharList ofAll(Iterable<Character> iterable);

    ImmutableCharList withAll(Iterable<Character> iterable);
}
