/*
 * Copyright (c) 2026 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.impl.bag.immutable.primitive;

import jdk.m3.collections.eclipse.api.ByteIterable;
import jdk.m3.collections.eclipse.api.bag.primitive.ImmutableByteBag;
import jdk.m3.collections.eclipse.api.bag.primitive.MutableByteBag;
import jdk.m3.collections.eclipse.api.factory.bag.primitive.ImmutableByteBagFactory;
import jdk.m3.collections.eclipse.impl.bag.mutable.primitive.ByteHashBag;
import jdk.m3.collections.eclipse.impl.factory.primitive.ByteBags;

/**
 * ImmutableByteBagFactoryImpl is a factory implementation which creates instances of type {@link ImmutableByteBag}.
 * This file was automatically generated from template file immutablePrimitiveBagFactoryImpl.stg.
 *
 * @since 4.0.
 */
@SuppressWarnings("missing-explicit-ctor")
public class ImmutableByteBagFactoryImpl implements ImmutableByteBagFactory
{
    public static final ImmutableByteBagFactory INSTANCE = new ImmutableByteBagFactoryImpl();

    @Override
    public ImmutableByteBag empty()
    {
        return ImmutableByteEmptyBag.INSTANCE;
    }

    @Override
    public ImmutableByteBag of()
    {
        return this.empty();
    }

    @Override
    public ImmutableByteBag with()
    {
        return this.empty();
    }

    @Override
    public ImmutableByteBag of(byte one)
    {
        return this.with(one);
    }

    @Override
    public ImmutableByteBag with(byte one)
    {
        return new ImmutableByteSingletonBag(one);
    }

    @Override
    public ImmutableByteBag of(byte... items)
    {
        return this.with(items);
    }

    @Override
    public ImmutableByteBag with(byte... items)
    {
        if (items == null || items.length == 0)
        {
            return this.with();
        }
        if (items.length == 1)
        {
            return this.with(items[0]);
        }
        return ImmutableByteHashBag.newBagWith(items);
    }

    @Override
    public ImmutableByteBag ofAll(ByteIterable items)
    {
        return this.withAll(items);
    }

    @Override
    public ImmutableByteBag withAll(ByteIterable items)
    {
        if (items instanceof ImmutableByteBag)
        {
            return (ImmutableByteBag) items;
        }
        if (items == null)
        {
            return this.with();
        }
        MutableByteBag bag = new ByteHashBag();
        items.forEach(bag::add);
        if (bag.size() == 0)
        {
            return this.with();
        }
        if (bag.size() == 1)
        {
            return this.with(bag.toArray());
        }
        return ImmutableByteHashBag.newBagWith(bag);
    }

    /**
     * @since 10.0
     */
    @Override
    public ImmutableByteBag ofAll(Iterable<Byte> iterable)
    {
        return this.withAll(iterable);
    }

    /**
     * @since 10.0
     */
    @Override
    public ImmutableByteBag withAll(Iterable<Byte> iterable)
    {
        return ByteBags.mutable.withAll(iterable).toImmutable();
    }
}
