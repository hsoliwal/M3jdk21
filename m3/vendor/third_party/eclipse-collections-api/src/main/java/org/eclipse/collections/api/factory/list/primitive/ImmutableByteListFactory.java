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

import org.eclipse.collections.api.ByteIterable;
import org.eclipse.collections.api.list.primitive.ImmutableByteList;

/**
 * A factory which creates instances of type {@link ImmutableByteList}.
 * This file was automatically generated from template file immutablePrimitiveListFactory.stg.
 *
 * @since 3.2.
 */
public interface ImmutableByteListFactory
{
    /**
     * @since 6.0
     */
    ImmutableByteList empty();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableByteList of();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableByteList with();

    /**
     * Same as {@link #with(byte)}.
     */
    ImmutableByteList of(byte one);

    ImmutableByteList with(byte one);

    /**
     * Same as {@link #with(byte[])}.
     */
    ImmutableByteList of(byte... items);

    ImmutableByteList with(byte... items);

    /**
     * Same as {@link #withAll(ByteIterable)}.
     */
    ImmutableByteList ofAll(ByteIterable items);

    ImmutableByteList withAll(ByteIterable items);

    /**
     * Same as {@link #withAll(Iterable)}.
     */
    ImmutableByteList ofAll(Iterable<Byte> iterable);

    ImmutableByteList withAll(Iterable<Byte> iterable);
}
