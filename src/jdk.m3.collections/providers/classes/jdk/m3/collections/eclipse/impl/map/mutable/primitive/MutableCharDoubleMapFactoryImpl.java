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
import jdk.m3.collections.eclipse.api.block.function.primitive.DoubleFunction;
import jdk.m3.collections.eclipse.api.factory.map.primitive.MutableCharDoubleMapFactory;
import jdk.m3.collections.eclipse.impl.factory.primitive.CharDoubleMaps;
import jdk.m3.collections.eclipse.api.map.primitive.MutableCharDoubleMap;
import jdk.m3.collections.eclipse.api.map.primitive.CharDoubleMap;
import jdk.m3.collections.eclipse.impl.utility.Iterate;

/**
 * MutableCharDoubleMapFactoryImpl is a factory implementation which creates instances of type {@link MutableCharDoubleMap}.
 * This file was automatically generated from template file mutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 6.0.
 */
@SuppressWarnings("missing-explicit-ctor")
public class MutableCharDoubleMapFactoryImpl implements MutableCharDoubleMapFactory
{
    public static final MutableCharDoubleMapFactory INSTANCE = new MutableCharDoubleMapFactoryImpl();

    @Override
    public MutableCharDoubleMap empty()
    {
        return new CharDoubleHashMap(0);
    }

    @Override
    public MutableCharDoubleMap of()
    {
        return this.empty();
    }

    @Override
    public MutableCharDoubleMap with()
    {
        return this.empty();
    }

    @Override
    public MutableCharDoubleMap with(char key, double value)
    {
        return CharDoubleHashMap.newWithKeysValues(key, value);
    }

    @Override
    public MutableCharDoubleMap of(char key, double value)
    {
        return this.with(key, value);
    }

    @Override
    public MutableCharDoubleMap of(char key1, double value1, char key2, double value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    @Override
    public MutableCharDoubleMap with(char key1, double value1, char key2, double value2)
    {
        return CharDoubleHashMap.newWithKeysValues(key1, value1, key2, value2);
    }

    @Override
    public MutableCharDoubleMap of(char key1, double value1, char key2, double value2, char key3, double value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableCharDoubleMap with(char key1, double value1, char key2, double value2, char key3, double value3)
    {
        return CharDoubleHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableCharDoubleMap of(
            char key1, double value1, char key2, double value2, char key3, double value3, char key4, double value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableCharDoubleMap with(
            char key1, double value1, char key2, double value2, char key3, double value3, char key4, double value4)
    {
        return CharDoubleHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableCharDoubleMap ofInitialCapacity(int capacity)
    {
        return this.withInitialCapacity(capacity);
    }

    @Override
    public MutableCharDoubleMap withInitialCapacity(int capacity)
    {
        return new CharDoubleHashMap(capacity);
    }

    @Override
    public MutableCharDoubleMap ofAll(CharDoubleMap map)
    {
        return this.withAll(map);
    }

    @Override
    public MutableCharDoubleMap withAll(CharDoubleMap map)
    {
        if (map.isEmpty())
        {
            return this.empty();
        }
        return new CharDoubleHashMap(map);
    }

    @Override
    public <T> MutableCharDoubleMap from(Iterable<T> iterable, CharFunction<? super T> keyFunction, DoubleFunction<? super T> valueFunction)
    {
        MutableCharDoubleMap map = CharDoubleMaps.mutable.empty();
        Iterate.forEach(iterable, each -> map.put(keyFunction.charValueOf(each), valueFunction.doubleValueOf(each)));
        return map;
    }
}
