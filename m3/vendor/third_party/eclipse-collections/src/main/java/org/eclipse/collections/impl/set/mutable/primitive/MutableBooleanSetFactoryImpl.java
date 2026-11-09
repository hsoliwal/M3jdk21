/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.set.mutable.primitive;

import org.eclipse.collections.api.BooleanIterable;
import org.eclipse.collections.api.factory.set.primitive.MutableBooleanSetFactory;
import org.eclipse.collections.api.set.primitive.MutableBooleanSet;
import org.eclipse.collections.impl.factory.primitive.BooleanSets;
import org.eclipse.collections.impl.utility.Iterate;

/**
 * MutableBooleanSetFactoryImpl is a factory implementation which creates instances of type {@link MutableBooleanSet}.
 * This file was automatically generated from template file mutablePrimitiveSetFactoryImpl.stg.
 *
 * @since 6.0.
 */
public class MutableBooleanSetFactoryImpl implements MutableBooleanSetFactory
{
    public static final MutableBooleanSetFactory INSTANCE = new MutableBooleanSetFactoryImpl();

    @Override
    public MutableBooleanSet empty()
    {
        return new BooleanHashSet();
    }

    @Override
    public MutableBooleanSet of()
    {
        return this.empty();
    }

    @Override
    public MutableBooleanSet with()
    {
        return this.empty();
    }

    @Override
    public MutableBooleanSet of(boolean... items)
    {
        return this.with(items);
    }

    @Override
    public MutableBooleanSet with(boolean... items)
    {
        if (items == null || items.length == 0)
        {
            return this.empty();
        }
        return BooleanHashSet.newSetWith(items);
    }

    @Override
    public MutableBooleanSet ofAll(BooleanIterable items)
    {
        return this.withAll(items);
    }

    @Override
    public MutableBooleanSet withAll(BooleanIterable items)
    {
        return BooleanHashSet.newSet(items);
    }

    /**
     * @since 10.0
     */
    @Override
    public MutableBooleanSet ofAll(Iterable<Boolean> iterable)
    {
        return this.withAll(iterable);
    }

    /**
     * @since 10.0
     */
    @Override
    public MutableBooleanSet withAll(Iterable<Boolean> iterable)
    {
        return Iterate.collectBoolean(iterable, Boolean::booleanValue, BooleanSets.mutable.empty());
    }
}
