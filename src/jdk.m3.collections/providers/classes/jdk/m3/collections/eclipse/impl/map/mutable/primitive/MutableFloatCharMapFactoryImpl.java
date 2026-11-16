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
import jdk.m3.collections.eclipse.api.block.function.primitive.CharFunction;
import jdk.m3.collections.eclipse.api.factory.map.primitive.MutableFloatCharMapFactory;
import jdk.m3.collections.eclipse.impl.factory.primitive.FloatCharMaps;
import jdk.m3.collections.eclipse.api.map.primitive.MutableFloatCharMap;
import jdk.m3.collections.eclipse.api.map.primitive.FloatCharMap;
import jdk.m3.collections.eclipse.impl.utility.Iterate;

/**
 * MutableFloatCharMapFactoryImpl is a factory implementation which creates instances of type {@link MutableFloatCharMap}.
 * This file was automatically generated from template file mutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 6.0.
 */
@SuppressWarnings("missing-explicit-ctor")
public class MutableFloatCharMapFactoryImpl implements MutableFloatCharMapFactory
{
    public static final MutableFloatCharMapFactory INSTANCE = new MutableFloatCharMapFactoryImpl();

    @Override
    public MutableFloatCharMap empty()
    {
        return new FloatCharHashMap(0);
    }

    @Override
    public MutableFloatCharMap of()
    {
        return this.empty();
    }

    @Override
    public MutableFloatCharMap with()
    {
        return this.empty();
    }

    @Override
    public MutableFloatCharMap with(float key, char value)
    {
        return FloatCharHashMap.newWithKeysValues(key, value);
    }

    @Override
    public MutableFloatCharMap of(float key, char value)
    {
        return this.with(key, value);
    }

    @Override
    public MutableFloatCharMap of(float key1, char value1, float key2, char value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    @Override
    public MutableFloatCharMap with(float key1, char value1, float key2, char value2)
    {
        return FloatCharHashMap.newWithKeysValues(key1, value1, key2, value2);
    }

    @Override
    public MutableFloatCharMap of(float key1, char value1, float key2, char value2, float key3, char value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableFloatCharMap with(float key1, char value1, float key2, char value2, float key3, char value3)
    {
        return FloatCharHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableFloatCharMap of(
            float key1, char value1, float key2, char value2, float key3, char value3, float key4, char value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableFloatCharMap with(
            float key1, char value1, float key2, char value2, float key3, char value3, float key4, char value4)
    {
        return FloatCharHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableFloatCharMap ofInitialCapacity(int capacity)
    {
        return this.withInitialCapacity(capacity);
    }

    @Override
    public MutableFloatCharMap withInitialCapacity(int capacity)
    {
        return new FloatCharHashMap(capacity);
    }

    @Override
    public MutableFloatCharMap ofAll(FloatCharMap map)
    {
        return this.withAll(map);
    }

    @Override
    public MutableFloatCharMap withAll(FloatCharMap map)
    {
        if (map.isEmpty())
        {
            return this.empty();
        }
        return new FloatCharHashMap(map);
    }

    @Override
    public <T> MutableFloatCharMap from(Iterable<T> iterable, FloatFunction<? super T> keyFunction, CharFunction<? super T> valueFunction)
    {
        MutableFloatCharMap map = FloatCharMaps.mutable.empty();
        Iterate.forEach(iterable, each -> map.put(keyFunction.floatValueOf(each), valueFunction.charValueOf(each)));
        return map;
    }
}
