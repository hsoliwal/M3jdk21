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
import org.eclipse.collections.api.bag.primitive.MutableCharBag;

/**
 * A factory which creates instances of type {@link MutableCharBag}.
 * This file was automatically generated from template file mutablePrimitiveBagFactory.stg.
 *
 * @since 6.0.
 */
public interface MutableCharBagFactory
{
    MutableCharBag empty();

    /**
     * Same as {@link #empty()}.
     */
    MutableCharBag of();

    /**
     * Same as {@link #empty()}.
     */
    MutableCharBag with();

    /**
     * Same as {@link #with(char[])}.
     */
    MutableCharBag of(char... items);

    MutableCharBag with(char... items);

    /**
     * Same as {@link #withAll(CharIterable)}.
     */
    MutableCharBag ofAll(CharIterable items);

    MutableCharBag withAll(CharIterable items);

    /**
     * Same as {@link #withAll(Iterable)}.
     */
    MutableCharBag ofAll(Iterable<Character> iterable);

    MutableCharBag withAll(Iterable<Character> iterable);
}
