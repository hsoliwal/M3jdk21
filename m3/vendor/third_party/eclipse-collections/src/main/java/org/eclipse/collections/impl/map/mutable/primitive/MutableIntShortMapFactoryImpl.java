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
import org.eclipse.collections.api.block.function.primitive.ShortFunction;
import org.eclipse.collections.api.factory.map.primitive.MutableIntShortMapFactory;
import org.eclipse.collections.impl.factory.primitive.IntShortMaps;
import org.eclipse.collections.api.map.primitive.MutableIntShortMap;
import org.eclipse.collections.api.map.primitive.IntShortMap;
import org.eclipse.collections.impl.utility.Iterate;

/**
 * MutableIntShortMapFactoryImpl is a factory implementation which creates instances of type {@link MutableIntShortMap}.
 * This file was automatically generated from template file mutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 6.0.
 */
public class MutableIntShortMapFactoryImpl implements MutableIntShortMapFactory
{
    public static final MutableIntShortMapFactory INSTANCE = new MutableIntShortMapFactoryImpl();

    @Override
    public MutableIntShortMap empty()
    {
        return new IntShortHashMap(0);
    }

    @Override
    public MutableIntShortMap of()
    {
        return this.empty();
    }

    @Override
    public MutableIntShortMap with()
    {
        return this.empty();
    }

    @Override
    public MutableIntShortMap with(int key, short value)
    {
        return IntShortHashMap.newWithKeysValues(key, value);
    }

    @Override
    public MutableIntShortMap of(int key, short value)
    {
        return this.with(key, value);
    }

    @Override
    public MutableIntShortMap of(int key1, short value1, int key2, short value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    @Override
    public MutableIntShortMap with(int key1, short value1, int key2, short value2)
    {
        return IntShortHashMap.newWithKeysValues(key1, value1, key2, value2);
    }

    @Override
    public MutableIntShortMap of(int key1, short value1, int key2, short value2, int key3, short value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableIntShortMap with(int key1, short value1, int key2, short value2, int key3, short value3)
    {
        return IntShortHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableIntShortMap of(
            int key1, short value1, int key2, short value2, int key3, short value3, int key4, short value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableIntShortMap with(
            int key1, short value1, int key2, short value2, int key3, short value3, int key4, short value4)
    {
        return IntShortHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableIntShortMap ofInitialCapacity(int capacity)
    {
        return this.withInitialCapacity(capacity);
    }

    @Override
    public MutableIntShortMap withInitialCapacity(int capacity)
    {
        return new IntShortHashMap(capacity);
    }

    @Override
    public MutableIntShortMap ofAll(IntShortMap map)
    {
        return this.withAll(map);
    }

    @Override
    public MutableIntShortMap withAll(IntShortMap map)
    {
        if (map.isEmpty())
        {
            return this.empty();
        }
        return new IntShortHashMap(map);
    }

    @Override
    public <T> MutableIntShortMap from(Iterable<T> iterable, IntFunction<? super T> keyFunction, ShortFunction<? super T> valueFunction)
    {
        MutableIntShortMap map = IntShortMaps.mutable.empty();
        Iterate.forEach(iterable, each -> map.put(keyFunction.intValueOf(each), valueFunction.shortValueOf(each)));
        return map;
    }
}
