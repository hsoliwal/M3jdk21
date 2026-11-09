/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.list.immutable.primitive;

import org.eclipse.collections.api.CharIterable;
import org.eclipse.collections.api.factory.list.primitive.ImmutableCharListFactory;
import org.eclipse.collections.api.list.primitive.ImmutableCharList;
import org.eclipse.collections.impl.factory.primitive.CharLists;

/**
 * ImmutableCharListFactoryImpl is a factory implementation which creates instances of type {@link ImmutableCharList}.
 * This file was automatically generated from template file immutablePrimitiveListFactoryImpl.stg.
 *
 * @since 3.2.
 */
public class ImmutableCharListFactoryImpl implements ImmutableCharListFactory
{
    public static final ImmutableCharListFactory INSTANCE = new ImmutableCharListFactoryImpl();

    @Override
    public ImmutableCharList empty()
    {
        return ImmutableCharEmptyList.INSTANCE;
    }

    @Override
    public ImmutableCharList of()
    {
        return this.empty();
    }

    @Override
    public ImmutableCharList with()
    {
        return this.empty();
    }

    @Override
    public ImmutableCharList of(char one)
    {
        return this.with(one);
    }

    @Override
    public ImmutableCharList with(char one)
    {
        return new ImmutableCharSingletonList(one);
    }

    @Override
    public ImmutableCharList of(char... items)
    {
        return this.with(items);
    }

    @Override
    public ImmutableCharList with(char... items)
    {
        if (items == null || items.length == 0)
        {
            return this.with();
        }
        if (items.length == 1)
        {
            return this.with(items[0]);
        }
        return ImmutableCharArrayList.newListWith(items);
    }

    @Override
    public ImmutableCharList ofAll(CharIterable items)
    {
        return this.withAll(items);
    }

    @Override
    public ImmutableCharList withAll(CharIterable items)
    {
        if (items instanceof ImmutableCharList)
        {
            return (ImmutableCharList) items;
        }
        if (items == null || items.size() == 0)
        {
            return this.with();
        }
        if (items.size() == 1)
        {
            return this.with(items.toArray()[0]);
        }
        return ImmutableCharArrayList.newList(items);
    }

    /**
     * @since 10.0
     */
    @Override
    public ImmutableCharList ofAll(Iterable<Character> iterable)
    {
        return this.withAll(iterable);
    }

    /**
     * @since 10.0
     */
    @Override
    public ImmutableCharList withAll(Iterable<Character> iterable)
    {
        return CharLists.mutable.withAll(iterable).toImmutable();
    }
}
