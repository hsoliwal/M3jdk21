/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.map.mutable.primitive;

import org.eclipse.collections.api.block.function.primitive.IntFunction;
import org.eclipse.collections.api.block.function.primitive.ByteFunction;
import org.eclipse.collections.api.factory.map.primitive.MutableIntByteMapFactory;
import org.eclipse.collections.impl.factory.primitive.IntByteMaps;
import org.eclipse.collections.api.map.primitive.MutableIntByteMap;
import org.eclipse.collections.api.map.primitive.IntByteMap;
import org.eclipse.collections.impl.utility.Iterate;

/**
 * MutableIntByteMapFactoryImpl is a factory implementation which creates instances of type {@link MutableIntByteMap}.
 * This file was automatically generated from template file mutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 6.0.
 */
public class MutableIntByteMapFactoryImpl implements MutableIntByteMapFactory
{
    public static final MutableIntByteMapFactory INSTANCE = new MutableIntByteMapFactoryImpl();

    @Override
    public MutableIntByteMap empty()
    {
        return new IntByteHashMap(0);
    }

    @Override
    public MutableIntByteMap of()
    {
        return this.empty();
    }

    @Override
    public MutableIntByteMap with()
    {
        return this.empty();
    }

    @Override
    public MutableIntByteMap with(int key, byte value)
    {
        return IntByteHashMap.newWithKeysValues(key, value);
    }

    @Override
    public MutableIntByteMap of(int key, byte value)
    {
        return this.with(key, value);
    }

    @Override
    public MutableIntByteMap of(int key1, byte value1, int key2, byte value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    @Override
    public MutableIntByteMap with(int key1, byte value1, int key2, byte value2)
    {
        return IntByteHashMap.newWithKeysValues(key1, value1, key2, value2);
    }

    @Override
    public MutableIntByteMap of(int key1, byte value1, int key2, byte value2, int key3, byte value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableIntByteMap with(int key1, byte value1, int key2, byte value2, int key3, byte value3)
    {
        return IntByteHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableIntByteMap of(
            int key1, byte value1, int key2, byte value2, int key3, byte value3, int key4, byte value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableIntByteMap with(
            int key1, byte value1, int key2, byte value2, int key3, byte value3, int key4, byte value4)
    {
        return IntByteHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableIntByteMap ofInitialCapacity(int capacity)
    {
        return this.withInitialCapacity(capacity);
    }

    @Override
    public MutableIntByteMap withInitialCapacity(int capacity)
    {
        return new IntByteHashMap(capacity);
    }

    @Override
    public MutableIntByteMap ofAll(IntByteMap map)
    {
        return this.withAll(map);
    }

    @Override
    public MutableIntByteMap withAll(IntByteMap map)
    {
        if (map.isEmpty())
        {
            return this.empty();
        }
        return new IntByteHashMap(map);
    }

    @Override
    public <T> MutableIntByteMap from(Iterable<T> iterable, IntFunction<? super T> keyFunction, ByteFunction<? super T> valueFunction)
    {
        MutableIntByteMap map = IntByteMaps.mutable.empty();
        Iterate.forEach(iterable, each -> map.put(keyFunction.intValueOf(each), valueFunction.byteValueOf(each)));
        return map;
    }
}
