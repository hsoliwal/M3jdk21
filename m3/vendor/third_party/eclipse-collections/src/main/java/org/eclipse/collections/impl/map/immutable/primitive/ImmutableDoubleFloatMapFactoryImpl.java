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
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.factory.map.primitive.ImmutableDoubleFloatMapFactory;
import org.eclipse.collections.api.map.primitive.ImmutableDoubleFloatMap;
import org.eclipse.collections.api.map.primitive.DoubleFloatMap;
import org.eclipse.collections.impl.factory.primitive.DoubleFloatMaps;

/**
 * ImmutableDoubleFloatMapFactoryImpl is a factory implementation which creates instances of type {@link ImmutableDoubleFloatMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 4.0.
 */
public class ImmutableDoubleFloatMapFactoryImpl implements ImmutableDoubleFloatMapFactory
{
    public static final ImmutableDoubleFloatMapFactory INSTANCE = new ImmutableDoubleFloatMapFactoryImpl();

    @Override
    public ImmutableDoubleFloatMap empty()
    {
        return ImmutableDoubleFloatEmptyMap.INSTANCE;
    }

    @Override
    public ImmutableDoubleFloatMap of()
    {
        return this.empty();
    }

    @Override
    public ImmutableDoubleFloatMap with()
    {
        return this.empty();
    }

    @Override
    public ImmutableDoubleFloatMap of(double key, float value)
    {
        return this.with(key, value);
    }

    @Override
    public ImmutableDoubleFloatMap with(double key, float value)
    {
        return new ImmutableDoubleFloatSingletonMap(key, value);
    }

    @Override
    public ImmutableDoubleFloatMap ofAll(DoubleFloatMap map)
    {
        return this.withAll(map);
    }

    @Override
    public ImmutableDoubleFloatMap withAll(DoubleFloatMap map)
    {
        if (map instanceof ImmutableDoubleFloatMap)
        {
            return (ImmutableDoubleFloatMap) map;
        }
        if (map.isEmpty())
        {
            return this.with();
        }
        if (map.size() == 1)
        {
            double key = map.keysView().doubleIterator().next();
            return new ImmutableDoubleFloatSingletonMap(key, map.get(key));
        }
        return new ImmutableDoubleFloatHashMap(map);
    }

    @Override
    public <T> ImmutableDoubleFloatMap from(Iterable<T> iterable, DoubleFunction<? super T> keyFunction, FloatFunction<? super T> valueFunction)
    {
        return DoubleFloatMaps.mutable.from(iterable, keyFunction, valueFunction).toImmutable();
    }
}
