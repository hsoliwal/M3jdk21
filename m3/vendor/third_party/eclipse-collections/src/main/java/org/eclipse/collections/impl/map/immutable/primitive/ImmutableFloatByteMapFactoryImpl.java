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

import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.block.function.primitive.ByteFunction;
import org.eclipse.collections.api.factory.map.primitive.ImmutableFloatByteMapFactory;
import org.eclipse.collections.api.map.primitive.ImmutableFloatByteMap;
import org.eclipse.collections.api.map.primitive.FloatByteMap;
import org.eclipse.collections.impl.factory.primitive.FloatByteMaps;

/**
 * ImmutableFloatByteMapFactoryImpl is a factory implementation which creates instances of type {@link ImmutableFloatByteMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 4.0.
 */
public class ImmutableFloatByteMapFactoryImpl implements ImmutableFloatByteMapFactory
{
    public static final ImmutableFloatByteMapFactory INSTANCE = new ImmutableFloatByteMapFactoryImpl();

    @Override
    public ImmutableFloatByteMap empty()
    {
        return ImmutableFloatByteEmptyMap.INSTANCE;
    }

    @Override
    public ImmutableFloatByteMap of()
    {
        return this.empty();
    }

    @Override
    public ImmutableFloatByteMap with()
    {
        return this.empty();
    }

    @Override
    public ImmutableFloatByteMap of(float key, byte value)
    {
        return this.with(key, value);
    }

    @Override
    public ImmutableFloatByteMap with(float key, byte value)
    {
        return new ImmutableFloatByteSingletonMap(key, value);
    }

    @Override
    public ImmutableFloatByteMap ofAll(FloatByteMap map)
    {
        return this.withAll(map);
    }

    @Override
    public ImmutableFloatByteMap withAll(FloatByteMap map)
    {
        if (map instanceof ImmutableFloatByteMap)
        {
            return (ImmutableFloatByteMap) map;
        }
        if (map.isEmpty())
        {
            return this.with();
        }
        if (map.size() == 1)
        {
            float key = map.keysView().floatIterator().next();
            return new ImmutableFloatByteSingletonMap(key, map.get(key));
        }
        return new ImmutableFloatByteHashMap(map);
    }

    @Override
    public <T> ImmutableFloatByteMap from(Iterable<T> iterable, FloatFunction<? super T> keyFunction, ByteFunction<? super T> valueFunction)
    {
        return FloatByteMaps.mutable.from(iterable, keyFunction, valueFunction).toImmutable();
    }
}
