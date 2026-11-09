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

import org.eclipse.collections.api.BooleanIterable;
import org.eclipse.collections.api.factory.stack.primitive.ImmutableBooleanStackFactory;
import org.eclipse.collections.api.stack.primitive.ImmutableBooleanStack;
import org.eclipse.collections.impl.factory.primitive.BooleanStacks;

/**
 * ImmutableBooleanStackFactoryImpl is a factory implementation which creates instances of type {@link ImmutableBooleanStack}.
 * This file was automatically generated from template file immutablePrimitiveStackFactoryImpl.stg.
 *
 * @since 4.0.
 */
public class ImmutableBooleanStackFactoryImpl implements ImmutableBooleanStackFactory
{
    public static final ImmutableBooleanStackFactory INSTANCE = new ImmutableBooleanStackFactoryImpl();

    @Override
    public ImmutableBooleanStack empty()
    {
        return ImmutableBooleanEmptyStack.INSTANCE;
    }

    @Override
    public ImmutableBooleanStack of()
    {
        return this.empty();
    }

    @Override
    public ImmutableBooleanStack with()
    {
        return this.empty();
    }

    @Override
    public ImmutableBooleanStack of(boolean one)
    {
        return this.with(one);
    }

    @Override
    public ImmutableBooleanStack with(boolean one)
    {
        return new ImmutableBooleanSingletonStack(one);
    }

    @Override
    public ImmutableBooleanStack of(boolean... items)
    {
        return this.with(items);
    }

    @Override
    public ImmutableBooleanStack with(boolean... items)
    {
        if (items == null || items.length == 0)
        {
            return this.with();
        }
        if (items.length == 1)
        {
            return this.with(items[0]);
        }
        return ImmutableBooleanArrayStack.newStackWith(items);
    }

    @Override
    public ImmutableBooleanStack ofAll(BooleanIterable items)
    {
        return this.withAll(items);
    }

    @Override
    public ImmutableBooleanStack withAll(BooleanIterable items)
    {
        if (items instanceof ImmutableBooleanStack)
        {
            return (ImmutableBooleanStack) items;
        }
        return this.with(items.toArray());
    }

    /**
     * @since 10.0
     */
    @Override
    public ImmutableBooleanStack ofAll(Iterable<Boolean> iterable)
    {
        return this.withAll(iterable);
    }

    /**
     * @since 10.0
     */
    @Override
    public ImmutableBooleanStack withAll(Iterable<Boolean> iterable)
    {
        return BooleanStacks.mutable.withAll(iterable).toImmutable();
    }

    @Override
    public ImmutableBooleanStack ofAllReversed(BooleanIterable items)
    {
        return this.withAllReversed(items);
    }

    @Override
    public ImmutableBooleanStack withAllReversed(BooleanIterable items)
    {
        if (items == null || items.isEmpty())
        {
            return this.with();
        }
        if (items.size() == 1)
        {
            return this.with(items.toArray());
        }
        return ImmutableBooleanArrayStack.newStackFromTopToBottom(items);
    }
}
