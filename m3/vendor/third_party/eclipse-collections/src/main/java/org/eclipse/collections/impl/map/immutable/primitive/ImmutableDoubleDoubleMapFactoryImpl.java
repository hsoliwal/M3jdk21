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
import org.eclipse.collections.api.factory.map.primitive.ImmutableDoubleDoubleMapFactory;
import org.eclipse.collections.api.map.primitive.ImmutableDoubleDoubleMap;
import org.eclipse.collections.api.map.primitive.DoubleDoubleMap;
import org.eclipse.collections.impl.factory.primitive.DoubleDoubleMaps;

/**
 * ImmutableDoubleDoubleMapFactoryImpl is a factory implementation which creates instances of type {@link ImmutableDoubleDoubleMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 4.0.
 */
public class ImmutableDoubleDoubleMapFactoryImpl implements ImmutableDoubleDoubleMapFactory
{
    public static final ImmutableDoubleDoubleMapFactory INSTANCE = new ImmutableDoubleDoubleMapFactoryImpl();

    @Override
    public ImmutableDoubleDoubleMap empty()
    {
        return ImmutableDoubleDoubleEmptyMap.INSTANCE;
    }

    @Override
    public ImmutableDoubleDoubleMap of()
    {
        return this.empty();
    }

    @Override
    public ImmutableDoubleDoubleMap with()
    {
        return this.empty();
    }

    @Override
    public ImmutableDoubleDoubleMap of(double key, double value)
    {
        return this.with(key, value);
    }

    @Override
    public ImmutableDoubleDoubleMap with(double key, double value)
    {
        return new ImmutableDoubleDoubleSingletonMap(key, value);
    }

    @Override
    public ImmutableDoubleDoubleMap ofAll(DoubleDoubleMap map)
    {
        return this.withAll(map);
    }

    @Override
    public ImmutableDoubleDoubleMap withAll(DoubleDoubleMap map)
    {
        if (map instanceof ImmutableDoubleDoubleMap)
        {
            return (ImmutableDoubleDoubleMap) map;
        }
        if (map.isEmpty())
        {
            return this.with();
        }
        if (map.size() == 1)
        {
            double key = map.keysView().doubleIterator().next();
            return new ImmutableDoubleDoubleSingletonMap(key, map.get(key));
        }
        return new ImmutableDoubleDoubleHashMap(map);
    }

    @Override
    public <T> ImmutableDoubleDoubleMap from(Iterable<T> iterable, DoubleFunction<? super T> keyFunction, DoubleFunction<? super T> valueFunction)
    {
        return DoubleDoubleMaps.mutable.from(iterable, keyFunction, valueFunction).toImmutable();
    }
}
