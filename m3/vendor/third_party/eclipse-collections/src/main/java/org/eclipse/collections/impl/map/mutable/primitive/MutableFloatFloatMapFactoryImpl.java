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
import org.eclipse.collections.api.factory.map.primitive.MutableFloatFloatMapFactory;
import org.eclipse.collections.impl.factory.primitive.FloatFloatMaps;
import org.eclipse.collections.api.map.primitive.MutableFloatFloatMap;
import org.eclipse.collections.api.map.primitive.FloatFloatMap;
import org.eclipse.collections.impl.utility.Iterate;

/**
 * MutableFloatFloatMapFactoryImpl is a factory implementation which creates instances of type {@link MutableFloatFloatMap}.
 * This file was automatically generated from template file mutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 6.0.
 */
public class MutableFloatFloatMapFactoryImpl implements MutableFloatFloatMapFactory
{
    public static final MutableFloatFloatMapFactory INSTANCE = new MutableFloatFloatMapFactoryImpl();

    @Override
    public MutableFloatFloatMap empty()
    {
        return new FloatFloatHashMap(0);
    }

    @Override
    public MutableFloatFloatMap of()
    {
        return this.empty();
    }

    @Override
    public MutableFloatFloatMap with()
    {
        return this.empty();
    }

    @Override
    public MutableFloatFloatMap with(float key, float value)
    {
        return FloatFloatHashMap.newWithKeysValues(key, value);
    }

    @Override
    public MutableFloatFloatMap of(float key, float value)
    {
        return this.with(key, value);
    }

    @Override
    public MutableFloatFloatMap of(float key1, float value1, float key2, float value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    @Override
    public MutableFloatFloatMap with(float key1, float value1, float key2, float value2)
    {
        return FloatFloatHashMap.newWithKeysValues(key1, value1, key2, value2);
    }

    @Override
    public MutableFloatFloatMap of(float key1, float value1, float key2, float value2, float key3, float value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableFloatFloatMap with(float key1, float value1, float key2, float value2, float key3, float value3)
    {
        return FloatFloatHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableFloatFloatMap of(
            float key1, float value1, float key2, float value2, float key3, float value3, float key4, float value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableFloatFloatMap with(
            float key1, float value1, float key2, float value2, float key3, float value3, float key4, float value4)
    {
        return FloatFloatHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableFloatFloatMap ofInitialCapacity(int capacity)
    {
        return this.withInitialCapacity(capacity);
    }

    @Override
    public MutableFloatFloatMap withInitialCapacity(int capacity)
    {
        return new FloatFloatHashMap(capacity);
    }

    @Override
    public MutableFloatFloatMap ofAll(FloatFloatMap map)
    {
        return this.withAll(map);
    }

    @Override
    public MutableFloatFloatMap withAll(FloatFloatMap map)
    {
        if (map.isEmpty())
        {
            return this.empty();
        }
        return new FloatFloatHashMap(map);
    }

    @Override
    public <T> MutableFloatFloatMap from(Iterable<T> iterable, FloatFunction<? super T> keyFunction, FloatFunction<? super T> valueFunction)
    {
        MutableFloatFloatMap map = FloatFloatMaps.mutable.empty();
        Iterate.forEach(iterable, each -> map.put(keyFunction.floatValueOf(each), valueFunction.floatValueOf(each)));
        return map;
    }
}
