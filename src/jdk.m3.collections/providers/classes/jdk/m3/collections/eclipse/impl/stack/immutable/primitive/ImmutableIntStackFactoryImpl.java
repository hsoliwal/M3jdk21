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

import jdk.m3.collections.eclipse.api.IntIterable;
import jdk.m3.collections.eclipse.api.factory.stack.primitive.ImmutableIntStackFactory;
import jdk.m3.collections.eclipse.api.stack.primitive.ImmutableIntStack;
import jdk.m3.collections.eclipse.impl.factory.primitive.IntStacks;
import java.util.stream.IntStream;

/**
 * ImmutableIntStackFactoryImpl is a factory implementation which creates instances of type {@link ImmutableIntStack}.
 * This file was automatically generated from template file immutablePrimitiveStackFactoryImpl.stg.
 *
 * @since 4.0.
 */
@SuppressWarnings("missing-explicit-ctor")
public class ImmutableIntStackFactoryImpl implements ImmutableIntStackFactory
{
    public static final ImmutableIntStackFactory INSTANCE = new ImmutableIntStackFactoryImpl();

    @Override
    public ImmutableIntStack empty()
    {
        return ImmutableIntEmptyStack.INSTANCE;
    }

    @Override
    public ImmutableIntStack of()
    {
        return this.empty();
    }

    @Override
    public ImmutableIntStack with()
    {
        return this.empty();
    }

    @Override
    public ImmutableIntStack of(int one)
    {
        return this.with(one);
    }

    @Override
    public ImmutableIntStack with(int one)
    {
        return new ImmutableIntSingletonStack(one);
    }

    @Override
    public ImmutableIntStack of(int... items)
    {
        return this.with(items);
    }

    @Override
    public ImmutableIntStack with(int... items)
    {
        if (items == null || items.length == 0)
        {
            return this.with();
        }
        if (items.length == 1)
        {
            return this.with(items[0]);
        }
        return ImmutableIntArrayStack.newStackWith(items);
    }

    @Override
    public ImmutableIntStack ofAll(IntIterable items)
    {
        return this.withAll(items);
    }

    @Override
    public ImmutableIntStack withAll(IntIterable items)
    {
        if (items instanceof ImmutableIntStack)
        {
            return (ImmutableIntStack) items;
        }
        return this.with(items.toArray());
    }

    /**
     * @since 10.0
     */
    @Override
    public ImmutableIntStack ofAll(Iterable<Integer> iterable)
    {
        return this.withAll(iterable);
    }

    /**
     * @since 10.0
     */
    @Override
    public ImmutableIntStack withAll(Iterable<Integer> iterable)
    {
        return IntStacks.mutable.withAll(iterable).toImmutable();
    }

    @Override
    public ImmutableIntStack ofAllReversed(IntIterable items)
    {
        return this.withAllReversed(items);
    }

    @Override
    public ImmutableIntStack withAllReversed(IntIterable items)
    {
        if (items == null || items.isEmpty())
        {
            return this.with();
        }
        if (items.size() == 1)
        {
            return this.with(items.toArray());
        }
        return ImmutableIntArrayStack.newStackFromTopToBottom(items);
    }

    /**
     * @since 9.0
     */
    @Override
    public ImmutableIntStack ofAll(IntStream items)
    {
        return this.withAll(items);
    }

    /**
     * @since 9.0
     */
    @Override
    public ImmutableIntStack withAll(IntStream items)
    {
        return this.with(items.toArray());
    }
}
