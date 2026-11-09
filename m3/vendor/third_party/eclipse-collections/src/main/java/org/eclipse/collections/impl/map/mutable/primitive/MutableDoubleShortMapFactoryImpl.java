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
import org.eclipse.collections.api.block.function.primitive.ShortFunction;
import org.eclipse.collections.api.factory.map.primitive.MutableDoubleShortMapFactory;
import org.eclipse.collections.impl.factory.primitive.DoubleShortMaps;
import org.eclipse.collections.api.map.primitive.MutableDoubleShortMap;
import org.eclipse.collections.api.map.primitive.DoubleShortMap;
import org.eclipse.collections.impl.utility.Iterate;

/**
 * MutableDoubleShortMapFactoryImpl is a factory implementation which creates instances of type {@link MutableDoubleShortMap}.
 * This file was automatically generated from template file mutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 6.0.
 */
public class MutableDoubleShortMapFactoryImpl implements MutableDoubleShortMapFactory
{
    public static final MutableDoubleShortMapFactory INSTANCE = new MutableDoubleShortMapFactoryImpl();

    @Override
    public MutableDoubleShortMap empty()
    {
        return new DoubleShortHashMap(0);
    }

    @Override
    public MutableDoubleShortMap of()
    {
        return this.empty();
    }

    @Override
    public MutableDoubleShortMap with()
    {
        return this.empty();
    }

    @Override
    public MutableDoubleShortMap with(double key, short value)
    {
        return DoubleShortHashMap.newWithKeysValues(key, value);
    }

    @Override
    public MutableDoubleShortMap of(double key, short value)
    {
        return this.with(key, value);
    }

    @Override
    public MutableDoubleShortMap of(double key1, short value1, double key2, short value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    @Override
    public MutableDoubleShortMap with(double key1, short value1, double key2, short value2)
    {
        return DoubleShortHashMap.newWithKeysValues(key1, value1, key2, value2);
    }

    @Override
    public MutableDoubleShortMap of(double key1, short value1, double key2, short value2, double key3, short value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableDoubleShortMap with(double key1, short value1, double key2, short value2, double key3, short value3)
    {
        return DoubleShortHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableDoubleShortMap of(
            double key1, short value1, double key2, short value2, double key3, short value3, double key4, short value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableDoubleShortMap with(
            double key1, short value1, double key2, short value2, double key3, short value3, double key4, short value4)
    {
        return DoubleShortHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableDoubleShortMap ofInitialCapacity(int capacity)
    {
        return this.withInitialCapacity(capacity);
    }

    @Override
    public MutableDoubleShortMap withInitialCapacity(int capacity)
    {
        return new DoubleShortHashMap(capacity);
    }

    @Override
    public MutableDoubleShortMap ofAll(DoubleShortMap map)
    {
        return this.withAll(map);
    }

    @Override
    public MutableDoubleShortMap withAll(DoubleShortMap map)
    {
        if (map.isEmpty())
        {
            return this.empty();
        }
        return new DoubleShortHashMap(map);
    }

    @Override
    public <T> MutableDoubleShortMap from(Iterable<T> iterable, DoubleFunction<? super T> keyFunction, ShortFunction<? super T> valueFunction)
    {
        MutableDoubleShortMap map = DoubleShortMaps.mutable.empty();
        Iterate.forEach(iterable, each -> map.put(keyFunction.doubleValueOf(each), valueFunction.shortValueOf(each)));
        return map;
    }
}
