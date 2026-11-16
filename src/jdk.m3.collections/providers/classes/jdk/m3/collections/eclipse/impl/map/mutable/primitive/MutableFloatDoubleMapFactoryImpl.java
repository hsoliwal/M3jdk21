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
import jdk.m3.collections.eclipse.api.block.function.primitive.DoubleFunction;
import jdk.m3.collections.eclipse.api.factory.map.primitive.MutableFloatDoubleMapFactory;
import jdk.m3.collections.eclipse.impl.factory.primitive.FloatDoubleMaps;
import jdk.m3.collections.eclipse.api.map.primitive.MutableFloatDoubleMap;
import jdk.m3.collections.eclipse.api.map.primitive.FloatDoubleMap;
import jdk.m3.collections.eclipse.impl.utility.Iterate;

/**
 * MutableFloatDoubleMapFactoryImpl is a factory implementation which creates instances of type {@link MutableFloatDoubleMap}.
 * This file was automatically generated from template file mutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 6.0.
 */
@SuppressWarnings("missing-explicit-ctor")
public class MutableFloatDoubleMapFactoryImpl implements MutableFloatDoubleMapFactory
{
    public static final MutableFloatDoubleMapFactory INSTANCE = new MutableFloatDoubleMapFactoryImpl();

    @Override
    public MutableFloatDoubleMap empty()
    {
        return new FloatDoubleHashMap(0);
    }

    @Override
    public MutableFloatDoubleMap of()
    {
        return this.empty();
    }

    @Override
    public MutableFloatDoubleMap with()
    {
        return this.empty();
    }

    @Override
    public MutableFloatDoubleMap with(float key, double value)
    {
        return FloatDoubleHashMap.newWithKeysValues(key, value);
    }

    @Override
    public MutableFloatDoubleMap of(float key, double value)
    {
        return this.with(key, value);
    }

    @Override
    public MutableFloatDoubleMap of(float key1, double value1, float key2, double value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    @Override
    public MutableFloatDoubleMap with(float key1, double value1, float key2, double value2)
    {
        return FloatDoubleHashMap.newWithKeysValues(key1, value1, key2, value2);
    }

    @Override
    public MutableFloatDoubleMap of(float key1, double value1, float key2, double value2, float key3, double value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableFloatDoubleMap with(float key1, double value1, float key2, double value2, float key3, double value3)
    {
        return FloatDoubleHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableFloatDoubleMap of(
            float key1, double value1, float key2, double value2, float key3, double value3, float key4, double value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableFloatDoubleMap with(
            float key1, double value1, float key2, double value2, float key3, double value3, float key4, double value4)
    {
        return FloatDoubleHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableFloatDoubleMap ofInitialCapacity(int capacity)
    {
        return this.withInitialCapacity(capacity);
    }

    @Override
    public MutableFloatDoubleMap withInitialCapacity(int capacity)
    {
        return new FloatDoubleHashMap(capacity);
    }

    @Override
    public MutableFloatDoubleMap ofAll(FloatDoubleMap map)
    {
        return this.withAll(map);
    }

    @Override
    public MutableFloatDoubleMap withAll(FloatDoubleMap map)
    {
        if (map.isEmpty())
        {
            return this.empty();
        }
        return new FloatDoubleHashMap(map);
    }

    @Override
    public <T> MutableFloatDoubleMap from(Iterable<T> iterable, FloatFunction<? super T> keyFunction, DoubleFunction<? super T> valueFunction)
    {
        MutableFloatDoubleMap map = FloatDoubleMaps.mutable.empty();
        Iterate.forEach(iterable, each -> map.put(keyFunction.floatValueOf(each), valueFunction.doubleValueOf(each)));
        return map;
    }
}
