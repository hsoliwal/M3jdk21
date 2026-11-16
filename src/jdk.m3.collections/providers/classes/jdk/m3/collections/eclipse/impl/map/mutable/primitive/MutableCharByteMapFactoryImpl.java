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

import jdk.m3.collections.eclipse.api.block.function.primitive.CharFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ByteFunction;
import jdk.m3.collections.eclipse.api.factory.map.primitive.MutableCharByteMapFactory;
import jdk.m3.collections.eclipse.impl.factory.primitive.CharByteMaps;
import jdk.m3.collections.eclipse.api.map.primitive.MutableCharByteMap;
import jdk.m3.collections.eclipse.api.map.primitive.CharByteMap;
import jdk.m3.collections.eclipse.impl.utility.Iterate;

/**
 * MutableCharByteMapFactoryImpl is a factory implementation which creates instances of type {@link MutableCharByteMap}.
 * This file was automatically generated from template file mutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 6.0.
 */
@SuppressWarnings("missing-explicit-ctor")
public class MutableCharByteMapFactoryImpl implements MutableCharByteMapFactory
{
    public static final MutableCharByteMapFactory INSTANCE = new MutableCharByteMapFactoryImpl();

    @Override
    public MutableCharByteMap empty()
    {
        return new CharByteHashMap(0);
    }

    @Override
    public MutableCharByteMap of()
    {
        return this.empty();
    }

    @Override
    public MutableCharByteMap with()
    {
        return this.empty();
    }

    @Override
    public MutableCharByteMap with(char key, byte value)
    {
        return CharByteHashMap.newWithKeysValues(key, value);
    }

    @Override
    public MutableCharByteMap of(char key, byte value)
    {
        return this.with(key, value);
    }

    @Override
    public MutableCharByteMap of(char key1, byte value1, char key2, byte value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    @Override
    public MutableCharByteMap with(char key1, byte value1, char key2, byte value2)
    {
        return CharByteHashMap.newWithKeysValues(key1, value1, key2, value2);
    }

    @Override
    public MutableCharByteMap of(char key1, byte value1, char key2, byte value2, char key3, byte value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableCharByteMap with(char key1, byte value1, char key2, byte value2, char key3, byte value3)
    {
        return CharByteHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableCharByteMap of(
            char key1, byte value1, char key2, byte value2, char key3, byte value3, char key4, byte value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableCharByteMap with(
            char key1, byte value1, char key2, byte value2, char key3, byte value3, char key4, byte value4)
    {
        return CharByteHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableCharByteMap ofInitialCapacity(int capacity)
    {
        return this.withInitialCapacity(capacity);
    }

    @Override
    public MutableCharByteMap withInitialCapacity(int capacity)
    {
        return new CharByteHashMap(capacity);
    }

    @Override
    public MutableCharByteMap ofAll(CharByteMap map)
    {
        return this.withAll(map);
    }

    @Override
    public MutableCharByteMap withAll(CharByteMap map)
    {
        if (map.isEmpty())
        {
            return this.empty();
        }
        return new CharByteHashMap(map);
    }

    @Override
    public <T> MutableCharByteMap from(Iterable<T> iterable, CharFunction<? super T> keyFunction, ByteFunction<? super T> valueFunction)
    {
        MutableCharByteMap map = CharByteMaps.mutable.empty();
        Iterate.forEach(iterable, each -> map.put(keyFunction.charValueOf(each), valueFunction.byteValueOf(each)));
        return map;
    }
}
