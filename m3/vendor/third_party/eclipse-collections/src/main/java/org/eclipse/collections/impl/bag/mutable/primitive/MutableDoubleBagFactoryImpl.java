/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.bag.mutable.primitive;

import org.eclipse.collections.api.DoubleIterable;
import org.eclipse.collections.api.factory.bag.primitive.MutableDoubleBagFactory;
import org.eclipse.collections.api.bag.primitive.MutableDoubleBag;
import org.eclipse.collections.impl.factory.primitive.DoubleBags;
import org.eclipse.collections.impl.utility.Iterate;
import java.util.stream.DoubleStream;

/**
 * MutableDoubleBagFactoryImpl is a factory implementation which creates instances of type {@link MutableDoubleBag}.
 * This file was automatically generated from template file mutablePrimitiveBagFactoryImpl.stg.
 *
 * @since 6.0.
 */
public class MutableDoubleBagFactoryImpl implements MutableDoubleBagFactory
{
    public static final MutableDoubleBagFactory INSTANCE = new MutableDoubleBagFactoryImpl();

    @Override
    public MutableDoubleBag empty()
    {
        return new DoubleHashBag();
    }

    @Override
    public MutableDoubleBag of()
    {
        return this.empty();
    }

    @Override
    public MutableDoubleBag with()
    {
        return this.empty();
    }

    @Override
    public MutableDoubleBag of(double... items)
    {
        return this.with(items);
    }

    @Override
    public MutableDoubleBag with(double... items)
    {
        if (items == null || items.length == 0)
        {
            return this.with();
        }
        return DoubleHashBag.newBagWith(items);
    }

    @Override
    public MutableDoubleBag ofAll(DoubleIterable items)
    {
        return this.withAll(items);
    }

    @Override
    public MutableDoubleBag withAll(DoubleIterable items)
    {
        return DoubleHashBag.newBag(items);
    }

    /**
     * @since 10.0
     */
    @Override
    public MutableDoubleBag ofAll(Iterable<Double> iterable)
    {
        return this.withAll(iterable);
    }

    /**
     * @since 10.0
     */
    @Override
    public MutableDoubleBag withAll(Iterable<Double> iterable)
    {
        return Iterate.collectDouble(iterable, Double::doubleValue, DoubleBags.mutable.empty());
    }

    /**
     * @since 9.0
     */
    @Override
    public MutableDoubleBag ofAll(DoubleStream items)
    {
        return this.withAll(items);
    }

    /**
     * @since 9.0
     */
    @Override
    public MutableDoubleBag withAll(DoubleStream items)
    {
        return this.with(items.toArray());
    }
}
