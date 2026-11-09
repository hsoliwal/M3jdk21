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
import org.eclipse.collections.api.block.function.primitive.CharFunction;
import org.eclipse.collections.api.factory.map.primitive.MutableDoubleCharMapFactory;
import org.eclipse.collections.impl.factory.primitive.DoubleCharMaps;
import org.eclipse.collections.api.map.primitive.MutableDoubleCharMap;
import org.eclipse.collections.api.map.primitive.DoubleCharMap;
import org.eclipse.collections.impl.utility.Iterate;

/**
 * MutableDoubleCharMapFactoryImpl is a factory implementation which creates instances of type {@link MutableDoubleCharMap}.
 * This file was automatically generated from template file mutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 6.0.
 */
public class MutableDoubleCharMapFactoryImpl implements MutableDoubleCharMapFactory
{
    public static final MutableDoubleCharMapFactory INSTANCE = new MutableDoubleCharMapFactoryImpl();

    @Override
    public MutableDoubleCharMap empty()
    {
        return new DoubleCharHashMap(0);
    }

    @Override
    public MutableDoubleCharMap of()
    {
        return this.empty();
    }

    @Override
    public MutableDoubleCharMap with()
    {
        return this.empty();
    }

    @Override
    public MutableDoubleCharMap with(double key, char value)
    {
        return DoubleCharHashMap.newWithKeysValues(key, value);
    }

    @Override
    public MutableDoubleCharMap of(double key, char value)
    {
        return this.with(key, value);
    }

    @Override
    public MutableDoubleCharMap of(double key1, char value1, double key2, char value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    @Override
    public MutableDoubleCharMap with(double key1, char value1, double key2, char value2)
    {
        return DoubleCharHashMap.newWithKeysValues(key1, value1, key2, value2);
    }

    @Override
    public MutableDoubleCharMap of(double key1, char value1, double key2, char value2, double key3, char value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableDoubleCharMap with(double key1, char value1, double key2, char value2, double key3, char value3)
    {
        return DoubleCharHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableDoubleCharMap of(
            double key1, char value1, double key2, char value2, double key3, char value3, double key4, char value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableDoubleCharMap with(
            double key1, char value1, double key2, char value2, double key3, char value3, double key4, char value4)
    {
        return DoubleCharHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableDoubleCharMap ofInitialCapacity(int capacity)
    {
        return this.withInitialCapacity(capacity);
    }

    @Override
    public MutableDoubleCharMap withInitialCapacity(int capacity)
    {
        return new DoubleCharHashMap(capacity);
    }

    @Override
    public MutableDoubleCharMap ofAll(DoubleCharMap map)
    {
        return this.withAll(map);
    }

    @Override
    public MutableDoubleCharMap withAll(DoubleCharMap map)
    {
        if (map.isEmpty())
        {
            return this.empty();
        }
        return new DoubleCharHashMap(map);
    }

    @Override
    public <T> MutableDoubleCharMap from(Iterable<T> iterable, DoubleFunction<? super T> keyFunction, CharFunction<? super T> valueFunction)
    {
        MutableDoubleCharMap map = DoubleCharMaps.mutable.empty();
        Iterate.forEach(iterable, each -> map.put(keyFunction.doubleValueOf(each), valueFunction.charValueOf(each)));
        return map;
    }
}
