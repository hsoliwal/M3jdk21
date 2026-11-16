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
import jdk.m3.collections.eclipse.api.block.function.primitive.DoubleFunction;
import jdk.m3.collections.eclipse.api.factory.map.primitive.MutableByteDoubleMapFactory;
import jdk.m3.collections.eclipse.impl.factory.primitive.ByteDoubleMaps;
import jdk.m3.collections.eclipse.api.map.primitive.MutableByteDoubleMap;
import jdk.m3.collections.eclipse.api.map.primitive.ByteDoubleMap;
import jdk.m3.collections.eclipse.impl.utility.Iterate;

/**
 * MutableByteDoubleMapFactoryImpl is a factory implementation which creates instances of type {@link MutableByteDoubleMap}.
 * This file was automatically generated from template file mutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 6.0.
 */
@SuppressWarnings("missing-explicit-ctor")
public class MutableByteDoubleMapFactoryImpl implements MutableByteDoubleMapFactory
{
    public static final MutableByteDoubleMapFactory INSTANCE = new MutableByteDoubleMapFactoryImpl();

    @Override
    public MutableByteDoubleMap empty()
    {
        return new ByteDoubleHashMap(0);
    }

    @Override
    public MutableByteDoubleMap of()
    {
        return this.empty();
    }

    @Override
    public MutableByteDoubleMap with()
    {
        return this.empty();
    }

    @Override
    public MutableByteDoubleMap with(byte key, double value)
    {
        return ByteDoubleHashMap.newWithKeysValues(key, value);
    }

    @Override
    public MutableByteDoubleMap of(byte key, double value)
    {
        return this.with(key, value);
    }

    @Override
    public MutableByteDoubleMap of(byte key1, double value1, byte key2, double value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    @Override
    public MutableByteDoubleMap with(byte key1, double value1, byte key2, double value2)
    {
        return ByteDoubleHashMap.newWithKeysValues(key1, value1, key2, value2);
    }

    @Override
    public MutableByteDoubleMap of(byte key1, double value1, byte key2, double value2, byte key3, double value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableByteDoubleMap with(byte key1, double value1, byte key2, double value2, byte key3, double value3)
    {
        return ByteDoubleHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableByteDoubleMap of(
            byte key1, double value1, byte key2, double value2, byte key3, double value3, byte key4, double value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableByteDoubleMap with(
            byte key1, double value1, byte key2, double value2, byte key3, double value3, byte key4, double value4)
    {
        return ByteDoubleHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableByteDoubleMap ofInitialCapacity(int capacity)
    {
        return this.withInitialCapacity(capacity);
    }

    @Override
    public MutableByteDoubleMap withInitialCapacity(int capacity)
    {
        return new ByteDoubleHashMap(capacity);
    }

    @Override
    public MutableByteDoubleMap ofAll(ByteDoubleMap map)
    {
        return this.withAll(map);
    }

    @Override
    public MutableByteDoubleMap withAll(ByteDoubleMap map)
    {
        if (map.isEmpty())
        {
            return this.empty();
        }
        return new ByteDoubleHashMap(map);
    }

    @Override
    public <T> MutableByteDoubleMap from(Iterable<T> iterable, ByteFunction<? super T> keyFunction, DoubleFunction<? super T> valueFunction)
    {
        MutableByteDoubleMap map = ByteDoubleMaps.mutable.empty();
        Iterate.forEach(iterable, each -> map.put(keyFunction.byteValueOf(each), valueFunction.doubleValueOf(each)));
        return map;
    }
}
