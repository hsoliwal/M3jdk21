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

import org.eclipse.collections.api.block.function.primitive.ByteFunction;
import org.eclipse.collections.api.block.function.primitive.ShortFunction;
import org.eclipse.collections.api.factory.map.primitive.MutableByteShortMapFactory;
import org.eclipse.collections.impl.factory.primitive.ByteShortMaps;
import org.eclipse.collections.api.map.primitive.MutableByteShortMap;
import org.eclipse.collections.api.map.primitive.ByteShortMap;
import org.eclipse.collections.impl.utility.Iterate;

/**
 * MutableByteShortMapFactoryImpl is a factory implementation which creates instances of type {@link MutableByteShortMap}.
 * This file was automatically generated from template file mutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 6.0.
 */
public class MutableByteShortMapFactoryImpl implements MutableByteShortMapFactory
{
    public static final MutableByteShortMapFactory INSTANCE = new MutableByteShortMapFactoryImpl();

    @Override
    public MutableByteShortMap empty()
    {
        return new ByteShortHashMap(0);
    }

    @Override
    public MutableByteShortMap of()
    {
        return this.empty();
    }

    @Override
    public MutableByteShortMap with()
    {
        return this.empty();
    }

    @Override
    public MutableByteShortMap with(byte key, short value)
    {
        return ByteShortHashMap.newWithKeysValues(key, value);
    }

    @Override
    public MutableByteShortMap of(byte key, short value)
    {
        return this.with(key, value);
    }

    @Override
    public MutableByteShortMap of(byte key1, short value1, byte key2, short value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    @Override
    public MutableByteShortMap with(byte key1, short value1, byte key2, short value2)
    {
        return ByteShortHashMap.newWithKeysValues(key1, value1, key2, value2);
    }

    @Override
    public MutableByteShortMap of(byte key1, short value1, byte key2, short value2, byte key3, short value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableByteShortMap with(byte key1, short value1, byte key2, short value2, byte key3, short value3)
    {
        return ByteShortHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableByteShortMap of(
            byte key1, short value1, byte key2, short value2, byte key3, short value3, byte key4, short value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableByteShortMap with(
            byte key1, short value1, byte key2, short value2, byte key3, short value3, byte key4, short value4)
    {
        return ByteShortHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableByteShortMap ofInitialCapacity(int capacity)
    {
        return this.withInitialCapacity(capacity);
    }

    @Override
    public MutableByteShortMap withInitialCapacity(int capacity)
    {
        return new ByteShortHashMap(capacity);
    }

    @Override
    public MutableByteShortMap ofAll(ByteShortMap map)
    {
        return this.withAll(map);
    }

    @Override
    public MutableByteShortMap withAll(ByteShortMap map)
    {
        if (map.isEmpty())
        {
            return this.empty();
        }
        return new ByteShortHashMap(map);
    }

    @Override
    public <T> MutableByteShortMap from(Iterable<T> iterable, ByteFunction<? super T> keyFunction, ShortFunction<? super T> valueFunction)
    {
        MutableByteShortMap map = ByteShortMaps.mutable.empty();
        Iterate.forEach(iterable, each -> map.put(keyFunction.byteValueOf(each), valueFunction.shortValueOf(each)));
        return map;
    }
}
