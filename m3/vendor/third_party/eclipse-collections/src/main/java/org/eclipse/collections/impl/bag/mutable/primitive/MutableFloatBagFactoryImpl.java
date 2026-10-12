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

import org.eclipse.collections.api.FloatIterable;
import org.eclipse.collections.api.factory.bag.primitive.MutableFloatBagFactory;
import org.eclipse.collections.api.bag.primitive.MutableFloatBag;
import org.eclipse.collections.impl.factory.primitive.FloatBags;
import org.eclipse.collections.impl.utility.Iterate;

/**
 * MutableFloatBagFactoryImpl is a factory implementation which creates instances of type {@link MutableFloatBag}.
 * This file was automatically generated from template file mutablePrimitiveBagFactoryImpl.stg.
 *
 * @since 6.0.
 */
public class MutableFloatBagFactoryImpl implements MutableFloatBagFactory
{
    public static final MutableFloatBagFactory INSTANCE = new MutableFloatBagFactoryImpl();

    @Override
    public MutableFloatBag empty()
    {
        return new FloatHashBag();
    }

    @Override
    public MutableFloatBag of()
    {
        return this.empty();
    }

    @Override
    public MutableFloatBag with()
    {
        return this.empty();
    }

    @Override
    public MutableFloatBag of(float... items)
    {
        return this.with(items);
    }

    @Override
    public MutableFloatBag with(float... items)
    {
        if (items == null || items.length == 0)
        {
            return this.with();
        }
        return FloatHashBag.newBagWith(items);
    }

    @Override
    public MutableFloatBag ofAll(FloatIterable items)
    {
        return this.withAll(items);
    }

    @Override
    public MutableFloatBag withAll(FloatIterable items)
    {
        return FloatHashBag.newBag(items);
    }

    /**
     * @since 10.0
     */
    @Override
    public MutableFloatBag ofAll(Iterable<Float> iterable)
    {
        return this.withAll(iterable);
    }

    /**
     * @since 10.0
     */
    @Override
    public MutableFloatBag withAll(Iterable<Float> iterable)
    {
        return Iterate.collectFloat(iterable, Float::floatValue, FloatBags.mutable.empty());
    }
}
