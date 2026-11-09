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
import org.eclipse.collections.api.block.function.primitive.IntFunction;
import org.eclipse.collections.api.factory.map.primitive.MutableFloatIntMapFactory;
import org.eclipse.collections.impl.factory.primitive.FloatIntMaps;
import org.eclipse.collections.api.map.primitive.MutableFloatIntMap;
import org.eclipse.collections.api.map.primitive.FloatIntMap;
import org.eclipse.collections.impl.utility.Iterate;

/**
 * MutableFloatIntMapFactoryImpl is a factory implementation which creates instances of type {@link MutableFloatIntMap}.
 * This file was automatically generated from template file mutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 6.0.
 */
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
