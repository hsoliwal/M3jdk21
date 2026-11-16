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
import jdk.m3.collections.eclipse.api.block.function.primitive.ShortFunction;
import jdk.m3.collections.eclipse.api.factory.map.primitive.MutableFloatShortMapFactory;
import jdk.m3.collections.eclipse.impl.factory.primitive.FloatShortMaps;
import jdk.m3.collections.eclipse.api.map.primitive.MutableFloatShortMap;
import jdk.m3.collections.eclipse.api.map.primitive.FloatShortMap;
import jdk.m3.collections.eclipse.impl.utility.Iterate;

/**
 * MutableFloatShortMapFactoryImpl is a factory implementation which creates instances of type {@link MutableFloatShortMap}.
 * This file was automatically generated from template file mutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 6.0.
 */
@SuppressWarnings("missing-explicit-ctor")
public class MutableFloatShortMapFactoryImpl implements MutableFloatShortMapFactory
{
    public static final MutableFloatShortMapFactory INSTANCE = new MutableFloatShortMapFactoryImpl();

    @Override
    public MutableFloatShortMap empty()
    {
        return new FloatShortHashMap(0);
    }

    @Override
    public MutableFloatShortMap of()
    {
        return this.empty();
    }

    @Override
    public MutableFloatShortMap with()
    {
        return this.empty();
    }

    @Override
    public MutableFloatShortMap with(float key, short value)
    {
        return FloatShortHashMap.newWithKeysValues(key, value);
    }

    @Override
    public MutableFloatShortMap of(float key, short value)
    {
        return this.with(key, value);
    }

    @Override
    public MutableFloatShortMap of(float key1, short value1, float key2, short value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    @Override
    public MutableFloatShortMap with(float key1, short value1, float key2, short value2)
    {
        return FloatShortHashMap.newWithKeysValues(key1, value1, key2, value2);
    }

    @Override
    public MutableFloatShortMap of(float key1, short value1, float key2, short value2, float key3, short value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableFloatShortMap with(float key1, short value1, float key2, short value2, float key3, short value3)
    {
        return FloatShortHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableFloatShortMap of(
            float key1, short value1, float key2, short value2, float key3, short value3, float key4, short value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableFloatShortMap with(
            float key1, short value1, float key2, short value2, float key3, short value3, float key4, short value4)
    {
        return FloatShortHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableFloatShortMap ofInitialCapacity(int capacity)
    {
        return this.withInitialCapacity(capacity);
    }

    @Override
    public MutableFloatShortMap withInitialCapacity(int capacity)
    {
        return new FloatShortHashMap(capacity);
    }

    @Override
    public MutableFloatShortMap ofAll(FloatShortMap map)
    {
        return this.withAll(map);
    }

    @Override
    public MutableFloatShortMap withAll(FloatShortMap map)
    {
        if (map.isEmpty())
        {
            return this.empty();
        }
        return new FloatShortHashMap(map);
    }

    @Override
    public <T> MutableFloatShortMap from(Iterable<T> iterable, FloatFunction<? super T> keyFunction, ShortFunction<? super T> valueFunction)
    {
        MutableFloatShortMap map = FloatShortMaps.mutable.empty();
        Iterate.forEach(iterable, each -> map.put(keyFunction.floatValueOf(each), valueFunction.shortValueOf(each)));
        return map;
    }
}
