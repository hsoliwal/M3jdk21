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

import jdk.m3.collections.eclipse.api.block.function.primitive.FloatFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.LongFunction;
import jdk.m3.collections.eclipse.api.factory.map.primitive.MutableFloatLongMapFactory;
import jdk.m3.collections.eclipse.impl.factory.primitive.FloatLongMaps;
import jdk.m3.collections.eclipse.api.map.primitive.MutableFloatLongMap;
import jdk.m3.collections.eclipse.api.map.primitive.FloatLongMap;
import jdk.m3.collections.eclipse.impl.utility.Iterate;

/**
 * MutableFloatLongMapFactoryImpl is a factory implementation which creates instances of type {@link MutableFloatLongMap}.
 * This file was automatically generated from template file mutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 6.0.
 */
@SuppressWarnings("missing-explicit-ctor")
public class MutableFloatLongMapFactoryImpl implements MutableFloatLongMapFactory
{
    public static final MutableFloatLongMapFactory INSTANCE = new MutableFloatLongMapFactoryImpl();

    @Override
    public MutableFloatLongMap empty()
    {
        return new FloatLongHashMap(0);
    }

    @Override
    public MutableFloatLongMap of()
    {
        return this.empty();
    }

    @Override
    public MutableFloatLongMap with()
    {
        return this.empty();
    }

    @Override
    public MutableFloatLongMap with(float key, long value)
    {
        return FloatLongHashMap.newWithKeysValues(key, value);
    }

    @Override
    public MutableFloatLongMap of(float key, long value)
    {
        return this.with(key, value);
    }

    @Override
    public MutableFloatLongMap of(float key1, long value1, float key2, long value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    @Override
    public MutableFloatLongMap with(float key1, long value1, float key2, long value2)
    {
        return FloatLongHashMap.newWithKeysValues(key1, value1, key2, value2);
    }

    @Override
    public MutableFloatLongMap of(float key1, long value1, float key2, long value2, float key3, long value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableFloatLongMap with(float key1, long value1, float key2, long value2, float key3, long value3)
    {
        return FloatLongHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableFloatLongMap of(
            float key1, long value1, float key2, long value2, float key3, long value3, float key4, long value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableFloatLongMap with(
            float key1, long value1, float key2, long value2, float key3, long value3, float key4, long value4)
    {
        return FloatLongHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableFloatLongMap ofInitialCapacity(int capacity)
    {
        return this.withInitialCapacity(capacity);
    }

    @Override
    public MutableFloatLongMap withInitialCapacity(int capacity)
    {
        return new FloatLongHashMap(capacity);
    }

    @Override
    public MutableFloatLongMap ofAll(FloatLongMap map)
    {
        return this.withAll(map);
    }

    @Override
    public MutableFloatLongMap withAll(FloatLongMap map)
    {
        if (map.isEmpty())
        {
            return this.empty();
        }
        return new FloatLongHashMap(map);
    }

    @Override
    public <T> MutableFloatLongMap from(Iterable<T> iterable, FloatFunction<? super T> keyFunction, LongFunction<? super T> valueFunction)
    {
        MutableFloatLongMap map = FloatLongMaps.mutable.empty();
        Iterate.forEach(iterable, each -> map.put(keyFunction.floatValueOf(each), valueFunction.longValueOf(each)));
        return map;
    }
}
