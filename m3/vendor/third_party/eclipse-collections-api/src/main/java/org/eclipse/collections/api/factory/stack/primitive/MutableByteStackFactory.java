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
import org.eclipse.collections.api.stack.primitive.MutableByteStack;

/**
 * A factory which creates instances of type {@link MutableByteStack}.
 * This file was automatically generated from template file mutablePrimitiveStackFactory.stg.
 *
 * @since 6.0.
 */
public interface MutableByteStackFactory
{
    MutableByteStack empty();

    /**
     * Same as {@link #empty()}.
     */
    MutableByteStack of();

    /**
     * Same as {@link #empty()}.
     */
    MutableByteStack with();

    /**
     * Same as {@link #with(byte[])}.
     */
    MutableByteStack of(byte... items);

    MutableByteStack with(byte... items);

    /**
     * Same as {@link #withAll(ByteIterable)}.
     */
    MutableByteStack ofAll(ByteIterable items);

    MutableByteStack withAll(ByteIterable items);

    /**
     * Same as {@link #withAll(Iterable)}.
     */
    MutableByteStack ofAll(Iterable<Byte> iterable);

    MutableByteStack withAll(Iterable<Byte> iterable);

    /**
     * Same as {@link #withAllReversed(ByteIterable)}.
     */
    MutableByteStack ofAllReversed(ByteIterable items);

    MutableByteStack withAllReversed(ByteIterable items);
}
