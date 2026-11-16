/*
 * Copyright (c) 2026 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.impl.bag.immutable.primitive;

import jdk.m3.collections.eclipse.api.ShortIterable;
import jdk.m3.collections.eclipse.api.bag.primitive.ImmutableShortBag;
import jdk.m3.collections.eclipse.api.bag.primitive.MutableShortBag;
import jdk.m3.collections.eclipse.api.factory.bag.primitive.ImmutableShortBagFactory;
import jdk.m3.collections.eclipse.impl.bag.mutable.primitive.ShortHashBag;
import jdk.m3.collections.eclipse.impl.factory.primitive.ShortBags;

/**
 * ImmutableShortBagFactoryImpl is a factory implementation which creates instances of type {@link ImmutableShortBag}.
 * This file was automatically generated from template file immutablePrimitiveBagFactoryImpl.stg.
 *
 * @since 4.0.
 */
@SuppressWarnings("missing-explicit-ctor")
public class ImmutableShortBagFactoryImpl implements ImmutableShortBagFactory
{
    public static final ImmutableShortBagFactory INSTANCE = new ImmutableShortBagFactoryImpl();

    @Override
    public ImmutableShortBag empty()
    {
        return ImmutableShortEmptyBag.INSTANCE;
    }

    @Override
    public ImmutableShortBag of()
    {
        return this.empty();
    }

    @Override
    public ImmutableShortBag with()
    {
        return this.empty();
    }

    @Override
    public ImmutableShortBag of(short one)
    {
        return this.with(one);
    }

    @Override
    public ImmutableShortBag with(short one)
    {
        return new ImmutableShortSingletonBag(one);
    }

    @Override
    public ImmutableShortBag of(short... items)
    {
        return this.with(items);
    }

    @Override
    public ImmutableShortBag with(short... items)
    {
        if (items == null || items.length == 0)
        {
            return this.with();
        }
        if (items.length == 1)
        {
            return this.with(items[0]);
        }
        return ImmutableShortHashBag.newBagWith(items);
    }

    @Override
    public ImmutableShortBag ofAll(ShortIterable items)
    {
        return this.withAll(items);
    }

    @Override
    public ImmutableShortBag withAll(ShortIterable items)
    {
        if (items instanceof ImmutableShortBag)
        {
            return (ImmutableShortBag) items;
        }
        if (items == null)
        {
            return this.with();
        }
        MutableShortBag bag = new ShortHashBag();
        items.forEach(bag::add);
        if (bag.size() == 0)
        {
            return this.with();
        }
        if (bag.size() == 1)
        {
            return this.with(bag.toArray());
        }
        return ImmutableShortHashBag.newBagWith(bag);
    }

    /**
     * @since 10.0
     */
    @Override
    public ImmutableShortBag ofAll(Iterable<Short> iterable)
    {
        return this.withAll(iterable);
    }

    /**
     * @since 10.0
     */
    @Override
    public ImmutableShortBag withAll(Iterable<Short> iterable)
    {
        return ShortBags.mutable.withAll(iterable).toImmutable();
    }
}
