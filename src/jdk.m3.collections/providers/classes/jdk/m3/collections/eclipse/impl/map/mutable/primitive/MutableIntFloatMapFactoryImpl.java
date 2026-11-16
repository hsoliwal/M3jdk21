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

import jdk.m3.collections.eclipse.api.block.function.primitive.IntFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.FloatFunction;
import jdk.m3.collections.eclipse.api.factory.map.primitive.MutableIntFloatMapFactory;
import jdk.m3.collections.eclipse.impl.factory.primitive.IntFloatMaps;
import jdk.m3.collections.eclipse.api.map.primitive.MutableIntFloatMap;
import jdk.m3.collections.eclipse.api.map.primitive.IntFloatMap;
import jdk.m3.collections.eclipse.impl.utility.Iterate;

/**
 * MutableIntFloatMapFactoryImpl is a factory implementation which creates instances of type {@link MutableIntFloatMap}.
 * This file was automatically generated from template file mutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 6.0.
 */
@SuppressWarnings("missing-explicit-ctor")
public class MutableIntFloatMapFactoryImpl implements MutableIntFloatMapFactory
{
    public static final MutableIntFloatMapFactory INSTANCE = new MutableIntFloatMapFactoryImpl();

    @Override
    public MutableIntFloatMap empty()
    {
        return new IntFloatHashMap(0);
    }

    @Override
    public MutableIntFloatMap of()
    {
        return this.empty();
    }

    @Override
    public MutableIntFloatMap with()
    {
        return this.empty();
    }

    @Override
    public MutableIntFloatMap with(int key, float value)
    {
        return IntFloatHashMap.newWithKeysValues(key, value);
    }

    @Override
    public MutableIntFloatMap of(int key, float value)
    {
        return this.with(key, value);
    }

    @Override
    public MutableIntFloatMap of(int key1, float value1, int key2, float value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    @Override
    public MutableIntFloatMap with(int key1, float value1, int key2, float value2)
    {
        return IntFloatHashMap.newWithKeysValues(key1, value1, key2, value2);
    }

    @Override
    public MutableIntFloatMap of(int key1, float value1, int key2, float value2, int key3, float value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableIntFloatMap with(int key1, float value1, int key2, float value2, int key3, float value3)
    {
        return IntFloatHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableIntFloatMap of(
            int key1, float value1, int key2, float value2, int key3, float value3, int key4, float value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableIntFloatMap with(
            int key1, float value1, int key2, float value2, int key3, float value3, int key4, float value4)
    {
        return IntFloatHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableIntFloatMap ofInitialCapacity(int capacity)
    {
        return this.withInitialCapacity(capacity);
    }

    @Override
    public MutableIntFloatMap withInitialCapacity(int capacity)
    {
        return new IntFloatHashMap(capacity);
    }

    @Override
    public MutableIntFloatMap ofAll(IntFloatMap map)
    {
        return this.withAll(map);
    }

    @Override
    public MutableIntFloatMap withAll(IntFloatMap map)
    {
        if (map.isEmpty())
        {
            return this.empty();
        }
        return new IntFloatHashMap(map);
    }

    @Override
    public <T> MutableIntFloatMap from(Iterable<T> iterable, IntFunction<? super T> keyFunction, FloatFunction<? super T> valueFunction)
    {
        MutableIntFloatMap map = IntFloatMaps.mutable.empty();
        Iterate.forEach(iterable, each -> map.put(keyFunction.intValueOf(each), valueFunction.floatValueOf(each)));
        return map;
    }
}
