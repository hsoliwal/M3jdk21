/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.list.immutable.primitive;

import org.eclipse.collections.api.DoubleIterable;
import org.eclipse.collections.api.factory.list.primitive.ImmutableDoubleListFactory;
import org.eclipse.collections.api.list.primitive.ImmutableDoubleList;
import org.eclipse.collections.impl.factory.primitive.DoubleLists;
import java.util.stream.DoubleStream;

/**
 * ImmutableDoubleListFactoryImpl is a factory implementation which creates instances of type {@link ImmutableDoubleList}.
 * This file was automatically generated from template file immutablePrimitiveListFactoryImpl.stg.
 *
 * @since 3.2.
 */
public class ImmutableDoubleListFactoryImpl implements ImmutableDoubleListFactory
{
    public static final ImmutableDoubleListFactory INSTANCE = new ImmutableDoubleListFactoryImpl();

    @Override
    public ImmutableDoubleList empty()
    {
        return ImmutableDoubleEmptyList.INSTANCE;
    }

    @Override
    public ImmutableDoubleList of()
    {
        return this.empty();
    }

    @Override
    public ImmutableDoubleList with()
    {
        return this.empty();
    }

    @Override
    public ImmutableDoubleList of(double one)
    {
        return this.with(one);
    }

    @Override
    public ImmutableDoubleList with(double one)
    {
        return new ImmutableDoubleSingletonList(one);
    }

    @Override
    public ImmutableDoubleList of(double... items)
    {
        return this.with(items);
    }

    @Override
    public ImmutableDoubleList with(double... items)
    {
        if (items == null || items.length == 0)
        {
            return this.with();
        }
        if (items.length == 1)
        {
            return this.with(items[0]);
        }
        return ImmutableDoubleArrayList.newListWith(items);
    }

    @Override
    public ImmutableDoubleList ofAll(DoubleIterable items)
    {
        return this.withAll(items);
    }

    @Override
    public ImmutableDoubleList withAll(DoubleIterable items)
    {
        if (items instanceof ImmutableDoubleList)
        {
            return (ImmutableDoubleList) items;
        }
        if (items == null || items.size() == 0)
        {
            return this.with();
        }
        if (items.size() == 1)
        {
            return this.with(items.toArray()[0]);
        }
        return ImmutableDoubleArrayList.newList(items);
    }

    /**
     * @since 10.0
     */
    @Override
    public ImmutableDoubleList ofAll(Iterable<Double> iterable)
    {
        return this.withAll(iterable);
    }

    /**
     * @since 10.0
     */
    @Override
    public ImmutableDoubleList withAll(Iterable<Double> iterable)
    {
        return DoubleLists.mutable.withAll(iterable).toImmutable();
    }

    /**
     * @since 9.0
     */
    @Override
    public ImmutableDoubleList ofAll(DoubleStream items)
    {
        return this.withAll(items);
    }

    /**
     * @since 9.0
     */
    @Override
    public ImmutableDoubleList withAll(DoubleStream items)
    {
        return this.with(items.toArray());
    }
}
