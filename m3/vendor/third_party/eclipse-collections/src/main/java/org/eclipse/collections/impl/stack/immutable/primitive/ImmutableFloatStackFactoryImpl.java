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

import org.eclipse.collections.api.FloatIterable;
import org.eclipse.collections.api.factory.stack.primitive.ImmutableFloatStackFactory;
import org.eclipse.collections.api.stack.primitive.ImmutableFloatStack;
import org.eclipse.collections.impl.factory.primitive.FloatStacks;

/**
 * ImmutableFloatStackFactoryImpl is a factory implementation which creates instances of type {@link ImmutableFloatStack}.
 * This file was automatically generated from template file immutablePrimitiveStackFactoryImpl.stg.
 *
 * @since 4.0.
 */
public class ImmutableFloatStackFactoryImpl implements ImmutableFloatStackFactory
{
    public static final ImmutableFloatStackFactory INSTANCE = new ImmutableFloatStackFactoryImpl();

    @Override
    public ImmutableFloatStack empty()
    {
        return ImmutableFloatEmptyStack.INSTANCE;
    }

    @Override
    public ImmutableFloatStack of()
    {
        return this.empty();
    }

    @Override
    public ImmutableFloatStack with()
    {
        return this.empty();
    }

    @Override
    public ImmutableFloatStack of(float one)
    {
        return this.with(one);
    }

    @Override
    public ImmutableFloatStack with(float one)
    {
        return new ImmutableFloatSingletonStack(one);
    }

    @Override
    public ImmutableFloatStack of(float... items)
    {
        return this.with(items);
    }

    @Override
    public ImmutableFloatStack with(float... items)
    {
        if (items == null || items.length == 0)
        {
            return this.with();
        }
        if (items.length == 1)
        {
            return this.with(items[0]);
        }
        return ImmutableFloatArrayStack.newStackWith(items);
    }

    @Override
    public ImmutableFloatStack ofAll(FloatIterable items)
    {
        return this.withAll(items);
    }

    @Override
    public ImmutableFloatStack withAll(FloatIterable items)
    {
        if (items instanceof ImmutableFloatStack)
        {
            return (ImmutableFloatStack) items;
        }
        return this.with(items.toArray());
    }

    /**
     * @since 10.0
     */
    @Override
    public ImmutableFloatStack ofAll(Iterable<Float> iterable)
    {
        return this.withAll(iterable);
    }

    /**
     * @since 10.0
     */
    @Override
    public ImmutableFloatStack withAll(Iterable<Float> iterable)
    {
        return FloatStacks.mutable.withAll(iterable).toImmutable();
    }

    @Override
    public ImmutableFloatStack ofAllReversed(FloatIterable items)
    {
        return this.withAllReversed(items);
    }

    @Override
    public ImmutableFloatStack withAllReversed(FloatIterable items)
    {
        if (items == null || items.isEmpty())
        {
            return this.with();
        }
        if (items.size() == 1)
        {
            return this.with(items.toArray());
        }
        return ImmutableFloatArrayStack.newStackFromTopToBottom(items);
    }
}
