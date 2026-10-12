/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.list.mutable.primitive;

import org.eclipse.collections.api.DoubleIterable;
import org.eclipse.collections.api.factory.list.primitive.MutableDoubleListFactory;
import org.eclipse.collections.api.list.primitive.MutableDoubleList;
import org.eclipse.collections.impl.factory.primitive.DoubleLists;
import org.eclipse.collections.impl.utility.Iterate;
import java.util.stream.DoubleStream;

/**
 * MutableDoubleListFactoryImpl is a factory implementation which creates instances of type {@link MutableDoubleList}.
 * This file was automatically generated from template file mutablePrimitiveListFactoryImpl.stg.
 *
 * @since 6.0.
 */
public class MutableDoubleListFactoryImpl implements MutableDoubleListFactory
{
    public static final MutableDoubleListFactory INSTANCE = new MutableDoubleListFactoryImpl();

    @Override
    public MutableDoubleList empty()
    {
        return new DoubleArrayList();
    }

    @Override
    public MutableDoubleList of()
    {
        return this.empty();
    }

    @Override
    public MutableDoubleList with()
    {
        return this.empty();
    }

    @Override
    public MutableDoubleList withInitialCapacity(int capacity)
    {
        return new DoubleArrayList(capacity);
    }

    @Override
    public MutableDoubleList of(double... items)
    {
        return this.with(items);
    }

    /**
     * Creates a new list using the passed {@code items} argument as the backing store.
     * <p>
     * !!! WARNING: This method uses the passed in array, so can be very unsafe if the original
     * array is held onto anywhere else. !!!
     */
    @Override
    public MutableDoubleList with(double... items)
    {
        if (items == null || items.length == 0)
        {
            return this.with();
        }
        return DoubleArrayList.newListWith(items);
    }

    @Override
    public MutableDoubleList ofAll(DoubleIterable items)
    {
        return this.withAll(items);
    }

    @Override
    public MutableDoubleList withAll(DoubleIterable items)
    {
        return DoubleArrayList.newList(items);
    }

    /**
     * @since 10.0
     */
    @Override
    public MutableDoubleList ofAll(Iterable<Double> iterable)
    {
        return this.withAll(iterable);
    }

    /**
     * @since 10.0
     */
    @Override
    public MutableDoubleList withAll(Iterable<Double> iterable)
    {
        return Iterate.collectDouble(iterable, Double::doubleValue, DoubleLists.mutable.empty());
    }

    /**
     * @since 9.0
     */
    @Override
    public MutableDoubleList ofAll(DoubleStream items)
    {
        return this.withAll(items);
    }

    /**
     * @since 9.0
     */
    @Override
    public MutableDoubleList withAll(DoubleStream items)
    {
        return this.with(items.toArray());
    }
}
