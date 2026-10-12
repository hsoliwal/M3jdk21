/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.stack.immutable.primitive;

import org.eclipse.collections.api.ByteIterable;
import org.eclipse.collections.api.factory.stack.primitive.ImmutableByteStackFactory;
import org.eclipse.collections.api.stack.primitive.ImmutableByteStack;
import org.eclipse.collections.impl.factory.primitive.ByteStacks;

/**
 * ImmutableByteStackFactoryImpl is a factory implementation which creates instances of type {@link ImmutableByteStack}.
 * This file was automatically generated from template file immutablePrimitiveStackFactoryImpl.stg.
 *
 * @since 4.0.
 */
public class ImmutableByteStackFactoryImpl implements ImmutableByteStackFactory
{
    public static final ImmutableByteStackFactory INSTANCE = new ImmutableByteStackFactoryImpl();

    @Override
    public ImmutableByteStack empty()
    {
        return ImmutableByteEmptyStack.INSTANCE;
    }

    @Override
    public ImmutableByteStack of()
    {
        return this.empty();
    }

    @Override
    public ImmutableByteStack with()
    {
        return this.empty();
    }

    @Override
    public ImmutableByteStack of(byte one)
    {
        return this.with(one);
    }

    @Override
    public ImmutableByteStack with(byte one)
    {
        return new ImmutableByteSingletonStack(one);
    }

    @Override
    public ImmutableByteStack of(byte... items)
    {
        return this.with(items);
    }

    @Override
    public ImmutableByteStack with(byte... items)
    {
        if (items == null || items.length == 0)
        {
            return this.with();
        }
        if (items.length == 1)
        {
            return this.with(items[0]);
        }
        return ImmutableByteArrayStack.newStackWith(items);
    }

    @Override
    public ImmutableByteStack ofAll(ByteIterable items)
    {
        return this.withAll(items);
    }

    @Override
    public ImmutableByteStack withAll(ByteIterable items)
    {
        if (items instanceof ImmutableByteStack)
        {
            return (ImmutableByteStack) items;
        }
        return this.with(items.toArray());
    }

    /**
     * @since 10.0
     */
    @Override
    public ImmutableByteStack ofAll(Iterable<Byte> iterable)
    {
        return this.withAll(iterable);
    }

    /**
     * @since 10.0
     */
    @Override
    public ImmutableByteStack withAll(Iterable<Byte> iterable)
    {
        return ByteStacks.mutable.withAll(iterable).toImmutable();
    }

    @Override
    public ImmutableByteStack ofAllReversed(ByteIterable items)
    {
        return this.withAllReversed(items);
    }

    @Override
    public ImmutableByteStack withAllReversed(ByteIterable items)
    {
        if (items == null || items.isEmpty())
        {
            return this.with();
        }
        if (items.size() == 1)
        {
            return this.with(items.toArray());
        }
        return ImmutableByteArrayStack.newStackFromTopToBottom(items);
    }
}
