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

import org.eclipse.collections.api.ShortIterable;
import org.eclipse.collections.api.factory.stack.primitive.ImmutableShortStackFactory;
import org.eclipse.collections.api.stack.primitive.ImmutableShortStack;
import org.eclipse.collections.impl.factory.primitive.ShortStacks;

/**
 * ImmutableShortStackFactoryImpl is a factory implementation which creates instances of type {@link ImmutableShortStack}.
 * This file was automatically generated from template file immutablePrimitiveStackFactoryImpl.stg.
 *
 * @since 4.0.
 */
public class ImmutableShortStackFactoryImpl implements ImmutableShortStackFactory
{
    public static final ImmutableShortStackFactory INSTANCE = new ImmutableShortStackFactoryImpl();

    @Override
    public ImmutableShortStack empty()
    {
        return ImmutableShortEmptyStack.INSTANCE;
    }

    @Override
    public ImmutableShortStack of()
    {
        return this.empty();
    }

    @Override
    public ImmutableShortStack with()
    {
        return this.empty();
    }

    @Override
    public ImmutableShortStack of(short one)
    {
        return this.with(one);
    }

    @Override
    public ImmutableShortStack with(short one)
    {
        return new ImmutableShortSingletonStack(one);
    }

    @Override
    public ImmutableShortStack of(short... items)
    {
        return this.with(items);
    }

    @Override
    public ImmutableShortStack with(short... items)
    {
        if (items == null || items.length == 0)
        {
            return this.with();
        }
        if (items.length == 1)
        {
            return this.with(items[0]);
        }
        return ImmutableShortArrayStack.newStackWith(items);
    }

    @Override
    public ImmutableShortStack ofAll(ShortIterable items)
    {
        return this.withAll(items);
    }

    @Override
    public ImmutableShortStack withAll(ShortIterable items)
    {
        if (items instanceof ImmutableShortStack)
        {
            return (ImmutableShortStack) items;
        }
        return this.with(items.toArray());
    }

    /**
     * @since 10.0
     */
    @Override
    public ImmutableShortStack ofAll(Iterable<Short> iterable)
    {
        return this.withAll(iterable);
    }

    /**
     * @since 10.0
     */
    @Override
    public ImmutableShortStack withAll(Iterable<Short> iterable)
    {
        return ShortStacks.mutable.withAll(iterable).toImmutable();
    }

    @Override
    public ImmutableShortStack ofAllReversed(ShortIterable items)
    {
        return this.withAllReversed(items);
    }

    @Override
    public ImmutableShortStack withAllReversed(ShortIterable items)
    {
        if (items == null || items.isEmpty())
        {
            return this.with();
        }
        if (items.size() == 1)
        {
            return this.with(items.toArray());
        }
        return ImmutableShortArrayStack.newStackFromTopToBottom(items);
    }
}
