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

import org.eclipse.collections.api.LongIterable;
import org.eclipse.collections.api.bag.primitive.ImmutableLongBag;
import org.eclipse.collections.api.factory.bag.primitive.ImmutableLongBagFactory;
import org.eclipse.collections.impl.factory.primitive.LongBags;
import java.util.stream.LongStream;

/**
 * ImmutableLongBagFactoryImpl is a factory implementation which creates instances of type {@link ImmutableLongBag}.
 * This file was automatically generated from template file immutablePrimitiveBagFactoryImpl.stg.
 *
 * @since 4.0.
 */
public class ImmutableLongBagFactoryImpl implements ImmutableLongBagFactory
{
    public static final ImmutableLongBagFactory INSTANCE = new ImmutableLongBagFactoryImpl();

    @Override
    public ImmutableLongBag empty()
    {
        return ImmutableLongEmptyBag.INSTANCE;
    }

    @Override
    public ImmutableLongBag of()
    {
        return this.empty();
    }

    @Override
    public ImmutableLongBag with()
    {
        return this.empty();
    }

    @Override
    public ImmutableLongBag of(long one)
    {
        return this.with(one);
    }

    @Override
    public ImmutableLongBag with(long one)
    {
        return new ImmutableLongSingletonBag(one);
    }

    @Override
    public ImmutableLongBag of(long... items)
    {
        return this.with(items);
    }

    @Override
    public ImmutableLongBag with(long... items)
    {
        if (items == null || items.length == 0)
        {
            return this.with();
        }
        if (items.length == 1)
        {
            return this.with(items[0]);
        }
        return ImmutableLongHashBag.newBagWith(items);
    }

    @Override
    public ImmutableLongBag ofAll(LongIterable items)
    {
        return this.withAll(items);
    }

    @Override
    public ImmutableLongBag withAll(LongIterable items)
    {
        if (items instanceof ImmutableLongBag)
        {
            return (ImmutableLongBag) items;
        }
        return this.with(items.toArray());
    }

    /**
     * @since 10.0
     */
    @Override
    public ImmutableLongBag ofAll(Iterable<Long> iterable)
    {
        return this.withAll(iterable);
    }

    /**
     * @since 10.0
     */
    @Override
    public ImmutableLongBag withAll(Iterable<Long> iterable)
    {
        return LongBags.mutable.withAll(iterable).toImmutable();
    }

    /**
     * @since 9.0
     */
    @Override
    public ImmutableLongBag ofAll(LongStream items)
    {
        return this.withAll(items);
    }

    /**
     * @since 9.0
     */
    @Override
    public ImmutableLongBag withAll(LongStream items)
    {
        return this.with(items.toArray());
    }
}
