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
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.factory.map.primitive.MutableLongFloatMapFactory;
import org.eclipse.collections.impl.factory.primitive.LongFloatMaps;
import org.eclipse.collections.api.map.primitive.MutableLongFloatMap;
import org.eclipse.collections.api.map.primitive.LongFloatMap;
import org.eclipse.collections.impl.utility.Iterate;

/**
 * MutableLongFloatMapFactoryImpl is a factory implementation which creates instances of type {@link MutableLongFloatMap}.
 * This file was automatically generated from template file mutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 6.0.
 */
public class MutableLongFloatMapFactoryImpl implements MutableLongFloatMapFactory
{
    public static final MutableLongFloatMapFactory INSTANCE = new MutableLongFloatMapFactoryImpl();

    @Override
    public MutableLongFloatMap empty()
    {
        return new LongFloatHashMap(0);
    }

    @Override
    public MutableLongFloatMap of()
    {
        return this.empty();
    }

    @Override
    public MutableLongFloatMap with()
    {
        return this.empty();
    }

    @Override
    public MutableLongFloatMap with(long key, float value)
    {
        return LongFloatHashMap.newWithKeysValues(key, value);
    }

    @Override
    public MutableLongFloatMap of(long key, float value)
    {
        return this.with(key, value);
    }

    @Override
    public MutableLongFloatMap of(long key1, float value1, long key2, float value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    @Override
    public MutableLongFloatMap with(long key1, float value1, long key2, float value2)
    {
        return LongFloatHashMap.newWithKeysValues(key1, value1, key2, value2);
    }

    @Override
    public MutableLongFloatMap of(long key1, float value1, long key2, float value2, long key3, float value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableLongFloatMap with(long key1, float value1, long key2, float value2, long key3, float value3)
    {
        return LongFloatHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableLongFloatMap of(
            long key1, float value1, long key2, float value2, long key3, float value3, long key4, float value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableLongFloatMap with(
            long key1, float value1, long key2, float value2, long key3, float value3, long key4, float value4)
    {
        return LongFloatHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableLongFloatMap ofInitialCapacity(int capacity)
    {
        return this.withInitialCapacity(capacity);
    }

    @Override
    public MutableLongFloatMap withInitialCapacity(int capacity)
    {
        return new LongFloatHashMap(capacity);
    }

    @Override
    public MutableLongFloatMap ofAll(LongFloatMap map)
    {
        return this.withAll(map);
    }

    @Override
    public MutableLongFloatMap withAll(LongFloatMap map)
    {
        if (map.isEmpty())
        {
            return this.empty();
        }
        return new LongFloatHashMap(map);
    }

    @Override
    public <T> MutableLongFloatMap from(Iterable<T> iterable, LongFunction<? super T> keyFunction, FloatFunction<? super T> valueFunction)
    {
        MutableLongFloatMap map = LongFloatMaps.mutable.empty();
        Iterate.forEach(iterable, each -> map.put(keyFunction.longValueOf(each), valueFunction.floatValueOf(each)));
        return map;
    }
}
