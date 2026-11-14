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

import org.eclipse.collections.api.block.function.primitive.DoubleFunction;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.factory.map.primitive.MutableDoubleFloatMapFactory;
import org.eclipse.collections.impl.factory.primitive.DoubleFloatMaps;
import org.eclipse.collections.api.map.primitive.MutableDoubleFloatMap;
import org.eclipse.collections.api.map.primitive.DoubleFloatMap;
import org.eclipse.collections.impl.utility.Iterate;

/**
 * MutableDoubleFloatMapFactoryImpl is a factory implementation which creates instances of type {@link MutableDoubleFloatMap}.
 * This file was automatically generated from template file mutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 6.0.
 */
public class MutableDoubleFloatMapFactoryImpl implements MutableDoubleFloatMapFactory
{
    public static final MutableDoubleFloatMapFactory INSTANCE = new MutableDoubleFloatMapFactoryImpl();

    @Override
    public MutableDoubleFloatMap empty()
    {
        return new DoubleFloatHashMap(0);
    }

    @Override
    public MutableDoubleFloatMap of()
    {
        return this.empty();
    }

    @Override
    public MutableDoubleFloatMap with()
    {
        return this.empty();
    }

    @Override
    public MutableDoubleFloatMap with(double key, float value)
    {
        return DoubleFloatHashMap.newWithKeysValues(key, value);
    }

    @Override
    public MutableDoubleFloatMap of(double key, float value)
    {
        return this.with(key, value);
    }

    @Override
    public MutableDoubleFloatMap of(double key1, float value1, double key2, float value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    @Override
    public MutableDoubleFloatMap with(double key1, float value1, double key2, float value2)
    {
        return DoubleFloatHashMap.newWithKeysValues(key1, value1, key2, value2);
    }

    @Override
    public MutableDoubleFloatMap of(double key1, float value1, double key2, float value2, double key3, float value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableDoubleFloatMap with(double key1, float value1, double key2, float value2, double key3, float value3)
    {
        return DoubleFloatHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableDoubleFloatMap of(
            double key1, float value1, double key2, float value2, double key3, float value3, double key4, float value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableDoubleFloatMap with(
            double key1, float value1, double key2, float value2, double key3, float value3, double key4, float value4)
    {
        return DoubleFloatHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableDoubleFloatMap ofInitialCapacity(int capacity)
    {
        return this.withInitialCapacity(capacity);
    }

    @Override
    public MutableDoubleFloatMap withInitialCapacity(int capacity)
    {
        return new DoubleFloatHashMap(capacity);
    }

    @Override
    public MutableDoubleFloatMap ofAll(DoubleFloatMap map)
    {
        return this.withAll(map);
    }

    @Override
    public MutableDoubleFloatMap withAll(DoubleFloatMap map)
    {
        if (map.isEmpty())
        {
            return this.empty();
        }
        return new DoubleFloatHashMap(map);
    }

    @Override
    public <T> MutableDoubleFloatMap from(Iterable<T> iterable, DoubleFunction<? super T> keyFunction, FloatFunction<? super T> valueFunction)
    {
        MutableDoubleFloatMap map = DoubleFloatMaps.mutable.empty();
        Iterate.forEach(iterable, each -> map.put(keyFunction.doubleValueOf(each), valueFunction.floatValueOf(each)));
        return map;
    }
}
