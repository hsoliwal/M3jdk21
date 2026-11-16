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

import jdk.m3.collections.eclipse.api.block.function.primitive.LongFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ByteFunction;
import jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableLongByteMapFactory;
import jdk.m3.collections.eclipse.api.map.primitive.ImmutableLongByteMap;
import jdk.m3.collections.eclipse.api.map.primitive.LongByteMap;
import jdk.m3.collections.eclipse.impl.factory.primitive.LongByteMaps;

/**
 * ImmutableLongByteMapFactoryImpl is a factory implementation which creates instances of type {@link ImmutableLongByteMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 4.0.
 */
@SuppressWarnings("missing-explicit-ctor")
public class ImmutableLongByteMapFactoryImpl implements ImmutableLongByteMapFactory
{
    public static final ImmutableLongByteMapFactory INSTANCE = new ImmutableLongByteMapFactoryImpl();

    @Override
    public ImmutableLongByteMap empty()
    {
        return ImmutableLongByteEmptyMap.INSTANCE;
    }

    @Override
    public ImmutableLongByteMap of()
    {
        return this.empty();
    }

    @Override
    public ImmutableLongByteMap with()
    {
        return this.empty();
    }

    @Override
    public ImmutableLongByteMap of(long key, byte value)
    {
        return this.with(key, value);
    }

    @Override
    public ImmutableLongByteMap with(long key, byte value)
    {
        return new ImmutableLongByteSingletonMap(key, value);
    }

    @Override
    public ImmutableLongByteMap ofAll(LongByteMap map)
    {
        return this.withAll(map);
    }

    @Override
    public ImmutableLongByteMap withAll(LongByteMap map)
    {
        if (map instanceof ImmutableLongByteMap)
        {
            return (ImmutableLongByteMap) map;
        }
        if (map.isEmpty())
        {
            return this.with();
        }
        if (map.size() == 1)
        {
            long key = map.keysView().longIterator().next();
            return new ImmutableLongByteSingletonMap(key, map.get(key));
        }
        return new ImmutableLongByteHashMap(map);
    }

    @Override
    public <T> ImmutableLongByteMap from(Iterable<T> iterable, LongFunction<? super T> keyFunction, ByteFunction<? super T> valueFunction)
    {
        return LongByteMaps.mutable.from(iterable, keyFunction, valueFunction).toImmutable();
    }
}
