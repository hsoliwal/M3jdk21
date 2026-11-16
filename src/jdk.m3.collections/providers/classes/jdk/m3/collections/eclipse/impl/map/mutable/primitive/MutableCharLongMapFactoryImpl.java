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
import jdk.m3.collections.eclipse.api.block.function.primitive.LongFunction;
import jdk.m3.collections.eclipse.api.factory.map.primitive.MutableCharLongMapFactory;
import jdk.m3.collections.eclipse.impl.factory.primitive.CharLongMaps;
import jdk.m3.collections.eclipse.api.map.primitive.MutableCharLongMap;
import jdk.m3.collections.eclipse.api.map.primitive.CharLongMap;
import jdk.m3.collections.eclipse.impl.utility.Iterate;

/**
 * MutableCharLongMapFactoryImpl is a factory implementation which creates instances of type {@link MutableCharLongMap}.
 * This file was automatically generated from template file mutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 6.0.
 */
@SuppressWarnings("missing-explicit-ctor")
public class MutableCharLongMapFactoryImpl implements MutableCharLongMapFactory
{
    public static final MutableCharLongMapFactory INSTANCE = new MutableCharLongMapFactoryImpl();

    @Override
    public MutableCharLongMap empty()
    {
        return new CharLongHashMap(0);
    }

    @Override
    public MutableCharLongMap of()
    {
        return this.empty();
    }

    @Override
    public MutableCharLongMap with()
    {
        return this.empty();
    }

    @Override
    public MutableCharLongMap with(char key, long value)
    {
        return CharLongHashMap.newWithKeysValues(key, value);
    }

    @Override
    public MutableCharLongMap of(char key, long value)
    {
        return this.with(key, value);
    }

    @Override
    public MutableCharLongMap of(char key1, long value1, char key2, long value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    @Override
    public MutableCharLongMap with(char key1, long value1, char key2, long value2)
    {
        return CharLongHashMap.newWithKeysValues(key1, value1, key2, value2);
    }

    @Override
    public MutableCharLongMap of(char key1, long value1, char key2, long value2, char key3, long value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableCharLongMap with(char key1, long value1, char key2, long value2, char key3, long value3)
    {
        return CharLongHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableCharLongMap of(
            char key1, long value1, char key2, long value2, char key3, long value3, char key4, long value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableCharLongMap with(
            char key1, long value1, char key2, long value2, char key3, long value3, char key4, long value4)
    {
        return CharLongHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableCharLongMap ofInitialCapacity(int capacity)
    {
        return this.withInitialCapacity(capacity);
    }

    @Override
    public MutableCharLongMap withInitialCapacity(int capacity)
    {
        return new CharLongHashMap(capacity);
    }

    @Override
    public MutableCharLongMap ofAll(CharLongMap map)
    {
        return this.withAll(map);
    }

    @Override
    public MutableCharLongMap withAll(CharLongMap map)
    {
        if (map.isEmpty())
        {
            return this.empty();
        }
        return new CharLongHashMap(map);
    }

    @Override
    public <T> MutableCharLongMap from(Iterable<T> iterable, CharFunction<? super T> keyFunction, LongFunction<? super T> valueFunction)
    {
        MutableCharLongMap map = CharLongMaps.mutable.empty();
        Iterate.forEach(iterable, each -> map.put(keyFunction.charValueOf(each), valueFunction.longValueOf(each)));
        return map;
    }
}
