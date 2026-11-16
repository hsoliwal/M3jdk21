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
import jdk.m3.collections.eclipse.api.block.function.primitive.LongFunction;
import jdk.m3.collections.eclipse.api.factory.map.primitive.MutableDoubleLongMapFactory;
import jdk.m3.collections.eclipse.impl.factory.primitive.DoubleLongMaps;
import jdk.m3.collections.eclipse.api.map.primitive.MutableDoubleLongMap;
import jdk.m3.collections.eclipse.api.map.primitive.DoubleLongMap;
import jdk.m3.collections.eclipse.impl.utility.Iterate;

/**
 * MutableDoubleLongMapFactoryImpl is a factory implementation which creates instances of type {@link MutableDoubleLongMap}.
 * This file was automatically generated from template file mutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 6.0.
 */
@SuppressWarnings("missing-explicit-ctor")
public class MutableDoubleLongMapFactoryImpl implements MutableDoubleLongMapFactory
{
    public static final MutableDoubleLongMapFactory INSTANCE = new MutableDoubleLongMapFactoryImpl();

    @Override
    public MutableDoubleLongMap empty()
    {
        return new DoubleLongHashMap(0);
    }

    @Override
    public MutableDoubleLongMap of()
    {
        return this.empty();
    }

    @Override
    public MutableDoubleLongMap with()
    {
        return this.empty();
    }

    @Override
    public MutableDoubleLongMap with(double key, long value)
    {
        return DoubleLongHashMap.newWithKeysValues(key, value);
    }

    @Override
    public MutableDoubleLongMap of(double key, long value)
    {
        return this.with(key, value);
    }

    @Override
    public MutableDoubleLongMap of(double key1, long value1, double key2, long value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    @Override
    public MutableDoubleLongMap with(double key1, long value1, double key2, long value2)
    {
        return DoubleLongHashMap.newWithKeysValues(key1, value1, key2, value2);
    }

    @Override
    public MutableDoubleLongMap of(double key1, long value1, double key2, long value2, double key3, long value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableDoubleLongMap with(double key1, long value1, double key2, long value2, double key3, long value3)
    {
        return DoubleLongHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableDoubleLongMap of(
            double key1, long value1, double key2, long value2, double key3, long value3, double key4, long value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableDoubleLongMap with(
            double key1, long value1, double key2, long value2, double key3, long value3, double key4, long value4)
    {
        return DoubleLongHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableDoubleLongMap ofInitialCapacity(int capacity)
    {
        return this.withInitialCapacity(capacity);
    }

    @Override
    public MutableDoubleLongMap withInitialCapacity(int capacity)
    {
        return new DoubleLongHashMap(capacity);
    }

    @Override
    public MutableDoubleLongMap ofAll(DoubleLongMap map)
    {
        return this.withAll(map);
    }

    @Override
    public MutableDoubleLongMap withAll(DoubleLongMap map)
    {
        if (map.isEmpty())
        {
            return this.empty();
        }
        return new DoubleLongHashMap(map);
    }

    @Override
    public <T> MutableDoubleLongMap from(Iterable<T> iterable, DoubleFunction<? super T> keyFunction, LongFunction<? super T> valueFunction)
    {
        MutableDoubleLongMap map = DoubleLongMaps.mutable.empty();
        Iterate.forEach(iterable, each -> map.put(keyFunction.doubleValueOf(each), valueFunction.longValueOf(each)));
        return map;
    }
}
