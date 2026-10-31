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

import org.eclipse.collections.api.DoubleIterable;
import org.eclipse.collections.api.bag.primitive.ImmutableDoubleBag;
import org.eclipse.collections.api.factory.bag.primitive.ImmutableDoubleBagFactory;
import org.eclipse.collections.impl.factory.primitive.DoubleBags;
import java.util.stream.DoubleStream;

/**
 * ImmutableDoubleBagFactoryImpl is a factory implementation which creates instances of type {@link ImmutableDoubleBag}.
 * This file was automatically generated from template file immutablePrimitiveBagFactoryImpl.stg.
 *
 * @since 4.0.
 */
public class ImmutableDoubleBagFactoryImpl implements ImmutableDoubleBagFactory
{
    public static final ImmutableDoubleBagFactory INSTANCE = new ImmutableDoubleBagFactoryImpl();

    @Override
    public ImmutableDoubleBag empty()
    {
        return ImmutableDoubleEmptyBag.INSTANCE;
    }

    @Override
    public ImmutableDoubleBag of()
    {
        return this.empty();
    }

    @Override
    public ImmutableDoubleBag with()
    {
        return this.empty();
    }

    @Override
    public ImmutableDoubleBag of(double one)
    {
        return this.with(one);
    }

    @Override
    public ImmutableDoubleBag with(double one)
    {
        return new ImmutableDoubleSingletonBag(one);
    }

    @Override
    public ImmutableDoubleBag of(double... items)
    {
        return this.with(items);
    }

    @Override
    public ImmutableDoubleBag with(double... items)
    {
        if (items == null || items.length == 0)
        {
            return this.with();
        }
        if (items.length == 1)
        {
            return this.with(items[0]);
        }
        return ImmutableDoubleHashBag.newBagWith(items);
    }

    @Override
    public ImmutableDoubleBag ofAll(DoubleIterable items)
    {
        return this.withAll(items);
    }

    @Override
    public ImmutableDoubleBag withAll(DoubleIterable items)
    {
        if (items instanceof ImmutableDoubleBag)
        {
            return (ImmutableDoubleBag) items;
        }
        return this.with(items.toArray());
    }

    /**
     * @since 10.0
     */
    @Override
    public ImmutableDoubleBag ofAll(Iterable<Double> iterable)
    {
        return this.withAll(iterable);
    }

    /**
     * @since 10.0
     */
    @Override
    public ImmutableDoubleBag withAll(Iterable<Double> iterable)
    {
        return DoubleBags.mutable.withAll(iterable).toImmutable();
    }

    /**
     * @since 9.0
     */
    @Override
    public ImmutableDoubleBag ofAll(DoubleStream items)
    {
        return this.withAll(items);
    }

    /**
     * @since 9.0
     */
    @Override
    public ImmutableDoubleBag withAll(DoubleStream items)
    {
        return this.with(items.toArray());
    }
}
