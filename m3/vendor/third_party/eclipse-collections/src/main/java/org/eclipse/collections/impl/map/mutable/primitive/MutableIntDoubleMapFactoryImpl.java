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

import org.eclipse.collections.api.block.function.primitive.IntFunction;
import org.eclipse.collections.api.block.function.primitive.DoubleFunction;
import org.eclipse.collections.api.factory.map.primitive.MutableIntDoubleMapFactory;
import org.eclipse.collections.impl.factory.primitive.IntDoubleMaps;
import org.eclipse.collections.api.map.primitive.MutableIntDoubleMap;
import org.eclipse.collections.api.map.primitive.IntDoubleMap;
import org.eclipse.collections.impl.utility.Iterate;

/**
 * MutableIntDoubleMapFactoryImpl is a factory implementation which creates instances of type {@link MutableIntDoubleMap}.
 * This file was automatically generated from template file mutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 6.0.
 */
public class MutableIntDoubleMapFactoryImpl implements MutableIntDoubleMapFactory
{
    public static final MutableIntDoubleMapFactory INSTANCE = new MutableIntDoubleMapFactoryImpl();

    @Override
    public MutableIntDoubleMap empty()
    {
        return new IntDoubleHashMap(0);
    }

    @Override
    public MutableIntDoubleMap of()
    {
        return this.empty();
    }

    @Override
    public MutableIntDoubleMap with()
    {
        return this.empty();
    }

    @Override
    public MutableIntDoubleMap with(int key, double value)
    {
        return IntDoubleHashMap.newWithKeysValues(key, value);
    }

    @Override
    public MutableIntDoubleMap of(int key, double value)
    {
        return this.with(key, value);
    }

    @Override
    public MutableIntDoubleMap of(int key1, double value1, int key2, double value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    @Override
    public MutableIntDoubleMap with(int key1, double value1, int key2, double value2)
    {
        return IntDoubleHashMap.newWithKeysValues(key1, value1, key2, value2);
    }

    @Override
    public MutableIntDoubleMap of(int key1, double value1, int key2, double value2, int key3, double value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableIntDoubleMap with(int key1, double value1, int key2, double value2, int key3, double value3)
    {
        return IntDoubleHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableIntDoubleMap of(
            int key1, double value1, int key2, double value2, int key3, double value3, int key4, double value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableIntDoubleMap with(
            int key1, double value1, int key2, double value2, int key3, double value3, int key4, double value4)
    {
        return IntDoubleHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableIntDoubleMap ofInitialCapacity(int capacity)
    {
        return this.withInitialCapacity(capacity);
    }

    @Override
    public MutableIntDoubleMap withInitialCapacity(int capacity)
    {
        return new IntDoubleHashMap(capacity);
    }

    @Override
    public MutableIntDoubleMap ofAll(IntDoubleMap map)
    {
        return this.withAll(map);
    }

    @Override
    public MutableIntDoubleMap withAll(IntDoubleMap map)
    {
        if (map.isEmpty())
        {
            return this.empty();
        }
        return new IntDoubleHashMap(map);
    }

    @Override
    public <T> MutableIntDoubleMap from(Iterable<T> iterable, IntFunction<? super T> keyFunction, DoubleFunction<? super T> valueFunction)
    {
        MutableIntDoubleMap map = IntDoubleMaps.mutable.empty();
        Iterate.forEach(iterable, each -> map.put(keyFunction.intValueOf(each), valueFunction.doubleValueOf(each)));
        return map;
    }
}
