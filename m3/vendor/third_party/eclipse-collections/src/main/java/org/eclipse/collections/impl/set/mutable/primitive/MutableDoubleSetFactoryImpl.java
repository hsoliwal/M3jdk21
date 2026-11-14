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

import org.eclipse.collections.api.DoubleIterable;
import org.eclipse.collections.api.factory.set.primitive.MutableDoubleSetFactory;
import org.eclipse.collections.api.set.primitive.MutableDoubleSet;
import org.eclipse.collections.impl.factory.primitive.DoubleSets;
import org.eclipse.collections.impl.utility.Iterate;
import java.util.stream.DoubleStream;

/**
 * MutableDoubleSetFactoryImpl is a factory implementation which creates instances of type {@link MutableDoubleSet}.
 * This file was automatically generated from template file mutablePrimitiveSetFactoryImpl.stg.
 *
 * @since 6.0.
 */
public class MutableDoubleSetFactoryImpl implements MutableDoubleSetFactory
{
    public static final MutableDoubleSetFactory INSTANCE = new MutableDoubleSetFactoryImpl();

    @Override
    public MutableDoubleSet empty()
    {
        return new DoubleHashSet();
    }

    @Override
    public MutableDoubleSet of()
    {
        return this.empty();
    }

    @Override
    public MutableDoubleSet with()
    {
        return this.empty();
    }

    @Override
    public MutableDoubleSet withInitialCapacity(int capacity)
    {
        return new DoubleHashSet(capacity);
    }

    @Override
    public MutableDoubleSet of(double... items)
    {
        return this.with(items);
    }

    @Override
    public MutableDoubleSet with(double... items)
    {
        if (items == null || items.length == 0)
        {
            return this.empty();
        }
        return DoubleHashSet.newSetWith(items);
    }

    @Override
    public MutableDoubleSet ofAll(DoubleIterable items)
    {
        return this.withAll(items);
    }

    @Override
    public MutableDoubleSet withAll(DoubleIterable items)
    {
        return DoubleHashSet.newSet(items);
    }

    /**
     * @since 10.0
     */
    @Override
    public MutableDoubleSet ofAll(Iterable<Double> iterable)
    {
        return this.withAll(iterable);
    }

    /**
     * @since 10.0
     */
    @Override
    public MutableDoubleSet withAll(Iterable<Double> iterable)
    {
        return Iterate.collectDouble(iterable, Double::doubleValue, DoubleSets.mutable.empty());
    }

    /**
     * @since 9.0
     */
    @Override
    public MutableDoubleSet ofAll(DoubleStream items)
    {
        return this.withAll(items);
    }

    /**
     * @since 9.0
     */
    @Override
    public MutableDoubleSet withAll(DoubleStream items)
    {
        return this.with(items.toArray());
    }
}
