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

import org.eclipse.collections.api.FloatIterable;
import org.eclipse.collections.api.bag.primitive.ImmutableFloatBag;
import org.eclipse.collections.api.factory.bag.primitive.ImmutableFloatBagFactory;
import org.eclipse.collections.impl.factory.primitive.FloatBags;

/**
 * ImmutableFloatBagFactoryImpl is a factory implementation which creates instances of type {@link ImmutableFloatBag}.
 * This file was automatically generated from template file immutablePrimitiveBagFactoryImpl.stg.
 *
 * @since 4.0.
 */
public class ImmutableFloatBagFactoryImpl implements ImmutableFloatBagFactory
{
    public static final ImmutableFloatBagFactory INSTANCE = new ImmutableFloatBagFactoryImpl();

    @Override
    public ImmutableFloatBag empty()
    {
        return ImmutableFloatEmptyBag.INSTANCE;
    }

    @Override
    public ImmutableFloatBag of()
    {
        return this.empty();
    }

    @Override
    public ImmutableFloatBag with()
    {
        return this.empty();
    }

    @Override
    public ImmutableFloatBag of(float one)
    {
        return this.with(one);
    }

    @Override
    public ImmutableFloatBag with(float one)
    {
        return new ImmutableFloatSingletonBag(one);
    }

    @Override
    public ImmutableFloatBag of(float... items)
    {
        return this.with(items);
    }

    @Override
    public ImmutableFloatBag with(float... items)
    {
        if (items == null || items.length == 0)
        {
            return this.with();
        }
        if (items.length == 1)
        {
            return this.with(items[0]);
        }
        return ImmutableFloatHashBag.newBagWith(items);
    }

    @Override
    public ImmutableFloatBag ofAll(FloatIterable items)
    {
        return this.withAll(items);
    }

    @Override
    public ImmutableFloatBag withAll(FloatIterable items)
    {
        if (items instanceof ImmutableFloatBag)
        {
            return (ImmutableFloatBag) items;
        }
        return this.with(items.toArray());
    }

    /**
     * @since 10.0
     */
    @Override
    public ImmutableFloatBag ofAll(Iterable<Float> iterable)
    {
        return this.withAll(iterable);
    }

    /**
     * @since 10.0
     */
    @Override
    public ImmutableFloatBag withAll(Iterable<Float> iterable)
    {
        return FloatBags.mutable.withAll(iterable).toImmutable();
    }
}
