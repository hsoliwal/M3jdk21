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

import org.eclipse.collections.api.DoubleIterable;
import org.eclipse.collections.api.factory.stack.primitive.ImmutableDoubleStackFactory;
import org.eclipse.collections.api.stack.primitive.ImmutableDoubleStack;
import org.eclipse.collections.impl.factory.primitive.DoubleStacks;
import java.util.stream.DoubleStream;

/**
 * ImmutableDoubleStackFactoryImpl is a factory implementation which creates instances of type {@link ImmutableDoubleStack}.
 * This file was automatically generated from template file immutablePrimitiveStackFactoryImpl.stg.
 *
 * @since 4.0.
 */
public class ImmutableDoubleStackFactoryImpl implements ImmutableDoubleStackFactory
{
    public static final ImmutableDoubleStackFactory INSTANCE = new ImmutableDoubleStackFactoryImpl();

    @Override
    public ImmutableDoubleStack empty()
    {
        return ImmutableDoubleEmptyStack.INSTANCE;
    }

    @Override
    public ImmutableDoubleStack of()
    {
        return this.empty();
    }

    @Override
    public ImmutableDoubleStack with()
    {
        return this.empty();
    }

    @Override
    public ImmutableDoubleStack of(double one)
    {
        return this.with(one);
    }

    @Override
    public ImmutableDoubleStack with(double one)
    {
        return new ImmutableDoubleSingletonStack(one);
    }

    @Override
    public ImmutableDoubleStack of(double... items)
    {
        return this.with(items);
    }

    @Override
    public ImmutableDoubleStack with(double... items)
    {
        if (items == null || items.length == 0)
        {
            return this.with();
        }
        if (items.length == 1)
        {
            return this.with(items[0]);
        }
        return ImmutableDoubleArrayStack.newStackWith(items);
    }

    @Override
    public ImmutableDoubleStack ofAll(DoubleIterable items)
    {
        return this.withAll(items);
    }

    @Override
    public ImmutableDoubleStack withAll(DoubleIterable items)
    {
        if (items instanceof ImmutableDoubleStack)
        {
            return (ImmutableDoubleStack) items;
        }
        return this.with(items.toArray());
    }

    /**
     * @since 10.0
     */
    @Override
    public ImmutableDoubleStack ofAll(Iterable<Double> iterable)
    {
        return this.withAll(iterable);
    }

    /**
     * @since 10.0
     */
    @Override
    public ImmutableDoubleStack withAll(Iterable<Double> iterable)
    {
        return DoubleStacks.mutable.withAll(iterable).toImmutable();
    }

    @Override
    public ImmutableDoubleStack ofAllReversed(DoubleIterable items)
    {
        return this.withAllReversed(items);
    }

    @Override
    public ImmutableDoubleStack withAllReversed(DoubleIterable items)
    {
        if (items == null || items.isEmpty())
        {
            return this.with();
        }
        if (items.size() == 1)
        {
            return this.with(items.toArray());
        }
        return ImmutableDoubleArrayStack.newStackFromTopToBottom(items);
    }

    /**
     * @since 9.0
     */
    @Override
    public ImmutableDoubleStack ofAll(DoubleStream items)
    {
        return this.withAll(items);
    }

    /**
     * @since 9.0
     */
    @Override
    public ImmutableDoubleStack withAll(DoubleStream items)
    {
        return this.with(items.toArray());
    }
}
