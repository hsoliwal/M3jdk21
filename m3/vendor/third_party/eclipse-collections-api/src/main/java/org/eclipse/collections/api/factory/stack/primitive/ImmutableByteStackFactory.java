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

import org.eclipse.collections.api.ByteIterable;
import org.eclipse.collections.api.stack.primitive.ImmutableByteStack;

/**
 * A factory which creates instances of type {@link ImmutableByteStack}.
 * This file was automatically generated from template file immutablePrimitiveStackFactory.stg.
 *
 * @since 4.0.
 */
public interface ImmutableByteStackFactory
{
    /**
     * @since 6.0
     */
    ImmutableByteStack empty();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableByteStack of();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableByteStack with();

    /**
     * Same as {@link #with(byte)}.
     */
    ImmutableByteStack of(byte one);

    ImmutableByteStack with(byte one);

    /**
     * Same as {@link #with(byte[])}.
     */
    ImmutableByteStack of(byte... items);

    ImmutableByteStack with(byte... items);

    /**
     * Same as {@link #withAll(ByteIterable)}.
     */
    ImmutableByteStack ofAll(ByteIterable items);

    ImmutableByteStack withAll(ByteIterable items);

    /**
     * Same as {@link #withAll(Iterable)}.
     */
    ImmutableByteStack ofAll(Iterable<Byte> iterable);

    ImmutableByteStack withAll(Iterable<Byte> iterable);

    /**
     * Same as {@link #withAllReversed(ByteIterable)}.
     */
    ImmutableByteStack ofAllReversed(ByteIterable items);

    ImmutableByteStack withAllReversed(ByteIterable items);
}
