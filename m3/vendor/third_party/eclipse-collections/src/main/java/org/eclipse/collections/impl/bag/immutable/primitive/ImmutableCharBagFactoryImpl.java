/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.bag.immutable.primitive;

import org.eclipse.collections.api.CharIterable;
import org.eclipse.collections.api.bag.primitive.ImmutableCharBag;
import org.eclipse.collections.api.factory.bag.primitive.ImmutableCharBagFactory;
import org.eclipse.collections.impl.factory.primitive.CharBags;

/**
 * ImmutableCharBagFactoryImpl is a factory implementation which creates instances of type {@link ImmutableCharBag}.
 * This file was automatically generated from template file immutablePrimitiveBagFactoryImpl.stg.
 *
 * @since 4.0.
 */
public class ImmutableCharBagFactoryImpl implements ImmutableCharBagFactory
{
    public static final ImmutableCharBagFactory INSTANCE = new ImmutableCharBagFactoryImpl();

    @Override
    public ImmutableCharBag empty()
    {
        return ImmutableCharEmptyBag.INSTANCE;
    }

    @Override
    public ImmutableCharBag of()
    {
        return this.empty();
    }

    @Override
    public ImmutableCharBag with()
    {
        return this.empty();
    }

    @Override
    public ImmutableCharBag of(char one)
    {
        return this.with(one);
    }

    @Override
    public ImmutableCharBag with(char one)
    {
        return new ImmutableCharSingletonBag(one);
    }

    @Override
    public ImmutableCharBag of(char... items)
    {
        return this.with(items);
    }

    @Override
    public ImmutableCharBag with(char... items)
    {
        if (items == null || items.length == 0)
        {
            return this.with();
        }
        if (items.length == 1)
        {
            return this.with(items[0]);
        }
        return ImmutableCharHashBag.newBagWith(items);
    }

    @Override
    public ImmutableCharBag ofAll(CharIterable items)
    {
        return this.withAll(items);
    }

    @Override
    public ImmutableCharBag withAll(CharIterable items)
    {
        if (items instanceof ImmutableCharBag)
        {
            return (ImmutableCharBag) items;
        }
        return this.with(items.toArray());
    }

    /**
     * @since 10.0
     */
    @Override
    public ImmutableCharBag ofAll(Iterable<Character> iterable)
    {
        return this.withAll(iterable);
    }

    /**
     * @since 10.0
     */
    @Override
    public ImmutableCharBag withAll(Iterable<Character> iterable)
    {
        return CharBags.mutable.withAll(iterable).toImmutable();
    }
}
