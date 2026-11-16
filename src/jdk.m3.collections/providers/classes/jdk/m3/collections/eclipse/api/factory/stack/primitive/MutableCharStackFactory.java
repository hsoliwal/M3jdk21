/*
 * Copyright (c) 2026 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.api.factory.stack.primitive;

import jdk.m3.collections.eclipse.api.CharIterable;
import jdk.m3.collections.eclipse.api.stack.primitive.MutableCharStack;

/**
 * A factory which creates instances of type {@link MutableCharStack}.
 * This file was automatically generated from template file mutablePrimitiveStackFactory.stg.
 *
 * @since 6.0.
 */
public interface MutableCharStackFactory
{
    MutableCharStack empty();

    /**
     * Same as {@link #empty()}.
     */
    MutableCharStack of();

    /**
     * Same as {@link #empty()}.
     */
    MutableCharStack with();

    /**
     * Same as {@link #with(char[])}.
     */
    MutableCharStack of(char... items);

    MutableCharStack with(char... items);

    /**
     * Same as {@link #withAll(CharIterable)}.
     */
    MutableCharStack ofAll(CharIterable items);

    MutableCharStack withAll(CharIterable items);

    /**
     * Same as {@link #withAll(Iterable)}.
     */
    MutableCharStack ofAll(Iterable<Character> iterable);

    MutableCharStack withAll(Iterable<Character> iterable);

    /**
     * Same as {@link #withAllReversed(CharIterable)}.
     */
    MutableCharStack ofAllReversed(CharIterable items);

    MutableCharStack withAllReversed(CharIterable items);
}
