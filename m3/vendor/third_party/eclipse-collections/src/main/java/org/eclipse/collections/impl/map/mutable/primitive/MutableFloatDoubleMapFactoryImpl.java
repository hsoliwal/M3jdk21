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

import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.block.function.primitive.DoubleFunction;
import org.eclipse.collections.api.factory.map.primitive.MutableFloatDoubleMapFactory;
import org.eclipse.collections.impl.factory.primitive.FloatDoubleMaps;
import org.eclipse.collections.api.map.primitive.MutableFloatDoubleMap;
import org.eclipse.collections.api.map.primitive.FloatDoubleMap;
import org.eclipse.collections.impl.utility.Iterate;

/**
 * MutableFloatDoubleMapFactoryImpl is a factory implementation which creates instances of type {@link MutableFloatDoubleMap}.
 * This file was automatically generated from template file mutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 6.0.
 */
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
