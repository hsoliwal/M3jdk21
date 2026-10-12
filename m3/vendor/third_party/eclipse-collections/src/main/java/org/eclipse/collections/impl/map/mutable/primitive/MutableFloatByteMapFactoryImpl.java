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
import org.eclipse.collections.api.block.function.primitive.ByteFunction;
import org.eclipse.collections.api.factory.map.primitive.MutableFloatByteMapFactory;
import org.eclipse.collections.impl.factory.primitive.FloatByteMaps;
import org.eclipse.collections.api.map.primitive.MutableFloatByteMap;
import org.eclipse.collections.api.map.primitive.FloatByteMap;
import org.eclipse.collections.impl.utility.Iterate;

/**
 * MutableFloatByteMapFactoryImpl is a factory implementation which creates instances of type {@link MutableFloatByteMap}.
 * This file was automatically generated from template file mutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 6.0.
 */
public class MutableFloatByteMapFactoryImpl implements MutableFloatByteMapFactory
{
    public static final MutableFloatByteMapFactory INSTANCE = new MutableFloatByteMapFactoryImpl();

    @Override
    public MutableFloatByteMap empty()
    {
        return new FloatByteHashMap(0);
    }

    @Override
    public MutableFloatByteMap of()
    {
        return this.empty();
    }

    @Override
    public MutableFloatByteMap with()
    {
        return this.empty();
    }

    @Override
    public MutableFloatByteMap with(float key, byte value)
    {
        return FloatByteHashMap.newWithKeysValues(key, value);
    }

    @Override
    public MutableFloatByteMap of(float key, byte value)
    {
        return this.with(key, value);
    }

    @Override
    public MutableFloatByteMap of(float key1, byte value1, float key2, byte value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    @Override
    public MutableFloatByteMap with(float key1, byte value1, float key2, byte value2)
    {
        return FloatByteHashMap.newWithKeysValues(key1, value1, key2, value2);
    }

    @Override
    public MutableFloatByteMap of(float key1, byte value1, float key2, byte value2, float key3, byte value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableFloatByteMap with(float key1, byte value1, float key2, byte value2, float key3, byte value3)
    {
        return FloatByteHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableFloatByteMap of(
            float key1, byte value1, float key2, byte value2, float key3, byte value3, float key4, byte value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableFloatByteMap with(
            float key1, byte value1, float key2, byte value2, float key3, byte value3, float key4, byte value4)
    {
        return FloatByteHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableFloatByteMap ofInitialCapacity(int capacity)
    {
        return this.withInitialCapacity(capacity);
    }

    @Override
    public MutableFloatByteMap withInitialCapacity(int capacity)
    {
        return new FloatByteHashMap(capacity);
    }

    @Override
    public MutableFloatByteMap ofAll(FloatByteMap map)
    {
        return this.withAll(map);
    }

    @Override
    public MutableFloatByteMap withAll(FloatByteMap map)
    {
        if (map.isEmpty())
        {
            return this.empty();
        }
        return new FloatByteHashMap(map);
    }

    @Override
    public <T> MutableFloatByteMap from(Iterable<T> iterable, FloatFunction<? super T> keyFunction, ByteFunction<? super T> valueFunction)
    {
        MutableFloatByteMap map = FloatByteMaps.mutable.empty();
        Iterate.forEach(iterable, each -> map.put(keyFunction.floatValueOf(each), valueFunction.byteValueOf(each)));
        return map;
    }
}
