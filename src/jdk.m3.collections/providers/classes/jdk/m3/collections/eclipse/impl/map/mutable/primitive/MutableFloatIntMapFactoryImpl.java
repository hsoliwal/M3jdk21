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
import jdk.m3.collections.eclipse.api.block.function.primitive.IntFunction;
import jdk.m3.collections.eclipse.api.factory.map.primitive.MutableFloatIntMapFactory;
import jdk.m3.collections.eclipse.impl.factory.primitive.FloatIntMaps;
import jdk.m3.collections.eclipse.api.map.primitive.MutableFloatIntMap;
import jdk.m3.collections.eclipse.api.map.primitive.FloatIntMap;
import jdk.m3.collections.eclipse.impl.utility.Iterate;

/**
 * MutableFloatIntMapFactoryImpl is a factory implementation which creates instances of type {@link MutableFloatIntMap}.
 * This file was automatically generated from template file mutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 6.0.
 */
@SuppressWarnings("missing-explicit-ctor")
public class MutableFloatIntMapFactoryImpl implements MutableFloatIntMapFactory
{
    public static final MutableFloatIntMapFactory INSTANCE = new MutableFloatIntMapFactoryImpl();

    @Override
    public MutableFloatIntMap empty()
    {
        return new FloatIntHashMap(0);
    }

    @Override
    public MutableFloatIntMap of()
    {
        return this.empty();
    }

    @Override
    public MutableFloatIntMap with()
    {
        return this.empty();
    }

    @Override
    public MutableFloatIntMap with(float key, int value)
    {
        return FloatIntHashMap.newWithKeysValues(key, value);
    }

    @Override
    public MutableFloatIntMap of(float key, int value)
    {
        return this.with(key, value);
    }

    @Override
    public MutableFloatIntMap of(float key1, int value1, float key2, int value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    @Override
    public MutableFloatIntMap with(float key1, int value1, float key2, int value2)
    {
        return FloatIntHashMap.newWithKeysValues(key1, value1, key2, value2);
    }

    @Override
    public MutableFloatIntMap of(float key1, int value1, float key2, int value2, float key3, int value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableFloatIntMap with(float key1, int value1, float key2, int value2, float key3, int value3)
    {
        return FloatIntHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableFloatIntMap of(
            float key1, int value1, float key2, int value2, float key3, int value3, float key4, int value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableFloatIntMap with(
            float key1, int value1, float key2, int value2, float key3, int value3, float key4, int value4)
    {
        return FloatIntHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableFloatIntMap ofInitialCapacity(int capacity)
    {
        return this.withInitialCapacity(capacity);
    }

    @Override
    public MutableFloatIntMap withInitialCapacity(int capacity)
    {
        return new FloatIntHashMap(capacity);
    }

    @Override
    public MutableFloatIntMap ofAll(FloatIntMap map)
    {
        return this.withAll(map);
    }

    @Override
    public MutableFloatIntMap withAll(FloatIntMap map)
    {
        if (map.isEmpty())
        {
            return this.empty();
        }
        return new FloatIntHashMap(map);
    }

    @Override
    public <T> MutableFloatIntMap from(Iterable<T> iterable, FloatFunction<? super T> keyFunction, IntFunction<? super T> valueFunction)
    {
        MutableFloatIntMap map = FloatIntMaps.mutable.empty();
        Iterate.forEach(iterable, each -> map.put(keyFunction.floatValueOf(each), valueFunction.intValueOf(each)));
        return map;
    }
}
