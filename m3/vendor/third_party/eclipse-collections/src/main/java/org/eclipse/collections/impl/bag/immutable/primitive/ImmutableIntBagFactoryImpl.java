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

import org.eclipse.collections.api.IntIterable;
import org.eclipse.collections.api.bag.primitive.ImmutableIntBag;
import org.eclipse.collections.api.factory.bag.primitive.ImmutableIntBagFactory;
import org.eclipse.collections.impl.factory.primitive.IntBags;
import java.util.stream.IntStream;

/**
 * ImmutableIntBagFactoryImpl is a factory implementation which creates instances of type {@link ImmutableIntBag}.
 * This file was automatically generated from template file immutablePrimitiveBagFactoryImpl.stg.
 *
 * @since 4.0.
 */
public class ImmutableIntBagFactoryImpl implements ImmutableIntBagFactory
{
    public static final ImmutableIntBagFactory INSTANCE = new ImmutableIntBagFactoryImpl();

    @Override
    public ImmutableIntBag empty()
    {
        return ImmutableIntEmptyBag.INSTANCE;
    }

    @Override
    public ImmutableIntBag of()
    {
        return this.empty();
    }

    @Override
    public ImmutableIntBag with()
    {
        return this.empty();
    }

    @Override
    public ImmutableIntBag of(int one)
    {
        return this.with(one);
    }

    @Override
    public ImmutableIntBag with(int one)
    {
        return new ImmutableIntSingletonBag(one);
    }

    @Override
    public ImmutableIntBag of(int... items)
    {
        return this.with(items);
    }

    @Override
    public ImmutableIntBag with(int... items)
    {
        if (items == null || items.length == 0)
        {
            return this.with();
        }
        if (items.length == 1)
        {
            return this.with(items[0]);
        }
        return ImmutableIntHashBag.newBagWith(items);
    }

    @Override
    public ImmutableIntBag ofAll(IntIterable items)
    {
        return this.withAll(items);
    }

    @Override
    public ImmutableIntBag withAll(IntIterable items)
    {
        if (items instanceof ImmutableIntBag)
        {
            return (ImmutableIntBag) items;
        }
        return this.with(items.toArray());
    }

    /**
     * @since 10.0
     */
    @Override
    public ImmutableIntBag ofAll(Iterable<Integer> iterable)
    {
        return this.withAll(iterable);
    }

    /**
     * @since 10.0
     */
    @Override
    public ImmutableIntBag withAll(Iterable<Integer> iterable)
    {
        return IntBags.mutable.withAll(iterable).toImmutable();
    }

    /**
     * @since 9.0
     */
    @Override
    public ImmutableIntBag ofAll(IntStream items)
    {
        return this.withAll(items);
    }

    /**
     * @since 9.0
     */
    @Override
    public ImmutableIntBag withAll(IntStream items)
    {
        return this.with(items.toArray());
    }
}
