/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.set.mutable.primitive;

import org.eclipse.collections.api.ByteIterable;
import org.eclipse.collections.api.factory.set.primitive.MutableByteSetFactory;
import org.eclipse.collections.api.set.primitive.MutableByteSet;
import org.eclipse.collections.impl.factory.primitive.ByteSets;
import org.eclipse.collections.impl.utility.Iterate;

/**
 * MutableByteSetFactoryImpl is a factory implementation which creates instances of type {@link MutableByteSet}.
 * This file was automatically generated from template file mutablePrimitiveSetFactoryImpl.stg.
 *
 * @since 6.0.
 */
public class MutableByteSetFactoryImpl implements MutableByteSetFactory
{
    public static final MutableByteSetFactory INSTANCE = new MutableByteSetFactoryImpl();

    @Override
    public MutableByteSet empty()
    {
        return new ByteHashSet();
    }

    @Override
    public MutableByteSet of()
    {
        return this.empty();
    }

    @Override
    public MutableByteSet with()
    {
        return this.empty();
    }

    @Override
    public MutableByteSet of(byte... items)
    {
        return this.with(items);
    }

    @Override
    public MutableByteSet with(byte... items)
    {
        if (items == null || items.length == 0)
        {
            return this.empty();
        }
        return ByteHashSet.newSetWith(items);
    }

    @Override
    public MutableByteSet ofAll(ByteIterable items)
    {
        return this.withAll(items);
    }

    @Override
    public MutableByteSet withAll(ByteIterable items)
    {
        return ByteHashSet.newSet(items);
    }

    /**
     * @since 10.0
     */
    @Override
    public MutableByteSet ofAll(Iterable<Byte> iterable)
    {
        return this.withAll(iterable);
    }

    /**
     * @since 10.0
     */
    @Override
    public MutableByteSet withAll(Iterable<Byte> iterable)
    {
        return Iterate.collectByte(iterable, Byte::byteValue, ByteSets.mutable.empty());
    }
}
