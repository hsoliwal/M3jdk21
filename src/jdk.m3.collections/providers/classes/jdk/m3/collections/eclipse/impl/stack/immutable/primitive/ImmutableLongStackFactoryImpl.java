/*
 * Copyright (c) 2026 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.impl.stack.immutable.primitive;

import jdk.m3.collections.eclipse.api.LongIterable;
import jdk.m3.collections.eclipse.api.factory.stack.primitive.ImmutableLongStackFactory;
import jdk.m3.collections.eclipse.api.stack.primitive.ImmutableLongStack;
import jdk.m3.collections.eclipse.impl.factory.primitive.LongStacks;
import java.util.stream.LongStream;

/**
 * ImmutableLongStackFactoryImpl is a factory implementation which creates instances of type {@link ImmutableLongStack}.
 * This file was automatically generated from template file immutablePrimitiveStackFactoryImpl.stg.
 *
 * @since 4.0.
 */
@SuppressWarnings("missing-explicit-ctor")
public class ImmutableLongStackFactoryImpl implements ImmutableLongStackFactory
{
    public static final ImmutableLongStackFactory INSTANCE = new ImmutableLongStackFactoryImpl();

    @Override
    public ImmutableLongStack empty()
    {
        return ImmutableLongEmptyStack.INSTANCE;
    }

    @Override
    public ImmutableLongStack of()
    {
        return this.empty();
    }

    @Override
    public ImmutableLongStack with()
    {
        return this.empty();
    }

    @Override
    public ImmutableLongStack of(long one)
    {
        return this.with(one);
    }

    @Override
    public ImmutableLongStack with(long one)
    {
        return new ImmutableLongSingletonStack(one);
    }

    @Override
    public ImmutableLongStack of(long... items)
    {
        return this.with(items);
    }

    @Override
    public ImmutableLongStack with(long... items)
    {
        if (items == null || items.length == 0)
        {
            return this.with();
        }
        if (items.length == 1)
        {
            return this.with(items[0]);
        }
        return ImmutableLongArrayStack.newStackWith(items);
    }

    @Override
    public ImmutableLongStack ofAll(LongIterable items)
    {
        return this.withAll(items);
    }

    @Override
    public ImmutableLongStack withAll(LongIterable items)
    {
        if (items instanceof ImmutableLongStack)
        {
            return (ImmutableLongStack) items;
        }
        return this.with(items.toArray());
    }

    /**
     * @since 10.0
     */
    @Override
    public ImmutableLongStack ofAll(Iterable<Long> iterable)
    {
        return this.withAll(iterable);
    }

    /**
     * @since 10.0
     */
    @Override
    public ImmutableLongStack withAll(Iterable<Long> iterable)
    {
        return LongStacks.mutable.withAll(iterable).toImmutable();
    }

    @Override
    public ImmutableLongStack ofAllReversed(LongIterable items)
    {
        return this.withAllReversed(items);
    }

    @Override
    public ImmutableLongStack withAllReversed(LongIterable items)
    {
        if (items == null || items.isEmpty())
        {
            return this.with();
        }
        if (items.size() == 1)
        {
            return this.with(items.toArray());
        }
        return ImmutableLongArrayStack.newStackFromTopToBottom(items);
    }

    /**
     * @since 9.0
     */
    @Override
    public ImmutableLongStack ofAll(LongStream items)
    {
        return this.withAll(items);
    }

    /**
     * @since 9.0
     */
    @Override
    public ImmutableLongStack withAll(LongStream items)
    {
        return this.with(items.toArray());
    }
}
