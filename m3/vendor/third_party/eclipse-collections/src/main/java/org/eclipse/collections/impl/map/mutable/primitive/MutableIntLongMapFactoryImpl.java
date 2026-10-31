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
import org.eclipse.collections.api.block.function.primitive.LongFunction;
import org.eclipse.collections.api.factory.map.primitive.MutableIntLongMapFactory;
import org.eclipse.collections.impl.factory.primitive.IntLongMaps;
import org.eclipse.collections.api.map.primitive.MutableIntLongMap;
import org.eclipse.collections.api.map.primitive.IntLongMap;
import org.eclipse.collections.impl.utility.Iterate;

/**
 * MutableIntLongMapFactoryImpl is a factory implementation which creates instances of type {@link MutableIntLongMap}.
 * This file was automatically generated from template file mutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 6.0.
 */
public class MutableIntLongMapFactoryImpl implements MutableIntLongMapFactory
{
    public static final MutableIntLongMapFactory INSTANCE = new MutableIntLongMapFactoryImpl();

    @Override
    public MutableIntLongMap empty()
    {
        return new IntLongHashMap(0);
    }

    @Override
    public MutableIntLongMap of()
    {
        return this.empty();
    }

    @Override
    public MutableIntLongMap with()
    {
        return this.empty();
    }

    @Override
    public MutableIntLongMap with(int key, long value)
    {
        return IntLongHashMap.newWithKeysValues(key, value);
    }

    @Override
    public MutableIntLongMap of(int key, long value)
    {
        return this.with(key, value);
    }

    @Override
    public MutableIntLongMap of(int key1, long value1, int key2, long value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    @Override
    public MutableIntLongMap with(int key1, long value1, int key2, long value2)
    {
        return IntLongHashMap.newWithKeysValues(key1, value1, key2, value2);
    }

    @Override
    public MutableIntLongMap of(int key1, long value1, int key2, long value2, int key3, long value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableIntLongMap with(int key1, long value1, int key2, long value2, int key3, long value3)
    {
        return IntLongHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableIntLongMap of(
            int key1, long value1, int key2, long value2, int key3, long value3, int key4, long value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableIntLongMap with(
            int key1, long value1, int key2, long value2, int key3, long value3, int key4, long value4)
    {
        return IntLongHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableIntLongMap ofInitialCapacity(int capacity)
    {
        return this.withInitialCapacity(capacity);
    }

    @Override
    public MutableIntLongMap withInitialCapacity(int capacity)
    {
        return new IntLongHashMap(capacity);
    }

    @Override
    public MutableIntLongMap ofAll(IntLongMap map)
    {
        return this.withAll(map);
    }

    @Override
    public MutableIntLongMap withAll(IntLongMap map)
    {
        if (map.isEmpty())
        {
            return this.empty();
        }
        return new IntLongHashMap(map);
    }

    @Override
    public <T> MutableIntLongMap from(Iterable<T> iterable, IntFunction<? super T> keyFunction, LongFunction<? super T> valueFunction)
    {
        MutableIntLongMap map = IntLongMaps.mutable.empty();
        Iterate.forEach(iterable, each -> map.put(keyFunction.intValueOf(each), valueFunction.longValueOf(each)));
        return map;
    }
}
