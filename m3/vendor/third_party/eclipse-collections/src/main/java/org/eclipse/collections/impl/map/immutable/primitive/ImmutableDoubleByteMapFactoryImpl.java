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
import org.eclipse.collections.api.block.function.primitive.ByteFunction;
import org.eclipse.collections.api.factory.map.primitive.ImmutableDoubleByteMapFactory;
import org.eclipse.collections.api.map.primitive.ImmutableDoubleByteMap;
import org.eclipse.collections.api.map.primitive.DoubleByteMap;
import org.eclipse.collections.impl.factory.primitive.DoubleByteMaps;

/**
 * ImmutableDoubleByteMapFactoryImpl is a factory implementation which creates instances of type {@link ImmutableDoubleByteMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 4.0.
 */
public class ImmutableDoubleByteMapFactoryImpl implements ImmutableDoubleByteMapFactory
{
    public static final ImmutableDoubleByteMapFactory INSTANCE = new ImmutableDoubleByteMapFactoryImpl();

    @Override
    public ImmutableDoubleByteMap empty()
    {
        return ImmutableDoubleByteEmptyMap.INSTANCE;
    }

    @Override
    public ImmutableDoubleByteMap of()
    {
        return this.empty();
    }

    @Override
    public ImmutableDoubleByteMap with()
    {
        return this.empty();
    }

    @Override
    public ImmutableDoubleByteMap of(double key, byte value)
    {
        return this.with(key, value);
    }

    @Override
    public ImmutableDoubleByteMap with(double key, byte value)
    {
        return new ImmutableDoubleByteSingletonMap(key, value);
    }

    @Override
    public ImmutableDoubleByteMap ofAll(DoubleByteMap map)
    {
        return this.withAll(map);
    }

    @Override
    public ImmutableDoubleByteMap withAll(DoubleByteMap map)
    {
        if (map instanceof ImmutableDoubleByteMap)
        {
            return (ImmutableDoubleByteMap) map;
        }
        if (map.isEmpty())
        {
            return this.with();
        }
        if (map.size() == 1)
        {
            double key = map.keysView().doubleIterator().next();
            return new ImmutableDoubleByteSingletonMap(key, map.get(key));
        }
        return new ImmutableDoubleByteHashMap(map);
    }

    @Override
    public <T> ImmutableDoubleByteMap from(Iterable<T> iterable, DoubleFunction<? super T> keyFunction, ByteFunction<? super T> valueFunction)
    {
        return DoubleByteMaps.mutable.from(iterable, keyFunction, valueFunction).toImmutable();
    }
}
