/*
 * Copyright (c) 2026 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.impl.list.immutable.primitive;

import jdk.m3.collections.eclipse.api.ByteIterable;
import jdk.m3.collections.eclipse.api.factory.list.primitive.ImmutableByteListFactory;
import jdk.m3.collections.eclipse.api.list.primitive.ImmutableByteList;
import jdk.m3.collections.eclipse.impl.factory.primitive.ByteLists;

/**
 * ImmutableByteListFactoryImpl is a factory implementation which creates instances of type {@link ImmutableByteList}.
 * This file was automatically generated from template file immutablePrimitiveListFactoryImpl.stg.
 *
 * @since 3.2.
 */
@SuppressWarnings("missing-explicit-ctor")
public class ImmutableByteListFactoryImpl implements ImmutableByteListFactory
{
    public static final ImmutableByteListFactory INSTANCE = new ImmutableByteListFactoryImpl();

    @Override
    public ImmutableByteList empty()
    {
        return ImmutableByteEmptyList.INSTANCE;
    }

    @Override
    public ImmutableByteList of()
    {
        return this.empty();
    }

    @Override
    public ImmutableByteList with()
    {
        return this.empty();
    }

    @Override
    public ImmutableByteList of(byte one)
    {
        return this.with(one);
    }

    @Override
    public ImmutableByteList with(byte one)
    {
        return new ImmutableByteSingletonList(one);
    }

    @Override
    public ImmutableByteList of(byte... items)
    {
        return this.with(items);
    }

    @Override
    public ImmutableByteList with(byte... items)
    {
        if (items == null || items.length == 0)
        {
            return this.with();
        }
        if (items.length == 1)
        {
            return this.with(items[0]);
        }
        return ImmutableByteArrayList.newListWith(items);
    }

    @Override
    public ImmutableByteList ofAll(ByteIterable items)
    {
        return this.withAll(items);
    }

    @Override
    public ImmutableByteList withAll(ByteIterable items)
    {
        if (items instanceof ImmutableByteList)
        {
            return (ImmutableByteList) items;
        }
        if (items == null || items.size() == 0)
        {
            return this.with();
        }
        if (items.size() == 1)
        {
            return this.with(items.toArray()[0]);
        }
        return ImmutableByteArrayList.newList(items);
    }

    /**
     * @since 10.0
     */
    @Override
    public ImmutableByteList ofAll(Iterable<Byte> iterable)
    {
        return this.withAll(iterable);
    }

    /**
     * @since 10.0
     */
    @Override
    public ImmutableByteList withAll(Iterable<Byte> iterable)
    {
        return ByteLists.mutable.withAll(iterable).toImmutable();
    }
}
