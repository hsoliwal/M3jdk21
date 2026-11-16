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

import jdk.m3.collections.eclipse.api.block.function.primitive.CharFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.FloatFunction;
import jdk.m3.collections.eclipse.api.factory.map.primitive.MutableCharFloatMapFactory;
import jdk.m3.collections.eclipse.impl.factory.primitive.CharFloatMaps;
import jdk.m3.collections.eclipse.api.map.primitive.MutableCharFloatMap;
import jdk.m3.collections.eclipse.api.map.primitive.CharFloatMap;
import jdk.m3.collections.eclipse.impl.utility.Iterate;

/**
 * MutableCharFloatMapFactoryImpl is a factory implementation which creates instances of type {@link MutableCharFloatMap}.
 * This file was automatically generated from template file mutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 6.0.
 */
@SuppressWarnings("missing-explicit-ctor")
public class MutableCharFloatMapFactoryImpl implements MutableCharFloatMapFactory
{
    public static final MutableCharFloatMapFactory INSTANCE = new MutableCharFloatMapFactoryImpl();

    @Override
    public MutableCharFloatMap empty()
    {
        return new CharFloatHashMap(0);
    }

    @Override
    public MutableCharFloatMap of()
    {
        return this.empty();
    }

    @Override
    public MutableCharFloatMap with()
    {
        return this.empty();
    }

    @Override
    public MutableCharFloatMap with(char key, float value)
    {
        return CharFloatHashMap.newWithKeysValues(key, value);
    }

    @Override
    public MutableCharFloatMap of(char key, float value)
    {
        return this.with(key, value);
    }

    @Override
    public MutableCharFloatMap of(char key1, float value1, char key2, float value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    @Override
    public MutableCharFloatMap with(char key1, float value1, char key2, float value2)
    {
        return CharFloatHashMap.newWithKeysValues(key1, value1, key2, value2);
    }

    @Override
    public MutableCharFloatMap of(char key1, float value1, char key2, float value2, char key3, float value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableCharFloatMap with(char key1, float value1, char key2, float value2, char key3, float value3)
    {
        return CharFloatHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableCharFloatMap of(
            char key1, float value1, char key2, float value2, char key3, float value3, char key4, float value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableCharFloatMap with(
            char key1, float value1, char key2, float value2, char key3, float value3, char key4, float value4)
    {
        return CharFloatHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableCharFloatMap ofInitialCapacity(int capacity)
    {
        return this.withInitialCapacity(capacity);
    }

    @Override
    public MutableCharFloatMap withInitialCapacity(int capacity)
    {
        return new CharFloatHashMap(capacity);
    }

    @Override
    public MutableCharFloatMap ofAll(CharFloatMap map)
    {
        return this.withAll(map);
    }

    @Override
    public MutableCharFloatMap withAll(CharFloatMap map)
    {
        if (map.isEmpty())
        {
            return this.empty();
        }
        return new CharFloatHashMap(map);
    }

    @Override
    public <T> MutableCharFloatMap from(Iterable<T> iterable, CharFunction<? super T> keyFunction, FloatFunction<? super T> valueFunction)
    {
        MutableCharFloatMap map = CharFloatMaps.mutable.empty();
        Iterate.forEach(iterable, each -> map.put(keyFunction.charValueOf(each), valueFunction.floatValueOf(each)));
        return map;
    }
}
