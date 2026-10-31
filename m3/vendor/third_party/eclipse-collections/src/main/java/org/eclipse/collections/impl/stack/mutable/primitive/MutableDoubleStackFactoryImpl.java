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

import org.eclipse.collections.api.DoubleIterable;
import org.eclipse.collections.api.factory.stack.primitive.MutableDoubleStackFactory;
import org.eclipse.collections.api.stack.primitive.MutableDoubleStack;
import org.eclipse.collections.impl.factory.primitive.DoubleStacks;
import org.eclipse.collections.impl.utility.Iterate;
import java.util.stream.DoubleStream;

/**
 * MutableDoubleStackFactoryImpl is a factory implementation which creates instances of type {@link MutableDoubleStack}.
 * This file was automatically generated from template file mutablePrimitiveStackFactoryImpl.stg.
 *
 * @since 6.0.
 */
public class MutableDoubleStackFactoryImpl implements MutableDoubleStackFactory
{
    public static final MutableDoubleStackFactory INSTANCE = new MutableDoubleStackFactoryImpl();

    @Override
    public MutableDoubleStack empty()
    {
        return new DoubleArrayStack();
    }

    @Override
    public MutableDoubleStack of()
    {
        return this.empty();
    }

    @Override
    public MutableDoubleStack with()
    {
        return this.empty();
    }

    @Override
    public MutableDoubleStack of(double... items)
    {
        return this.with(items);
    }

    @Override
    public MutableDoubleStack with(double... items)
    {
        if (items.length == 0)
        {
            return this.empty();
        }
        return DoubleArrayStack.newStackWith(items);
    }

    @Override
    public MutableDoubleStack ofAll(DoubleIterable items)
    {
        return this.withAll(items);
    }

    @Override
    public MutableDoubleStack withAll(DoubleIterable items)
    {
        if (items.isEmpty())
        {
            return this.empty();
        }
        return DoubleArrayStack.newStack(items);
    }

    /**
     * @since 10.0
     */
    @Override
    public MutableDoubleStack ofAll(Iterable<Double> iterable)
    {
        return this.withAll(iterable);
    }

    /**
     * @since 10.0
     */
    @Override
    public MutableDoubleStack withAll(Iterable<Double> iterable)
    {
        MutableDoubleStack stack = DoubleStacks.mutable.empty();
        Iterate.forEach(iterable, stack::push);
        return stack;
    }

    @Override
    public MutableDoubleStack ofAllReversed(DoubleIterable items)
    {
        return this.withAllReversed(items);
    }

    @Override
    public MutableDoubleStack withAllReversed(DoubleIterable items)
    {
        if (items.isEmpty())
        {
            return this.empty();
        }
        return DoubleArrayStack.newStackFromTopToBottom(items);
    }

    /**
     * @since 9.0
     */
    @Override
    public MutableDoubleStack ofAll(DoubleStream items)
    {
        return this.withAll(items);
    }

    /**
     * @since 9.0
     */
    @Override
    public MutableDoubleStack withAll(DoubleStream items)
    {
        return this.with(items.toArray());
    }
}
