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

import jdk.m3.collections.eclipse.api.block.function.primitive.IntFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.CharFunction;
import jdk.m3.collections.eclipse.api.factory.map.primitive.MutableIntCharMapFactory;
import jdk.m3.collections.eclipse.impl.factory.primitive.IntCharMaps;
import jdk.m3.collections.eclipse.api.map.primitive.MutableIntCharMap;
import jdk.m3.collections.eclipse.api.map.primitive.IntCharMap;
import jdk.m3.collections.eclipse.impl.utility.Iterate;

/**
 * MutableIntCharMapFactoryImpl is a factory implementation which creates instances of type {@link MutableIntCharMap}.
 * This file was automatically generated from template file mutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 6.0.
 */
@SuppressWarnings("missing-explicit-ctor")
public class MutableIntCharMapFactoryImpl implements MutableIntCharMapFactory
{
    public static final MutableIntCharMapFactory INSTANCE = new MutableIntCharMapFactoryImpl();

    @Override
    public MutableIntCharMap empty()
    {
        return new IntCharHashMap(0);
    }

    @Override
    public MutableIntCharMap of()
    {
        return this.empty();
    }

    @Override
    public MutableIntCharMap with()
    {
        return this.empty();
    }

    @Override
    public MutableIntCharMap with(int key, char value)
    {
        return IntCharHashMap.newWithKeysValues(key, value);
    }

    @Override
    public MutableIntCharMap of(int key, char value)
    {
        return this.with(key, value);
    }

    @Override
    public MutableIntCharMap of(int key1, char value1, int key2, char value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    @Override
    public MutableIntCharMap with(int key1, char value1, int key2, char value2)
    {
        return IntCharHashMap.newWithKeysValues(key1, value1, key2, value2);
    }

    @Override
    public MutableIntCharMap of(int key1, char value1, int key2, char value2, int key3, char value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableIntCharMap with(int key1, char value1, int key2, char value2, int key3, char value3)
    {
        return IntCharHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableIntCharMap of(
            int key1, char value1, int key2, char value2, int key3, char value3, int key4, char value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableIntCharMap with(
            int key1, char value1, int key2, char value2, int key3, char value3, int key4, char value4)
    {
        return IntCharHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableIntCharMap ofInitialCapacity(int capacity)
    {
        return this.withInitialCapacity(capacity);
    }

    @Override
    public MutableIntCharMap withInitialCapacity(int capacity)
    {
        return new IntCharHashMap(capacity);
    }

    @Override
    public MutableIntCharMap ofAll(IntCharMap map)
    {
        return this.withAll(map);
    }

    @Override
    public MutableIntCharMap withAll(IntCharMap map)
    {
        if (map.isEmpty())
        {
            return this.empty();
        }
        return new IntCharHashMap(map);
    }

    @Override
    public <T> MutableIntCharMap from(Iterable<T> iterable, IntFunction<? super T> keyFunction, CharFunction<? super T> valueFunction)
    {
        MutableIntCharMap map = IntCharMaps.mutable.empty();
        Iterate.forEach(iterable, each -> map.put(keyFunction.intValueOf(each), valueFunction.charValueOf(each)));
        return map;
    }
}
