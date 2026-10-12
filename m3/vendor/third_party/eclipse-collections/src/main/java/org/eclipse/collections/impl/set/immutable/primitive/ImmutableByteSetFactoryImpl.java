/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.set.immutable.primitive;

import org.eclipse.collections.api.ByteIterable;
import org.eclipse.collections.api.factory.set.primitive.ImmutableByteSetFactory;
import org.eclipse.collections.api.set.primitive.ImmutableByteSet;
import org.eclipse.collections.impl.set.mutable.primitive.ByteHashSet;
import org.eclipse.collections.impl.factory.primitive.ByteSets;

/**
 * ImmutableByteSetFactoryImpl is a factory implementation which creates instances of type {@link ImmutableByteSet}.
 * This file was automatically generated from template file immutablePrimitiveSetFactoryImpl.stg.
 *
 * @since 4.0.
 */
public class ImmutableByteSetFactoryImpl implements ImmutableByteSetFactory
{
    public static final ImmutableByteSetFactory INSTANCE = new ImmutableByteSetFactoryImpl();

    @Override
    public ImmutableByteSet empty()
    {
        return ImmutableByteEmptySet.INSTANCE;
    }

    @Override
    public ImmutableByteSet of()
    {
        return this.empty();
    }

    @Override
    public ImmutableByteSet with()
    {
        return this.empty();
    }

    @Override
    public ImmutableByteSet of(byte one)
    {
        return this.with(one);
    }

    @Override
    public ImmutableByteSet with(byte one)
    {
        return new ImmutableByteSingletonSet(one);
    }

    @Override
    public ImmutableByteSet of(byte... items)
    {
        return this.with(items);
    }

    @Override
    public ImmutableByteSet with(byte... items)
    {
        if (items == null || items.length == 0)
        {
            return this.with();
        }
        if (items.length == 1)
        {
            return this.with(items[0]);
        }
        return ByteHashSet.newSetWith(items).toImmutable();
    }

    @Override
    public ImmutableByteSet ofAll(ByteIterable items)
    {
        return this.withAll(items);
    }

    @Override
    public ImmutableByteSet withAll(ByteIterable items)
    {
        if (items instanceof ImmutableByteSet)
        {
            return (ImmutableByteSet) items;
        }
        return this.with(items.toArray());
    }

    /**
     * @since 10.0
     */
    @Override
    public ImmutableByteSet ofAll(Iterable<Byte> iterable)
    {
        return this.withAll(iterable);
    }

    /**
     * @since 10.0
     */
    @Override
    public ImmutableByteSet withAll(Iterable<Byte> iterable)
    {
        return ByteSets.mutable.withAll(iterable).toImmutable();
    }
}
