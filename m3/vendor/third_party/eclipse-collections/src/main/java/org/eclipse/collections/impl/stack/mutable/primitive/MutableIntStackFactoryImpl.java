/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.stack.mutable.primitive;

import org.eclipse.collections.api.IntIterable;
import org.eclipse.collections.api.factory.stack.primitive.MutableIntStackFactory;
import org.eclipse.collections.api.stack.primitive.MutableIntStack;
import org.eclipse.collections.impl.factory.primitive.IntStacks;
import org.eclipse.collections.impl.utility.Iterate;
import java.util.stream.IntStream;

/**
 * MutableIntStackFactoryImpl is a factory implementation which creates instances of type {@link MutableIntStack}.
 * This file was automatically generated from template file mutablePrimitiveStackFactoryImpl.stg.
 *
 * @since 6.0.
 */
public class MutableIntStackFactoryImpl implements MutableIntStackFactory
{
    public static final MutableIntStackFactory INSTANCE = new MutableIntStackFactoryImpl();

    @Override
    public MutableIntStack empty()
    {
        return new IntArrayStack();
    }

    @Override
    public MutableIntStack of()
    {
        return this.empty();
    }

    @Override
    public MutableIntStack with()
    {
        return this.empty();
    }

    @Override
    public MutableIntStack of(int... items)
    {
        return this.with(items);
    }

    @Override
    public MutableIntStack with(int... items)
    {
        if (items.length == 0)
        {
            return this.empty();
        }
        return IntArrayStack.newStackWith(items);
    }

    @Override
    public MutableIntStack ofAll(IntIterable items)
    {
        return this.withAll(items);
    }

    @Override
    public MutableIntStack withAll(IntIterable items)
    {
        if (items.isEmpty())
        {
            return this.empty();
        }
        return IntArrayStack.newStack(items);
    }

    /**
     * @since 10.0
     */
    @Override
    public MutableIntStack ofAll(Iterable<Integer> iterable)
    {
        return this.withAll(iterable);
    }

    /**
     * @since 10.0
     */
    @Override
    public MutableIntStack withAll(Iterable<Integer> iterable)
    {
        MutableIntStack stack = IntStacks.mutable.empty();
        Iterate.forEach(iterable, stack::push);
        return stack;
    }

    @Override
    public MutableIntStack ofAllReversed(IntIterable items)
    {
        return this.withAllReversed(items);
    }

    @Override
    public MutableIntStack withAllReversed(IntIterable items)
    {
        if (items.isEmpty())
        {
            return this.empty();
        }
        return IntArrayStack.newStackFromTopToBottom(items);
    }

    /**
     * @since 9.0
     */
    @Override
    public MutableIntStack ofAll(IntStream items)
    {
        return this.withAll(items);
    }

    /**
     * @since 9.0
     */
    @Override
    public MutableIntStack withAll(IntStream items)
    {
        return this.with(items.toArray());
    }
}
