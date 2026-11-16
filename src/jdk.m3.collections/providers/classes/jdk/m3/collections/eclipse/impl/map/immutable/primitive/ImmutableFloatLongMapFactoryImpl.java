/*
 * Copyright (c) 2026 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.impl.map.immutable.primitive;

import jdk.m3.collections.eclipse.api.block.function.primitive.FloatFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.LongFunction;
import jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableFloatLongMapFactory;
import jdk.m3.collections.eclipse.api.map.primitive.ImmutableFloatLongMap;
import jdk.m3.collections.eclipse.api.map.primitive.FloatLongMap;
import jdk.m3.collections.eclipse.impl.factory.primitive.FloatLongMaps;

/**
 * ImmutableFloatLongMapFactoryImpl is a factory implementation which creates instances of type {@link ImmutableFloatLongMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 4.0.
 */
@SuppressWarnings("missing-explicit-ctor")
public class ImmutableFloatLongMapFactoryImpl implements ImmutableFloatLongMapFactory
{
    public static final ImmutableFloatLongMapFactory INSTANCE = new ImmutableFloatLongMapFactoryImpl();

    @Override
    public ImmutableFloatLongMap empty()
    {
        return ImmutableFloatLongEmptyMap.INSTANCE;
    }

    @Override
    public ImmutableFloatLongMap of()
    {
        return this.empty();
    }

    @Override
    public ImmutableFloatLongMap with()
    {
        return this.empty();
    }

    @Override
    public ImmutableFloatLongMap of(float key, long value)
    {
        return this.with(key, value);
    }

    @Override
    public ImmutableFloatLongMap with(float key, long value)
    {
        return new ImmutableFloatLongSingletonMap(key, value);
    }

    @Override
    public ImmutableFloatLongMap ofAll(FloatLongMap map)
    {
        return this.withAll(map);
    }

    @Override
    public ImmutableFloatLongMap withAll(FloatLongMap map)
    {
        if (map instanceof ImmutableFloatLongMap)
        {
            return (ImmutableFloatLongMap) map;
        }
        if (map.isEmpty())
        {
            return this.with();
        }
        if (map.size() == 1)
        {
            float key = map.keysView().floatIterator().next();
            return new ImmutableFloatLongSingletonMap(key, map.get(key));
        }
        return new ImmutableFloatLongHashMap(map);
    }

    @Override
    public <T> ImmutableFloatLongMap from(Iterable<T> iterable, FloatFunction<? super T> keyFunction, LongFunction<? super T> valueFunction)
    {
        return FloatLongMaps.mutable.from(iterable, keyFunction, valueFunction).toImmutable();
    }
}
