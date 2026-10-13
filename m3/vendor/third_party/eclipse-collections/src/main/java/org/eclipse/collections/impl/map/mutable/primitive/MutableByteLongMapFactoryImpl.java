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
import org.eclipse.collections.api.block.function.primitive.LongFunction;
import org.eclipse.collections.api.factory.map.primitive.MutableByteLongMapFactory;
import org.eclipse.collections.impl.factory.primitive.ByteLongMaps;
import org.eclipse.collections.api.map.primitive.MutableByteLongMap;
import org.eclipse.collections.api.map.primitive.ByteLongMap;
import org.eclipse.collections.impl.utility.Iterate;

/**
 * MutableByteLongMapFactoryImpl is a factory implementation which creates instances of type {@link MutableByteLongMap}.
 * This file was automatically generated from template file mutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 6.0.
 */
public class MutableByteLongMapFactoryImpl implements MutableByteLongMapFactory
{
    public static final MutableByteLongMapFactory INSTANCE = new MutableByteLongMapFactoryImpl();

    @Override
    public MutableByteLongMap empty()
    {
        return new ByteLongHashMap(0);
    }

    @Override
    public MutableByteLongMap of()
    {
        return this.empty();
    }

    @Override
    public MutableByteLongMap with()
    {
        return this.empty();
    }

    @Override
    public MutableByteLongMap with(byte key, long value)
    {
        return ByteLongHashMap.newWithKeysValues(key, value);
    }

    @Override
    public MutableByteLongMap of(byte key, long value)
    {
        return this.with(key, value);
    }

    @Override
    public MutableByteLongMap of(byte key1, long value1, byte key2, long value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    @Override
    public MutableByteLongMap with(byte key1, long value1, byte key2, long value2)
    {
        return ByteLongHashMap.newWithKeysValues(key1, value1, key2, value2);
    }

    @Override
    public MutableByteLongMap of(byte key1, long value1, byte key2, long value2, byte key3, long value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableByteLongMap with(byte key1, long value1, byte key2, long value2, byte key3, long value3)
    {
        return ByteLongHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableByteLongMap of(
            byte key1, long value1, byte key2, long value2, byte key3, long value3, byte key4, long value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableByteLongMap with(
            byte key1, long value1, byte key2, long value2, byte key3, long value3, byte key4, long value4)
    {
        return ByteLongHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableByteLongMap ofInitialCapacity(int capacity)
    {
        return this.withInitialCapacity(capacity);
    }

    @Override
    public MutableByteLongMap withInitialCapacity(int capacity)
    {
        return new ByteLongHashMap(capacity);
    }

    @Override
    public MutableByteLongMap ofAll(ByteLongMap map)
    {
        return this.withAll(map);
    }

    @Override
    public MutableByteLongMap withAll(ByteLongMap map)
    {
        if (map.isEmpty())
        {
            return this.empty();
        }
        return new ByteLongHashMap(map);
    }

    @Override
    public <T> MutableByteLongMap from(Iterable<T> iterable, ByteFunction<? super T> keyFunction, LongFunction<? super T> valueFunction)
    {
        MutableByteLongMap map = ByteLongMaps.mutable.empty();
        Iterate.forEach(iterable, each -> map.put(keyFunction.byteValueOf(each), valueFunction.longValueOf(each)));
        return map;
    }
}
