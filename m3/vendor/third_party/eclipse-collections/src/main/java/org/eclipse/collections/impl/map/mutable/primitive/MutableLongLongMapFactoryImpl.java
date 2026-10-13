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
import org.eclipse.collections.api.factory.map.primitive.MutableLongLongMapFactory;
import org.eclipse.collections.impl.factory.primitive.LongLongMaps;
import org.eclipse.collections.api.map.primitive.MutableLongLongMap;
import org.eclipse.collections.api.map.primitive.LongLongMap;
import org.eclipse.collections.impl.utility.Iterate;

/**
 * MutableLongLongMapFactoryImpl is a factory implementation which creates instances of type {@link MutableLongLongMap}.
 * This file was automatically generated from template file mutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 6.0.
 */
public class MutableLongLongMapFactoryImpl implements MutableLongLongMapFactory
{
    public static final MutableLongLongMapFactory INSTANCE = new MutableLongLongMapFactoryImpl();

    @Override
    public MutableLongLongMap empty()
    {
        return new LongLongHashMap(0);
    }

    @Override
    public MutableLongLongMap of()
    {
        return this.empty();
    }

    @Override
    public MutableLongLongMap with()
    {
        return this.empty();
    }

    @Override
    public MutableLongLongMap with(long key, long value)
    {
        return LongLongHashMap.newWithKeysValues(key, value);
    }

    @Override
    public MutableLongLongMap of(long key, long value)
    {
        return this.with(key, value);
    }

    @Override
    public MutableLongLongMap of(long key1, long value1, long key2, long value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    @Override
    public MutableLongLongMap with(long key1, long value1, long key2, long value2)
    {
        return LongLongHashMap.newWithKeysValues(key1, value1, key2, value2);
    }

    @Override
    public MutableLongLongMap of(long key1, long value1, long key2, long value2, long key3, long value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableLongLongMap with(long key1, long value1, long key2, long value2, long key3, long value3)
    {
        return LongLongHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableLongLongMap of(
            long key1, long value1, long key2, long value2, long key3, long value3, long key4, long value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableLongLongMap with(
            long key1, long value1, long key2, long value2, long key3, long value3, long key4, long value4)
    {
        return LongLongHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableLongLongMap ofInitialCapacity(int capacity)
    {
        return this.withInitialCapacity(capacity);
    }

    @Override
    public MutableLongLongMap withInitialCapacity(int capacity)
    {
        return new LongLongHashMap(capacity);
    }

    @Override
    public MutableLongLongMap ofAll(LongLongMap map)
    {
        return this.withAll(map);
    }

    @Override
    public MutableLongLongMap withAll(LongLongMap map)
    {
        if (map.isEmpty())
        {
            return this.empty();
        }
        return new LongLongHashMap(map);
    }

    @Override
    public <T> MutableLongLongMap from(Iterable<T> iterable, LongFunction<? super T> keyFunction, LongFunction<? super T> valueFunction)
    {
        MutableLongLongMap map = LongLongMaps.mutable.empty();
        Iterate.forEach(iterable, each -> map.put(keyFunction.longValueOf(each), valueFunction.longValueOf(each)));
        return map;
    }
}
