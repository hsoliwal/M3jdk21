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
import org.eclipse.collections.api.factory.map.primitive.MutableDoubleDoubleMapFactory;
import org.eclipse.collections.impl.factory.primitive.DoubleDoubleMaps;
import org.eclipse.collections.api.map.primitive.MutableDoubleDoubleMap;
import org.eclipse.collections.api.map.primitive.DoubleDoubleMap;
import org.eclipse.collections.impl.utility.Iterate;

/**
 * MutableDoubleDoubleMapFactoryImpl is a factory implementation which creates instances of type {@link MutableDoubleDoubleMap}.
 * This file was automatically generated from template file mutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 6.0.
 */
public class MutableDoubleDoubleMapFactoryImpl implements MutableDoubleDoubleMapFactory
{
    public static final MutableDoubleDoubleMapFactory INSTANCE = new MutableDoubleDoubleMapFactoryImpl();

    @Override
    public MutableDoubleDoubleMap empty()
    {
        return new DoubleDoubleHashMap(0);
    }

    @Override
    public MutableDoubleDoubleMap of()
    {
        return this.empty();
    }

    @Override
    public MutableDoubleDoubleMap with()
    {
        return this.empty();
    }

    @Override
    public MutableDoubleDoubleMap with(double key, double value)
    {
        return DoubleDoubleHashMap.newWithKeysValues(key, value);
    }

    @Override
    public MutableDoubleDoubleMap of(double key, double value)
    {
        return this.with(key, value);
    }

    @Override
    public MutableDoubleDoubleMap of(double key1, double value1, double key2, double value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    @Override
    public MutableDoubleDoubleMap with(double key1, double value1, double key2, double value2)
    {
        return DoubleDoubleHashMap.newWithKeysValues(key1, value1, key2, value2);
    }

    @Override
    public MutableDoubleDoubleMap of(double key1, double value1, double key2, double value2, double key3, double value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableDoubleDoubleMap with(double key1, double value1, double key2, double value2, double key3, double value3)
    {
        return DoubleDoubleHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableDoubleDoubleMap of(
            double key1, double value1, double key2, double value2, double key3, double value3, double key4, double value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableDoubleDoubleMap with(
            double key1, double value1, double key2, double value2, double key3, double value3, double key4, double value4)
    {
        return DoubleDoubleHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableDoubleDoubleMap ofInitialCapacity(int capacity)
    {
        return this.withInitialCapacity(capacity);
    }

    @Override
    public MutableDoubleDoubleMap withInitialCapacity(int capacity)
    {
        return new DoubleDoubleHashMap(capacity);
    }

    @Override
    public MutableDoubleDoubleMap ofAll(DoubleDoubleMap map)
    {
        return this.withAll(map);
    }

    @Override
    public MutableDoubleDoubleMap withAll(DoubleDoubleMap map)
    {
        if (map.isEmpty())
        {
            return this.empty();
        }
        return new DoubleDoubleHashMap(map);
    }

    @Override
    public <T> MutableDoubleDoubleMap from(Iterable<T> iterable, DoubleFunction<? super T> keyFunction, DoubleFunction<? super T> valueFunction)
    {
        MutableDoubleDoubleMap map = DoubleDoubleMaps.mutable.empty();
        Iterate.forEach(iterable, each -> map.put(keyFunction.doubleValueOf(each), valueFunction.doubleValueOf(each)));
        return map;
    }
}
