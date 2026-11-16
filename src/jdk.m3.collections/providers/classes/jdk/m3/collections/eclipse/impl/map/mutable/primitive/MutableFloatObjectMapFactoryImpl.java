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

import jdk.m3.collections.eclipse.api.block.function.Function;
import jdk.m3.collections.eclipse.api.block.function.primitive.FloatFunction;
import jdk.m3.collections.eclipse.api.factory.map.primitive.MutableFloatObjectMapFactory;
import jdk.m3.collections.eclipse.api.map.primitive.MutableFloatObjectMap;
import jdk.m3.collections.eclipse.api.map.primitive.FloatObjectMap;
import jdk.m3.collections.eclipse.impl.factory.primitive.FloatObjectMaps;
import jdk.m3.collections.eclipse.impl.utility.Iterate;

/**
 * MutableFloatObjectMapFactoryImpl is a factory implementation which creates instances of type {@link MutableFloatObjectMap}.
 * This file was automatically generated from template file mutablePrimitiveObjectMapFactoryImpl.stg.
 *
 * @since 6.0.
 */
@SuppressWarnings({"missing-explicit-ctor", "rawtypes", "unchecked"})
public class MutableFloatObjectMapFactoryImpl implements MutableFloatObjectMapFactory
{
    public static final MutableFloatObjectMapFactory INSTANCE = new MutableFloatObjectMapFactoryImpl();

    @Override
    public <V> MutableFloatObjectMap<V> empty()
    {
        return new FloatObjectHashMap(0);
    }

    @Override
    public <V> MutableFloatObjectMap<V> of()
    {
        return this.empty();
    }

    @Override
    public <V> MutableFloatObjectMap<V> with()
    {
        return this.empty();
    }

    @Override
    public <V> MutableFloatObjectMap<V> of(float key, V value)
    {
        return this.with(key, value);
    }

    @Override
    public <V> MutableFloatObjectMap<V> with(float key, V value)
    {
        return FloatObjectHashMap.newWithKeysValues(key, value);
    }

    @Override
    public <V> MutableFloatObjectMap<V> of(float key1, V value1, float key2, V value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    @Override
    public <V> MutableFloatObjectMap<V> with(float key1, V value1, float key2, V value2)
    {
        return FloatObjectHashMap.newWithKeysValues(key1, value1, key2, value2);
    }

    @Override
    public <V> MutableFloatObjectMap<V> of(float key1, V value1, float key2, V value2, float key3, V value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public <V> MutableFloatObjectMap<V> with(float key1, V value1, float key2, V value2, float key3, V value3)
    {
        return FloatObjectHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public <V> MutableFloatObjectMap<V> of(float key1, V value1, float key2, V value2, float key3, V value3, float key4, V value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public <V> MutableFloatObjectMap<V> with(float key1, V value1, float key2, V value2, float key3, V value3, float key4, V value4)
    {
        return FloatObjectHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public <V> MutableFloatObjectMap<V> ofInitialCapacity(int capacity)
    {
        return this.withInitialCapacity(capacity);
    }

    @Override
    public <V> MutableFloatObjectMap<V> withInitialCapacity(int capacity)
    {
        return new FloatObjectHashMap<>(capacity);
    }

    @Override
    public <V> MutableFloatObjectMap<V> ofAll(FloatObjectMap<? extends V> map)
    {
        return this.withAll(map);
    }

    @Override
    public <V> MutableFloatObjectMap<V> withAll(FloatObjectMap<? extends V> map)
    {
        if (map.isEmpty())
        {
            return this.empty();
        }
        return new FloatObjectHashMap<>(map);
    }

    @Override
    public <T, V> MutableFloatObjectMap<V> from(Iterable<T> iterable, FloatFunction<? super T> keyFunction, Function<? super T, ? extends V> valueFunction)
    {
        MutableFloatObjectMap<V> map = FloatObjectMaps.mutable.empty();
        Iterate.forEach(iterable, each -> map.put(keyFunction.floatValueOf(each), valueFunction.valueOf(each)));
        return map;
    }
}
