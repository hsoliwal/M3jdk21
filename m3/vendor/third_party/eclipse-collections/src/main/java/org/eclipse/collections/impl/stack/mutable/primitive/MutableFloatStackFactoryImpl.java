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

import org.eclipse.collections.api.FloatIterable;
import org.eclipse.collections.api.factory.stack.primitive.MutableFloatStackFactory;
import org.eclipse.collections.api.stack.primitive.MutableFloatStack;
import org.eclipse.collections.impl.factory.primitive.FloatStacks;
import org.eclipse.collections.impl.utility.Iterate;

/**
 * MutableFloatStackFactoryImpl is a factory implementation which creates instances of type {@link MutableFloatStack}.
 * This file was automatically generated from template file mutablePrimitiveStackFactoryImpl.stg.
 *
 * @since 6.0.
 */
public class MutableFloatStackFactoryImpl implements MutableFloatStackFactory
{
    public static final MutableFloatStackFactory INSTANCE = new MutableFloatStackFactoryImpl();

    @Override
    public MutableFloatStack empty()
    {
        return new FloatArrayStack();
    }

    @Override
    public MutableFloatStack of()
    {
        return this.empty();
    }

    @Override
    public MutableFloatStack with()
    {
        return this.empty();
    }

    @Override
    public MutableFloatStack of(float... items)
    {
        return this.with(items);
    }

    @Override
    public MutableFloatStack with(float... items)
    {
        if (items.length == 0)
        {
            return this.empty();
        }
        return FloatArrayStack.newStackWith(items);
    }

    @Override
    public MutableFloatStack ofAll(FloatIterable items)
    {
        return this.withAll(items);
    }

    @Override
    public MutableFloatStack withAll(FloatIterable items)
    {
        if (items.isEmpty())
        {
            return this.empty();
        }
        return FloatArrayStack.newStack(items);
    }

    /**
     * @since 10.0
     */
    @Override
    public MutableFloatStack ofAll(Iterable<Float> iterable)
    {
        return this.withAll(iterable);
    }

    /**
     * @since 10.0
     */
    @Override
    public MutableFloatStack withAll(Iterable<Float> iterable)
    {
        MutableFloatStack stack = FloatStacks.mutable.empty();
        Iterate.forEach(iterable, stack::push);
        return stack;
    }

    @Override
    public MutableFloatStack ofAllReversed(FloatIterable items)
    {
        return this.withAllReversed(items);
    }

    @Override
    public MutableFloatStack withAllReversed(FloatIterable items)
    {
        if (items.isEmpty())
        {
            return this.empty();
        }
        return FloatArrayStack.newStackFromTopToBottom(items);
    }
}
