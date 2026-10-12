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
import org.eclipse.collections.api.block.function.primitive.BooleanFunction;
import org.eclipse.collections.api.factory.map.primitive.MutableFloatBooleanMapFactory;
import org.eclipse.collections.impl.factory.primitive.FloatBooleanMaps;
import org.eclipse.collections.api.map.primitive.MutableFloatBooleanMap;
import org.eclipse.collections.api.map.primitive.FloatBooleanMap;
import org.eclipse.collections.impl.utility.Iterate;

/**
 * MutableFloatBooleanMapFactoryImpl is a factory implementation which creates instances of type {@link MutableFloatBooleanMap}.
 * This file was automatically generated from template file mutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 6.0.
 */
public class MutableFloatBooleanMapFactoryImpl implements MutableFloatBooleanMapFactory
{
    public static final MutableFloatBooleanMapFactory INSTANCE = new MutableFloatBooleanMapFactoryImpl();

    @Override
    public MutableFloatBooleanMap empty()
    {
        return new FloatBooleanHashMap(0);
    }

    @Override
    public MutableFloatBooleanMap of()
    {
        return this.empty();
    }

    @Override
    public MutableFloatBooleanMap with()
    {
        return this.empty();
    }

    @Override
    public MutableFloatBooleanMap with(float key, boolean value)
    {
        return FloatBooleanHashMap.newWithKeysValues(key, value);
    }

    @Override
    public MutableFloatBooleanMap of(float key, boolean value)
    {
        return this.with(key, value);
    }

    @Override
    public MutableFloatBooleanMap of(float key1, boolean value1, float key2, boolean value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    @Override
    public MutableFloatBooleanMap with(float key1, boolean value1, float key2, boolean value2)
    {
        return FloatBooleanHashMap.newWithKeysValues(key1, value1, key2, value2);
    }

    @Override
    public MutableFloatBooleanMap of(float key1, boolean value1, float key2, boolean value2, float key3, boolean value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableFloatBooleanMap with(float key1, boolean value1, float key2, boolean value2, float key3, boolean value3)
    {
        return FloatBooleanHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableFloatBooleanMap of(
            float key1, boolean value1, float key2, boolean value2, float key3, boolean value3, float key4, boolean value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableFloatBooleanMap with(
            float key1, boolean value1, float key2, boolean value2, float key3, boolean value3, float key4, boolean value4)
    {
        return FloatBooleanHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableFloatBooleanMap ofInitialCapacity(int capacity)
    {
        return this.withInitialCapacity(capacity);
    }

    @Override
    public MutableFloatBooleanMap withInitialCapacity(int capacity)
    {
        return new FloatBooleanHashMap(capacity);
    }

    @Override
    public MutableFloatBooleanMap ofAll(FloatBooleanMap map)
    {
        return this.withAll(map);
    }

    @Override
    public MutableFloatBooleanMap withAll(FloatBooleanMap map)
    {
        if (map.isEmpty())
        {
            return this.empty();
        }
        return new FloatBooleanHashMap(map);
    }

    @Override
    public <T> MutableFloatBooleanMap from(Iterable<T> iterable, FloatFunction<? super T> keyFunction, BooleanFunction<? super T> valueFunction)
    {
        MutableFloatBooleanMap map = FloatBooleanMaps.mutable.empty();
        Iterate.forEach(iterable, each -> map.put(keyFunction.floatValueOf(each), valueFunction.booleanValueOf(each)));
        return map;
    }
}
