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
import jdk.m3.collections.eclipse.api.block.function.primitive.CharFunction;
import jdk.m3.collections.eclipse.api.factory.map.primitive.MutableByteCharMapFactory;
import jdk.m3.collections.eclipse.impl.factory.primitive.ByteCharMaps;
import jdk.m3.collections.eclipse.api.map.primitive.MutableByteCharMap;
import jdk.m3.collections.eclipse.api.map.primitive.ByteCharMap;
import jdk.m3.collections.eclipse.impl.utility.Iterate;

/**
 * MutableByteCharMapFactoryImpl is a factory implementation which creates instances of type {@link MutableByteCharMap}.
 * This file was automatically generated from template file mutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 6.0.
 */
@SuppressWarnings("missing-explicit-ctor")
public class MutableByteCharMapFactoryImpl implements MutableByteCharMapFactory
{
    public static final MutableByteCharMapFactory INSTANCE = new MutableByteCharMapFactoryImpl();

    @Override
    public MutableByteCharMap empty()
    {
        return new ByteCharHashMap(0);
    }

    @Override
    public MutableByteCharMap of()
    {
        return this.empty();
    }

    @Override
    public MutableByteCharMap with()
    {
        return this.empty();
    }

    @Override
    public MutableByteCharMap with(byte key, char value)
    {
        return ByteCharHashMap.newWithKeysValues(key, value);
    }

    @Override
    public MutableByteCharMap of(byte key, char value)
    {
        return this.with(key, value);
    }

    @Override
    public MutableByteCharMap of(byte key1, char value1, byte key2, char value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    @Override
    public MutableByteCharMap with(byte key1, char value1, byte key2, char value2)
    {
        return ByteCharHashMap.newWithKeysValues(key1, value1, key2, value2);
    }

    @Override
    public MutableByteCharMap of(byte key1, char value1, byte key2, char value2, byte key3, char value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableByteCharMap with(byte key1, char value1, byte key2, char value2, byte key3, char value3)
    {
        return ByteCharHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableByteCharMap of(
            byte key1, char value1, byte key2, char value2, byte key3, char value3, byte key4, char value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableByteCharMap with(
            byte key1, char value1, byte key2, char value2, byte key3, char value3, byte key4, char value4)
    {
        return ByteCharHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableByteCharMap ofInitialCapacity(int capacity)
    {
        return this.withInitialCapacity(capacity);
    }

    @Override
    public MutableByteCharMap withInitialCapacity(int capacity)
    {
        return new ByteCharHashMap(capacity);
    }

    @Override
    public MutableByteCharMap ofAll(ByteCharMap map)
    {
        return this.withAll(map);
    }

    @Override
    public MutableByteCharMap withAll(ByteCharMap map)
    {
        if (map.isEmpty())
        {
            return this.empty();
        }
        return new ByteCharHashMap(map);
    }

    @Override
    public <T> MutableByteCharMap from(Iterable<T> iterable, ByteFunction<? super T> keyFunction, CharFunction<? super T> valueFunction)
    {
        MutableByteCharMap map = ByteCharMaps.mutable.empty();
        Iterate.forEach(iterable, each -> map.put(keyFunction.byteValueOf(each), valueFunction.charValueOf(each)));
        return map;
    }
}
