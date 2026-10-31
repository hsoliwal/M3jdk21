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

import org.eclipse.collections.api.FloatIterable;
import org.eclipse.collections.api.factory.set.primitive.MutableFloatSetFactory;
import org.eclipse.collections.api.set.primitive.MutableFloatSet;
import org.eclipse.collections.impl.factory.primitive.FloatSets;
import org.eclipse.collections.impl.utility.Iterate;

/**
 * MutableFloatSetFactoryImpl is a factory implementation which creates instances of type {@link MutableFloatSet}.
 * This file was automatically generated from template file mutablePrimitiveSetFactoryImpl.stg.
 *
 * @since 6.0.
 */
public class MutableFloatSetFactoryImpl implements MutableFloatSetFactory
{
    public static final MutableFloatSetFactory INSTANCE = new MutableFloatSetFactoryImpl();

    @Override
    public MutableFloatSet empty()
    {
        return new FloatHashSet();
    }

    @Override
    public MutableFloatSet of()
    {
        return this.empty();
    }

    @Override
    public MutableFloatSet with()
    {
        return this.empty();
    }

    @Override
    public MutableFloatSet withInitialCapacity(int capacity)
    {
        return new FloatHashSet(capacity);
    }

    @Override
    public MutableFloatSet of(float... items)
    {
        return this.with(items);
    }

    @Override
    public MutableFloatSet with(float... items)
    {
        if (items == null || items.length == 0)
        {
            return this.empty();
        }
        return FloatHashSet.newSetWith(items);
    }

    @Override
    public MutableFloatSet ofAll(FloatIterable items)
    {
        return this.withAll(items);
    }

    @Override
    public MutableFloatSet withAll(FloatIterable items)
    {
        return FloatHashSet.newSet(items);
    }

    /**
     * @since 10.0
     */
    @Override
    public MutableFloatSet ofAll(Iterable<Float> iterable)
    {
        return this.withAll(iterable);
    }

    /**
     * @since 10.0
     */
    @Override
    public MutableFloatSet withAll(Iterable<Float> iterable)
    {
        return Iterate.collectFloat(iterable, Float::floatValue, FloatSets.mutable.empty());
    }
}
