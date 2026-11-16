/*
 * Copyright (c) 2026 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.impl.map.mutable.primitive;

import jdk.m3.collections.eclipse.api.block.function.primitive.LongFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ByteFunction;
import jdk.m3.collections.eclipse.api.factory.map.primitive.MutableLongByteMapFactory;
import jdk.m3.collections.eclipse.impl.factory.primitive.LongByteMaps;
import jdk.m3.collections.eclipse.api.map.primitive.MutableLongByteMap;
import jdk.m3.collections.eclipse.api.map.primitive.LongByteMap;
import jdk.m3.collections.eclipse.impl.utility.Iterate;

/**
 * MutableLongByteMapFactoryImpl is a factory implementation which creates instances of type {@link MutableLongByteMap}.
 * This file was automatically generated from template file mutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 6.0.
 */
@SuppressWarnings("missing-explicit-ctor")
public class MutableLongByteMapFactoryImpl implements MutableLongByteMapFactory
{
    public static final MutableLongByteMapFactory INSTANCE = new MutableLongByteMapFactoryImpl();

    @Override
    public MutableLongByteMap empty()
    {
        return new LongByteHashMap(0);
    }

    @Override
    public MutableLongByteMap of()
    {
        return this.empty();
    }

    @Override
    public MutableLongByteMap with()
    {
        return this.empty();
    }

    @Override
    public MutableLongByteMap with(long key, byte value)
    {
        return LongByteHashMap.newWithKeysValues(key, value);
    }

    @Override
    public MutableLongByteMap of(long key, byte value)
    {
        return this.with(key, value);
    }

    @Override
    public MutableLongByteMap of(long key1, byte value1, long key2, byte value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    @Override
    public MutableLongByteMap with(long key1, byte value1, long key2, byte value2)
    {
        return LongByteHashMap.newWithKeysValues(key1, value1, key2, value2);
    }

    @Override
    public MutableLongByteMap of(long key1, byte value1, long key2, byte value2, long key3, byte value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableLongByteMap with(long key1, byte value1, long key2, byte value2, long key3, byte value3)
    {
        return LongByteHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableLongByteMap of(
            long key1, byte value1, long key2, byte value2, long key3, byte value3, long key4, byte value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableLongByteMap with(
            long key1, byte value1, long key2, byte value2, long key3, byte value3, long key4, byte value4)
    {
        return LongByteHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableLongByteMap ofInitialCapacity(int capacity)
    {
        return this.withInitialCapacity(capacity);
    }

    @Override
    public MutableLongByteMap withInitialCapacity(int capacity)
    {
        return new LongByteHashMap(capacity);
    }

    @Override
    public MutableLongByteMap ofAll(LongByteMap map)
    {
        return this.withAll(map);
    }

    @Override
    public MutableLongByteMap withAll(LongByteMap map)
    {
        if (map.isEmpty())
        {
            return this.empty();
        }
        return new LongByteHashMap(map);
    }

    @Override
    public <T> MutableLongByteMap from(Iterable<T> iterable, LongFunction<? super T> keyFunction, ByteFunction<? super T> valueFunction)
    {
        MutableLongByteMap map = LongByteMaps.mutable.empty();
        Iterate.forEach(iterable, each -> map.put(keyFunction.longValueOf(each), valueFunction.byteValueOf(each)));
        return map;
    }
}
