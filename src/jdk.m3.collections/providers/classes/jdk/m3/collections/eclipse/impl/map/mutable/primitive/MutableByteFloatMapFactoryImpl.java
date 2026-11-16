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
import jdk.m3.collections.eclipse.api.block.function.primitive.FloatFunction;
import jdk.m3.collections.eclipse.api.factory.map.primitive.MutableByteFloatMapFactory;
import jdk.m3.collections.eclipse.impl.factory.primitive.ByteFloatMaps;
import jdk.m3.collections.eclipse.api.map.primitive.MutableByteFloatMap;
import jdk.m3.collections.eclipse.api.map.primitive.ByteFloatMap;
import jdk.m3.collections.eclipse.impl.utility.Iterate;

/**
 * MutableByteFloatMapFactoryImpl is a factory implementation which creates instances of type {@link MutableByteFloatMap}.
 * This file was automatically generated from template file mutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 6.0.
 */
@SuppressWarnings("missing-explicit-ctor")
public class MutableByteFloatMapFactoryImpl implements MutableByteFloatMapFactory
{
    public static final MutableByteFloatMapFactory INSTANCE = new MutableByteFloatMapFactoryImpl();

    @Override
    public MutableByteFloatMap empty()
    {
        return new ByteFloatHashMap(0);
    }

    @Override
    public MutableByteFloatMap of()
    {
        return this.empty();
    }

    @Override
    public MutableByteFloatMap with()
    {
        return this.empty();
    }

    @Override
    public MutableByteFloatMap with(byte key, float value)
    {
        return ByteFloatHashMap.newWithKeysValues(key, value);
    }

    @Override
    public MutableByteFloatMap of(byte key, float value)
    {
        return this.with(key, value);
    }

    @Override
    public MutableByteFloatMap of(byte key1, float value1, byte key2, float value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    @Override
    public MutableByteFloatMap with(byte key1, float value1, byte key2, float value2)
    {
        return ByteFloatHashMap.newWithKeysValues(key1, value1, key2, value2);
    }

    @Override
    public MutableByteFloatMap of(byte key1, float value1, byte key2, float value2, byte key3, float value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableByteFloatMap with(byte key1, float value1, byte key2, float value2, byte key3, float value3)
    {
        return ByteFloatHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableByteFloatMap of(
            byte key1, float value1, byte key2, float value2, byte key3, float value3, byte key4, float value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableByteFloatMap with(
            byte key1, float value1, byte key2, float value2, byte key3, float value3, byte key4, float value4)
    {
        return ByteFloatHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableByteFloatMap ofInitialCapacity(int capacity)
    {
        return this.withInitialCapacity(capacity);
    }

    @Override
    public MutableByteFloatMap withInitialCapacity(int capacity)
    {
        return new ByteFloatHashMap(capacity);
    }

    @Override
    public MutableByteFloatMap ofAll(ByteFloatMap map)
    {
        return this.withAll(map);
    }

    @Override
    public MutableByteFloatMap withAll(ByteFloatMap map)
    {
        if (map.isEmpty())
        {
            return this.empty();
        }
        return new ByteFloatHashMap(map);
    }

    @Override
    public <T> MutableByteFloatMap from(Iterable<T> iterable, ByteFunction<? super T> keyFunction, FloatFunction<? super T> valueFunction)
    {
        MutableByteFloatMap map = ByteFloatMaps.mutable.empty();
        Iterate.forEach(iterable, each -> map.put(keyFunction.byteValueOf(each), valueFunction.floatValueOf(each)));
        return map;
    }
}
