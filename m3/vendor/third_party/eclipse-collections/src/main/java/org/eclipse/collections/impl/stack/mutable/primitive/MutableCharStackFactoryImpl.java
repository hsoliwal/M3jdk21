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

import org.eclipse.collections.api.CharIterable;
import org.eclipse.collections.api.factory.stack.primitive.MutableCharStackFactory;
import org.eclipse.collections.api.stack.primitive.MutableCharStack;
import org.eclipse.collections.impl.factory.primitive.CharStacks;
import org.eclipse.collections.impl.utility.Iterate;

/**
 * MutableCharStackFactoryImpl is a factory implementation which creates instances of type {@link MutableCharStack}.
 * This file was automatically generated from template file mutablePrimitiveStackFactoryImpl.stg.
 *
 * @since 6.0.
 */
public class MutableCharStackFactoryImpl implements MutableCharStackFactory
{
    public static final MutableCharStackFactory INSTANCE = new MutableCharStackFactoryImpl();

    @Override
    public MutableCharStack empty()
    {
        return new CharArrayStack();
    }

    @Override
    public MutableCharStack of()
    {
        return this.empty();
    }

    @Override
    public MutableCharStack with()
    {
        return this.empty();
    }

    @Override
    public MutableCharStack of(char... items)
    {
        return this.with(items);
    }

    @Override
    public MutableCharStack with(char... items)
    {
        if (items.length == 0)
        {
            return this.empty();
        }
        return CharArrayStack.newStackWith(items);
    }

    @Override
    public MutableCharStack ofAll(CharIterable items)
    {
        return this.withAll(items);
    }

    @Override
    public MutableCharStack withAll(CharIterable items)
    {
        if (items.isEmpty())
        {
            return this.empty();
        }
        return CharArrayStack.newStack(items);
    }

    /**
     * @since 10.0
     */
    @Override
    public MutableCharStack ofAll(Iterable<Character> iterable)
    {
        return this.withAll(iterable);
    }

    /**
     * @since 10.0
     */
    @Override
    public MutableCharStack withAll(Iterable<Character> iterable)
    {
        MutableCharStack stack = CharStacks.mutable.empty();
        Iterate.forEach(iterable, stack::push);
        return stack;
    }

    @Override
    public MutableCharStack ofAllReversed(CharIterable items)
    {
        return this.withAllReversed(items);
    }

    @Override
    public MutableCharStack withAllReversed(CharIterable items)
    {
        if (items.isEmpty())
        {
            return this.empty();
        }
        return CharArrayStack.newStackFromTopToBottom(items);
    }
}
