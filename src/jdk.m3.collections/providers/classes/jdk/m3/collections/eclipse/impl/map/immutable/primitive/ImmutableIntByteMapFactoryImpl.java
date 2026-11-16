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

import jdk.m3.collections.eclipse.api.block.function.primitive.IntFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ByteFunction;
import jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableIntByteMapFactory;
import jdk.m3.collections.eclipse.api.map.primitive.ImmutableIntByteMap;
import jdk.m3.collections.eclipse.api.map.primitive.IntByteMap;
import jdk.m3.collections.eclipse.impl.factory.primitive.IntByteMaps;

/**
 * ImmutableIntByteMapFactoryImpl is a factory implementation which creates instances of type {@link ImmutableIntByteMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 4.0.
 */
@SuppressWarnings("missing-explicit-ctor")
public class ImmutableIntByteMapFactoryImpl implements ImmutableIntByteMapFactory
{
    public static final ImmutableIntByteMapFactory INSTANCE = new ImmutableIntByteMapFactoryImpl();

    @Override
    public ImmutableIntByteMap empty()
    {
        return ImmutableIntByteEmptyMap.INSTANCE;
    }

    @Override
    public ImmutableIntByteMap of()
    {
        return this.empty();
    }

    @Override
    public ImmutableIntByteMap with()
    {
        return this.empty();
    }

    @Override
    public ImmutableIntByteMap of(int key, byte value)
    {
        return this.with(key, value);
    }

    @Override
    public ImmutableIntByteMap with(int key, byte value)
    {
        return new ImmutableIntByteSingletonMap(key, value);
    }

    @Override
    public ImmutableIntByteMap ofAll(IntByteMap map)
    {
        return this.withAll(map);
    }

    @Override
    public ImmutableIntByteMap withAll(IntByteMap map)
    {
        if (map instanceof ImmutableIntByteMap)
        {
            return (ImmutableIntByteMap) map;
        }
        if (map.isEmpty())
        {
            return this.with();
        }
        if (map.size() == 1)
        {
            int key = map.keysView().intIterator().next();
            return new ImmutableIntByteSingletonMap(key, map.get(key));
        }
        return new ImmutableIntByteHashMap(map);
    }

    @Override
    public <T> ImmutableIntByteMap from(Iterable<T> iterable, IntFunction<? super T> keyFunction, ByteFunction<? super T> valueFunction)
    {
        return IntByteMaps.mutable.from(iterable, keyFunction, valueFunction).toImmutable();
    }
}
