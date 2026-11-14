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

import org.eclipse.collections.api.block.function.primitive.LongFunction;
import org.eclipse.collections.api.block.function.primitive.ShortFunction;
import org.eclipse.collections.api.factory.map.primitive.MutableLongShortMapFactory;
import org.eclipse.collections.impl.factory.primitive.LongShortMaps;
import org.eclipse.collections.api.map.primitive.MutableLongShortMap;
import org.eclipse.collections.api.map.primitive.LongShortMap;
import org.eclipse.collections.impl.utility.Iterate;

/**
 * MutableLongShortMapFactoryImpl is a factory implementation which creates instances of type {@link MutableLongShortMap}.
 * This file was automatically generated from template file mutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 6.0.
 */
public class MutableLongShortMapFactoryImpl implements MutableLongShortMapFactory
{
    public static final MutableLongShortMapFactory INSTANCE = new MutableLongShortMapFactoryImpl();

    @Override
    public MutableLongShortMap empty()
    {
        return new LongShortHashMap(0);
    }

    @Override
    public MutableLongShortMap of()
    {
        return this.empty();
    }

    @Override
    public MutableLongShortMap with()
    {
        return this.empty();
    }

    @Override
    public MutableLongShortMap with(long key, short value)
    {
        return LongShortHashMap.newWithKeysValues(key, value);
    }

    @Override
    public MutableLongShortMap of(long key, short value)
    {
        return this.with(key, value);
    }

    @Override
    public MutableLongShortMap of(long key1, short value1, long key2, short value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    @Override
    public MutableLongShortMap with(long key1, short value1, long key2, short value2)
    {
        return LongShortHashMap.newWithKeysValues(key1, value1, key2, value2);
    }

    @Override
    public MutableLongShortMap of(long key1, short value1, long key2, short value2, long key3, short value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableLongShortMap with(long key1, short value1, long key2, short value2, long key3, short value3)
    {
        return LongShortHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableLongShortMap of(
            long key1, short value1, long key2, short value2, long key3, short value3, long key4, short value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableLongShortMap with(
            long key1, short value1, long key2, short value2, long key3, short value3, long key4, short value4)
    {
        return LongShortHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableLongShortMap ofInitialCapacity(int capacity)
    {
        return this.withInitialCapacity(capacity);
    }

    @Override
    public MutableLongShortMap withInitialCapacity(int capacity)
    {
        return new LongShortHashMap(capacity);
    }

    @Override
    public MutableLongShortMap ofAll(LongShortMap map)
    {
        return this.withAll(map);
    }

    @Override
    public MutableLongShortMap withAll(LongShortMap map)
    {
        if (map.isEmpty())
        {
            return this.empty();
        }
        return new LongShortHashMap(map);
    }

    @Override
    public <T> MutableLongShortMap from(Iterable<T> iterable, LongFunction<? super T> keyFunction, ShortFunction<? super T> valueFunction)
    {
        MutableLongShortMap map = LongShortMaps.mutable.empty();
        Iterate.forEach(iterable, each -> map.put(keyFunction.longValueOf(each), valueFunction.shortValueOf(each)));
        return map;
    }
}
