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
import org.eclipse.collections.api.block.function.primitive.IntFunction;
import org.eclipse.collections.api.factory.map.primitive.MutableDoubleIntMapFactory;
import org.eclipse.collections.impl.factory.primitive.DoubleIntMaps;
import org.eclipse.collections.api.map.primitive.MutableDoubleIntMap;
import org.eclipse.collections.api.map.primitive.DoubleIntMap;
import org.eclipse.collections.impl.utility.Iterate;

/**
 * MutableDoubleIntMapFactoryImpl is a factory implementation which creates instances of type {@link MutableDoubleIntMap}.
 * This file was automatically generated from template file mutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 6.0.
 */
public class MutableDoubleIntMapFactoryImpl implements MutableDoubleIntMapFactory
{
    public static final MutableDoubleIntMapFactory INSTANCE = new MutableDoubleIntMapFactoryImpl();

    @Override
    public MutableDoubleIntMap empty()
    {
        return new DoubleIntHashMap(0);
    }

    @Override
    public MutableDoubleIntMap of()
    {
        return this.empty();
    }

    @Override
    public MutableDoubleIntMap with()
    {
        return this.empty();
    }

    @Override
    public MutableDoubleIntMap with(double key, int value)
    {
        return DoubleIntHashMap.newWithKeysValues(key, value);
    }

    @Override
    public MutableDoubleIntMap of(double key, int value)
    {
        return this.with(key, value);
    }

    @Override
    public MutableDoubleIntMap of(double key1, int value1, double key2, int value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    @Override
    public MutableDoubleIntMap with(double key1, int value1, double key2, int value2)
    {
        return DoubleIntHashMap.newWithKeysValues(key1, value1, key2, value2);
    }

    @Override
    public MutableDoubleIntMap of(double key1, int value1, double key2, int value2, double key3, int value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableDoubleIntMap with(double key1, int value1, double key2, int value2, double key3, int value3)
    {
        return DoubleIntHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableDoubleIntMap of(
            double key1, int value1, double key2, int value2, double key3, int value3, double key4, int value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableDoubleIntMap with(
            double key1, int value1, double key2, int value2, double key3, int value3, double key4, int value4)
    {
        return DoubleIntHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableDoubleIntMap ofInitialCapacity(int capacity)
    {
        return this.withInitialCapacity(capacity);
    }

    @Override
    public MutableDoubleIntMap withInitialCapacity(int capacity)
    {
        return new DoubleIntHashMap(capacity);
    }

    @Override
    public MutableDoubleIntMap ofAll(DoubleIntMap map)
    {
        return this.withAll(map);
    }

    @Override
    public MutableDoubleIntMap withAll(DoubleIntMap map)
    {
        if (map.isEmpty())
        {
            return this.empty();
        }
        return new DoubleIntHashMap(map);
    }

    @Override
    public <T> MutableDoubleIntMap from(Iterable<T> iterable, DoubleFunction<? super T> keyFunction, IntFunction<? super T> valueFunction)
    {
        MutableDoubleIntMap map = DoubleIntMaps.mutable.empty();
        Iterate.forEach(iterable, each -> map.put(keyFunction.doubleValueOf(each), valueFunction.intValueOf(each)));
        return map;
    }
}
