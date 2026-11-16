/*
 * Copyright (c) 2026 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.impl.stack.mutable.primitive;

import jdk.m3.collections.eclipse.api.LongIterable;
import jdk.m3.collections.eclipse.api.factory.stack.primitive.MutableLongStackFactory;
import jdk.m3.collections.eclipse.api.stack.primitive.MutableLongStack;
import jdk.m3.collections.eclipse.impl.factory.primitive.LongStacks;
import jdk.m3.collections.eclipse.impl.utility.Iterate;
import java.util.stream.LongStream;

/**
 * MutableLongStackFactoryImpl is a factory implementation which creates instances of type {@link MutableLongStack}.
 * This file was automatically generated from template file mutablePrimitiveStackFactoryImpl.stg.
 *
 * @since 6.0.
 */
@SuppressWarnings("missing-explicit-ctor")
public class MutableLongStackFactoryImpl implements MutableLongStackFactory
{
    public static final MutableLongStackFactory INSTANCE = new MutableLongStackFactoryImpl();

    @Override
    public MutableLongStack empty()
    {
        return new LongArrayStack();
    }

    @Override
    public MutableLongStack of()
    {
        return this.empty();
    }

    @Override
    public MutableLongStack with()
    {
        return this.empty();
    }

    @Override
    public MutableLongStack of(long... items)
    {
        return this.with(items);
    }

    @Override
    public MutableLongStack with(long... items)
    {
        if (items.length == 0)
        {
            return this.empty();
        }
        return LongArrayStack.newStackWith(items);
    }

    @Override
    public MutableLongStack ofAll(LongIterable items)
    {
        return this.withAll(items);
    }

    @Override
    public MutableLongStack withAll(LongIterable items)
    {
        if (items.isEmpty())
        {
            return this.empty();
        }
        return LongArrayStack.newStack(items);
    }

    /**
     * @since 10.0
     */
    @Override
    public MutableLongStack ofAll(Iterable<Long> iterable)
    {
        return this.withAll(iterable);
    }

    /**
     * @since 10.0
     */
    @Override
    public MutableLongStack withAll(Iterable<Long> iterable)
    {
        MutableLongStack stack = LongStacks.mutable.empty();
        Iterate.forEach(iterable, stack::push);
        return stack;
    }

    @Override
    public MutableLongStack ofAllReversed(LongIterable items)
    {
        return this.withAllReversed(items);
    }

    @Override
    public MutableLongStack withAllReversed(LongIterable items)
    {
        if (items.isEmpty())
        {
            return this.empty();
        }
        return LongArrayStack.newStackFromTopToBottom(items);
    }

    /**
     * @since 9.0
     */
    @Override
    public MutableLongStack ofAll(LongStream items)
    {
        return this.withAll(items);
    }

    /**
     * @since 9.0
     */
    @Override
    public MutableLongStack withAll(LongStream items)
    {
        return this.with(items.toArray());
    }
}
