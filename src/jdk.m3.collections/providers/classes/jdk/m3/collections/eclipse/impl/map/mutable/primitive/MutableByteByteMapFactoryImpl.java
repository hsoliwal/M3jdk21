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

import jdk.m3.collections.eclipse.api.block.function.primitive.ByteFunction;
import jdk.m3.collections.eclipse.api.factory.map.primitive.MutableByteByteMapFactory;
import jdk.m3.collections.eclipse.impl.factory.primitive.ByteByteMaps;
import jdk.m3.collections.eclipse.api.map.primitive.MutableByteByteMap;
import jdk.m3.collections.eclipse.api.map.primitive.ByteByteMap;
import jdk.m3.collections.eclipse.impl.utility.Iterate;

/**
 * MutableByteByteMapFactoryImpl is a factory implementation which creates instances of type {@link MutableByteByteMap}.
 * This file was automatically generated from template file mutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 6.0.
 */
@SuppressWarnings("missing-explicit-ctor")
public class MutableByteByteMapFactoryImpl implements MutableByteByteMapFactory
{
    public static final MutableByteByteMapFactory INSTANCE = new MutableByteByteMapFactoryImpl();

    @Override
    public MutableByteByteMap empty()
    {
        return new ByteByteHashMap(0);
    }

    @Override
    public MutableByteByteMap of()
    {
        return this.empty();
    }

    @Override
    public MutableByteByteMap with()
    {
        return this.empty();
    }

    @Override
    public MutableByteByteMap with(byte key, byte value)
    {
        return ByteByteHashMap.newWithKeysValues(key, value);
    }

    @Override
    public MutableByteByteMap of(byte key, byte value)
    {
        return this.with(key, value);
    }

    @Override
    public MutableByteByteMap of(byte key1, byte value1, byte key2, byte value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    @Override
    public MutableByteByteMap with(byte key1, byte value1, byte key2, byte value2)
    {
        return ByteByteHashMap.newWithKeysValues(key1, value1, key2, value2);
    }

    @Override
    public MutableByteByteMap of(byte key1, byte value1, byte key2, byte value2, byte key3, byte value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableByteByteMap with(byte key1, byte value1, byte key2, byte value2, byte key3, byte value3)
    {
        return ByteByteHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableByteByteMap of(
            byte key1, byte value1, byte key2, byte value2, byte key3, byte value3, byte key4, byte value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableByteByteMap with(
            byte key1, byte value1, byte key2, byte value2, byte key3, byte value3, byte key4, byte value4)
    {
        return ByteByteHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableByteByteMap ofInitialCapacity(int capacity)
    {
        return this.withInitialCapacity(capacity);
    }

    @Override
    public MutableByteByteMap withInitialCapacity(int capacity)
    {
        return new ByteByteHashMap(capacity);
    }

    @Override
    public MutableByteByteMap ofAll(ByteByteMap map)
    {
        return this.withAll(map);
    }

    @Override
    public MutableByteByteMap withAll(ByteByteMap map)
    {
        if (map.isEmpty())
        {
            return this.empty();
        }
        return new ByteByteHashMap(map);
    }

    @Override
    public <T> MutableByteByteMap from(Iterable<T> iterable, ByteFunction<? super T> keyFunction, ByteFunction<? super T> valueFunction)
    {
        MutableByteByteMap map = ByteByteMaps.mutable.empty();
        Iterate.forEach(iterable, each -> map.put(keyFunction.byteValueOf(each), valueFunction.byteValueOf(each)));
        return map;
    }
}
