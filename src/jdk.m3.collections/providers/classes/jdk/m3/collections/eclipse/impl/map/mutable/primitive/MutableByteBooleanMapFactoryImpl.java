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
import jdk.m3.collections.eclipse.api.block.function.primitive.BooleanFunction;
import jdk.m3.collections.eclipse.api.factory.map.primitive.MutableByteBooleanMapFactory;
import jdk.m3.collections.eclipse.impl.factory.primitive.ByteBooleanMaps;
import jdk.m3.collections.eclipse.api.map.primitive.MutableByteBooleanMap;
import jdk.m3.collections.eclipse.api.map.primitive.ByteBooleanMap;
import jdk.m3.collections.eclipse.impl.utility.Iterate;

/**
 * MutableByteBooleanMapFactoryImpl is a factory implementation which creates instances of type {@link MutableByteBooleanMap}.
 * This file was automatically generated from template file mutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 6.0.
 */
@SuppressWarnings("missing-explicit-ctor")
public class MutableByteBooleanMapFactoryImpl implements MutableByteBooleanMapFactory
{
    public static final MutableByteBooleanMapFactory INSTANCE = new MutableByteBooleanMapFactoryImpl();

    @Override
    public MutableByteBooleanMap empty()
    {
        return new ByteBooleanHashMap(0);
    }

    @Override
    public MutableByteBooleanMap of()
    {
        return this.empty();
    }

    @Override
    public MutableByteBooleanMap with()
    {
        return this.empty();
    }

    @Override
    public MutableByteBooleanMap with(byte key, boolean value)
    {
        return ByteBooleanHashMap.newWithKeysValues(key, value);
    }

    @Override
    public MutableByteBooleanMap of(byte key, boolean value)
    {
        return this.with(key, value);
    }

    @Override
    public MutableByteBooleanMap of(byte key1, boolean value1, byte key2, boolean value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    @Override
    public MutableByteBooleanMap with(byte key1, boolean value1, byte key2, boolean value2)
    {
        return ByteBooleanHashMap.newWithKeysValues(key1, value1, key2, value2);
    }

    @Override
    public MutableByteBooleanMap of(byte key1, boolean value1, byte key2, boolean value2, byte key3, boolean value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableByteBooleanMap with(byte key1, boolean value1, byte key2, boolean value2, byte key3, boolean value3)
    {
        return ByteBooleanHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableByteBooleanMap of(
            byte key1, boolean value1, byte key2, boolean value2, byte key3, boolean value3, byte key4, boolean value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableByteBooleanMap with(
            byte key1, boolean value1, byte key2, boolean value2, byte key3, boolean value3, byte key4, boolean value4)
    {
        return ByteBooleanHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableByteBooleanMap ofInitialCapacity(int capacity)
    {
        return this.withInitialCapacity(capacity);
    }

    @Override
    public MutableByteBooleanMap withInitialCapacity(int capacity)
    {
        return new ByteBooleanHashMap(capacity);
    }

    @Override
    public MutableByteBooleanMap ofAll(ByteBooleanMap map)
    {
        return this.withAll(map);
    }

    @Override
    public MutableByteBooleanMap withAll(ByteBooleanMap map)
    {
        if (map.isEmpty())
        {
            return this.empty();
        }
        return new ByteBooleanHashMap(map);
    }

    @Override
    public <T> MutableByteBooleanMap from(Iterable<T> iterable, ByteFunction<? super T> keyFunction, BooleanFunction<? super T> valueFunction)
    {
        MutableByteBooleanMap map = ByteBooleanMaps.mutable.empty();
        Iterate.forEach(iterable, each -> map.put(keyFunction.byteValueOf(each), valueFunction.booleanValueOf(each)));
        return map;
    }
}
