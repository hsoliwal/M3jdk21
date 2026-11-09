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
import org.eclipse.collections.api.set.primitive.MutableByteSet;

/**
 * A factory which creates instances of type {@link MutableByteSet}.
 * This file was automatically generated from template file mutablePrimitiveSetFactory.stg.
 *
 * @since 6.0.
 */
public interface MutableByteSetFactory
{
    MutableByteSet empty();

    /**
     * Same as {@link #empty()}.
     */
    MutableByteSet of();

    /**
     * Same as {@link #empty()}.
     */
    MutableByteSet with();

    /**
     * Same as {@link #with(byte[])}.
     */
    MutableByteSet of(byte... items);

    MutableByteSet with(byte... items);

    /**
     * Same as {@link #withAll(ByteIterable)}.
     */
    MutableByteSet ofAll(ByteIterable items);

    MutableByteSet withAll(ByteIterable items);

    /**
     * Same as {@link #withAll(Iterable)}.
     */
    MutableByteSet ofAll(Iterable<Byte> iterable);

    MutableByteSet withAll(Iterable<Byte> iterable);
}
