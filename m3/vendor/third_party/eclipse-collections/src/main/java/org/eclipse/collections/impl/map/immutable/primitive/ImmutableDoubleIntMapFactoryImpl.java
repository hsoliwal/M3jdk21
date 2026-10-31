/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.map.immutable.primitive;

import org.eclipse.collections.api.block.function.primitive.DoubleFunction;
import org.eclipse.collections.api.block.function.primitive.IntFunction;
import org.eclipse.collections.api.factory.map.primitive.ImmutableDoubleIntMapFactory;
import org.eclipse.collections.api.map.primitive.ImmutableDoubleIntMap;
import org.eclipse.collections.api.map.primitive.DoubleIntMap;
import org.eclipse.collections.impl.factory.primitive.DoubleIntMaps;

/**
 * ImmutableDoubleIntMapFactoryImpl is a factory implementation which creates instances of type {@link ImmutableDoubleIntMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 4.0.
 */
public class ImmutableDoubleIntMapFactoryImpl implements ImmutableDoubleIntMapFactory
{
    public static final ImmutableDoubleIntMapFactory INSTANCE = new ImmutableDoubleIntMapFactoryImpl();

    @Override
    public ImmutableDoubleIntMap empty()
    {
        return ImmutableDoubleIntEmptyMap.INSTANCE;
    }

    @Override
    public ImmutableDoubleIntMap of()
    {
        return this.empty();
    }

    @Override
    public ImmutableDoubleIntMap with()
    {
        return this.empty();
    }

    @Override
    public ImmutableDoubleIntMap of(double key, int value)
    {
        return this.with(key, value);
    }

    @Override
    public ImmutableDoubleIntMap with(double key, int value)
    {
        return new ImmutableDoubleIntSingletonMap(key, value);
    }

    @Override
    public ImmutableDoubleIntMap ofAll(DoubleIntMap map)
    {
        return this.withAll(map);
    }

    @Override
    public ImmutableDoubleIntMap withAll(DoubleIntMap map)
    {
        if (map instanceof ImmutableDoubleIntMap)
        {
            return (ImmutableDoubleIntMap) map;
        }
        if (map.isEmpty())
        {
            return this.with();
        }
        if (map.size() == 1)
        {
            double key = map.keysView().doubleIterator().next();
            return new ImmutableDoubleIntSingletonMap(key, map.get(key));
        }
        return new ImmutableDoubleIntHashMap(map);
    }

    @Override
    public <T> ImmutableDoubleIntMap from(Iterable<T> iterable, DoubleFunction<? super T> keyFunction, IntFunction<? super T> valueFunction)
    {
        return DoubleIntMaps.mutable.from(iterable, keyFunction, valueFunction).toImmutable();
    }
}
