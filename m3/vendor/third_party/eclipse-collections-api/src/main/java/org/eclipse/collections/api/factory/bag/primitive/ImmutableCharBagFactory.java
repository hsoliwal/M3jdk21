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

import org.eclipse.collections.api.CharIterable;
import org.eclipse.collections.api.bag.primitive.ImmutableCharBag;

/**
 * A factory which creates instances of type {@link ImmutableCharBag}.
 * This file was automatically generated from template file immutablePrimitiveBagFactory.stg.
 *
 * @since 4.0.
 */
public interface ImmutableCharBagFactory
{
    /**
     * @since 6.0
     */
    ImmutableCharBag empty();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableCharBag of();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableCharBag with();

    /**
     * Same as {@link #with(char)}.
     */
    ImmutableCharBag of(char one);

    ImmutableCharBag with(char one);

    /**
     * Same as {@link #with(char[])}.
     */
    ImmutableCharBag of(char... items);

    ImmutableCharBag with(char... items);

    /**
     * Same as {@link #withAll(CharIterable)}.
     */
    ImmutableCharBag ofAll(CharIterable items);

    ImmutableCharBag withAll(CharIterable items);

    /**
     * Same as {@link #withAll(Iterable)}.
     */
    ImmutableCharBag ofAll(Iterable<Character> iterable);

    ImmutableCharBag withAll(Iterable<Character> iterable);
}
