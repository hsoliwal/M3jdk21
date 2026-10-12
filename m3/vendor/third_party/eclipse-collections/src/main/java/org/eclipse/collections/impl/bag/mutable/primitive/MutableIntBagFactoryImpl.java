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

import org.eclipse.collections.api.IntIterable;
import org.eclipse.collections.api.factory.bag.primitive.MutableIntBagFactory;
import org.eclipse.collections.api.bag.primitive.MutableIntBag;
import org.eclipse.collections.impl.factory.primitive.IntBags;
import org.eclipse.collections.impl.utility.Iterate;
import java.util.stream.IntStream;

/**
 * MutableIntBagFactoryImpl is a factory implementation which creates instances of type {@link MutableIntBag}.
 * This file was automatically generated from template file mutablePrimitiveBagFactoryImpl.stg.
 *
 * @since 6.0.
 */
public class MutableIntBagFactoryImpl implements MutableIntBagFactory
{
    public static final MutableIntBagFactory INSTANCE = new MutableIntBagFactoryImpl();

    @Override
    public MutableIntBag empty()
    {
        return new IntHashBag();
    }

    @Override
    public MutableIntBag of()
    {
        return this.empty();
    }

    @Override
    public MutableIntBag with()
    {
        return this.empty();
    }

    @Override
    public MutableIntBag of(int... items)
    {
        return this.with(items);
    }

    @Override
    public MutableIntBag with(int... items)
    {
        if (items == null || items.length == 0)
        {
            return this.with();
        }
        return IntHashBag.newBagWith(items);
    }

    @Override
    public MutableIntBag ofAll(IntIterable items)
    {
        return this.withAll(items);
    }

    @Override
    public MutableIntBag withAll(IntIterable items)
    {
        return IntHashBag.newBag(items);
    }

    /**
     * @since 10.0
     */
    @Override
    public MutableIntBag ofAll(Iterable<Integer> iterable)
    {
        return this.withAll(iterable);
    }

    /**
     * @since 10.0
     */
    @Override
    public MutableIntBag withAll(Iterable<Integer> iterable)
    {
        return Iterate.collectInt(iterable, Integer::intValue, IntBags.mutable.empty());
    }

    /**
     * @since 9.0
     */
    @Override
    public MutableIntBag ofAll(IntStream items)
    {
        return this.withAll(items);
    }

    /**
     * @since 9.0
     */
    @Override
    public MutableIntBag withAll(IntStream items)
    {
        return this.with(items.toArray());
    }
}
