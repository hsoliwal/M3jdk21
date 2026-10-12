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

import org.eclipse.collections.api.BooleanIterable;
import org.eclipse.collections.api.factory.bag.primitive.MutableBooleanBagFactory;
import org.eclipse.collections.api.bag.primitive.MutableBooleanBag;
import org.eclipse.collections.impl.factory.primitive.BooleanBags;
import org.eclipse.collections.impl.utility.Iterate;

/**
 * MutableBooleanBagFactoryImpl is a factory implementation which creates instances of type {@link MutableBooleanBag}.
 * This file was automatically generated from template file mutablePrimitiveBagFactoryImpl.stg.
 *
 * @since 6.0.
 */
public class MutableBooleanBagFactoryImpl implements MutableBooleanBagFactory
{
    public static final MutableBooleanBagFactory INSTANCE = new MutableBooleanBagFactoryImpl();

    @Override
    public MutableBooleanBag empty()
    {
        return new BooleanHashBag();
    }

    @Override
    public MutableBooleanBag of()
    {
        return this.empty();
    }

    @Override
    public MutableBooleanBag with()
    {
        return this.empty();
    }

    @Override
    public MutableBooleanBag of(boolean... items)
    {
        return this.with(items);
    }

    @Override
    public MutableBooleanBag with(boolean... items)
    {
        if (items == null || items.length == 0)
        {
            return this.with();
        }
        return BooleanHashBag.newBagWith(items);
    }

    @Override
    public MutableBooleanBag ofAll(BooleanIterable items)
    {
        return this.withAll(items);
    }

    @Override
    public MutableBooleanBag withAll(BooleanIterable items)
    {
        return BooleanHashBag.newBag(items);
    }

    /**
     * @since 10.0
     */
    @Override
    public MutableBooleanBag ofAll(Iterable<Boolean> iterable)
    {
        return this.withAll(iterable);
    }

    /**
     * @since 10.0
     */
    @Override
    public MutableBooleanBag withAll(Iterable<Boolean> iterable)
    {
        return Iterate.collectBoolean(iterable, Boolean::booleanValue, BooleanBags.mutable.empty());
    }
}
