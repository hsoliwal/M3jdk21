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

import jdk.m3.collections.eclipse.api.block.function.primitive.DoubleFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ByteFunction;
import jdk.m3.collections.eclipse.api.factory.map.primitive.MutableDoubleByteMapFactory;
import jdk.m3.collections.eclipse.impl.factory.primitive.DoubleByteMaps;
import jdk.m3.collections.eclipse.api.map.primitive.MutableDoubleByteMap;
import jdk.m3.collections.eclipse.api.map.primitive.DoubleByteMap;
import jdk.m3.collections.eclipse.impl.utility.Iterate;

/**
 * MutableDoubleByteMapFactoryImpl is a factory implementation which creates instances of type {@link MutableDoubleByteMap}.
 * This file was automatically generated from template file mutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 6.0.
 */
@SuppressWarnings("missing-explicit-ctor")
public class MutableDoubleByteMapFactoryImpl implements MutableDoubleByteMapFactory
{
    public static final MutableDoubleByteMapFactory INSTANCE = new MutableDoubleByteMapFactoryImpl();

    @Override
    public MutableDoubleByteMap empty()
    {
        return new DoubleByteHashMap(0);
    }

    @Override
    public MutableDoubleByteMap of()
    {
        return this.empty();
    }

    @Override
    public MutableDoubleByteMap with()
    {
        return this.empty();
    }

    @Override
    public MutableDoubleByteMap with(double key, byte value)
    {
        return DoubleByteHashMap.newWithKeysValues(key, value);
    }

    @Override
    public MutableDoubleByteMap of(double key, byte value)
    {
        return this.with(key, value);
    }

    @Override
    public MutableDoubleByteMap of(double key1, byte value1, double key2, byte value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    @Override
    public MutableDoubleByteMap with(double key1, byte value1, double key2, byte value2)
    {
        return DoubleByteHashMap.newWithKeysValues(key1, value1, key2, value2);
    }

    @Override
    public MutableDoubleByteMap of(double key1, byte value1, double key2, byte value2, double key3, byte value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableDoubleByteMap with(double key1, byte value1, double key2, byte value2, double key3, byte value3)
    {
        return DoubleByteHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableDoubleByteMap of(
            double key1, byte value1, double key2, byte value2, double key3, byte value3, double key4, byte value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableDoubleByteMap with(
            double key1, byte value1, double key2, byte value2, double key3, byte value3, double key4, byte value4)
    {
        return DoubleByteHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableDoubleByteMap ofInitialCapacity(int capacity)
    {
        return this.withInitialCapacity(capacity);
    }

    @Override
    public MutableDoubleByteMap withInitialCapacity(int capacity)
    {
        return new DoubleByteHashMap(capacity);
    }

    @Override
    public MutableDoubleByteMap ofAll(DoubleByteMap map)
    {
        return this.withAll(map);
    }

    @Override
    public MutableDoubleByteMap withAll(DoubleByteMap map)
    {
        if (map.isEmpty())
        {
            return this.empty();
        }
        return new DoubleByteHashMap(map);
    }

    @Override
    public <T> MutableDoubleByteMap from(Iterable<T> iterable, DoubleFunction<? super T> keyFunction, ByteFunction<? super T> valueFunction)
    {
        MutableDoubleByteMap map = DoubleByteMaps.mutable.empty();
        Iterate.forEach(iterable, each -> map.put(keyFunction.doubleValueOf(each), valueFunction.byteValueOf(each)));
        return map;
    }
}
