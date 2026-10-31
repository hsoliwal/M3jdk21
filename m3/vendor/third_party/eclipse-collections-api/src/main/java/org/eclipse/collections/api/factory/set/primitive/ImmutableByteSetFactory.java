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

import org.eclipse.collections.api.ByteIterable;
import org.eclipse.collections.api.set.primitive.ImmutableByteSet;

/**
 * A factory which creates instances of type {@link ImmutableByteSet}.
 * This file was automatically generated from template file immutablePrimitiveSetFactory.stg.
 *
 * @since 4.0.
 */
public interface ImmutableByteSetFactory
{
    /**
     * @since 6.0
     */
    ImmutableByteSet empty();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableByteSet of();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableByteSet with();

    /**
     * Same as {@link #with(byte)}.
     */
    ImmutableByteSet of(byte one);

    ImmutableByteSet with(byte one);

    /**
     * Same as {@link #with(byte[])}.
     */
    ImmutableByteSet of(byte... items);

    ImmutableByteSet with(byte... items);

    /**
     * Same as {@link #withAll(ByteIterable)}.
     */
    ImmutableByteSet ofAll(ByteIterable items);

    ImmutableByteSet withAll(ByteIterable items);

    /**
     * Same as {@link #withAll(Iterable)}.
     */
    ImmutableByteSet ofAll(Iterable<Byte> iterable);

    ImmutableByteSet withAll(Iterable<Byte> iterable);
}
