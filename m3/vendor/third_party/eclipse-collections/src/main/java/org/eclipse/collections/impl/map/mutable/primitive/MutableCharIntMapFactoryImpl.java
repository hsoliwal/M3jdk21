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

import org.eclipse.collections.api.block.function.primitive.CharFunction;
import org.eclipse.collections.api.block.function.primitive.IntFunction;
import org.eclipse.collections.api.factory.map.primitive.MutableCharIntMapFactory;
import org.eclipse.collections.impl.factory.primitive.CharIntMaps;
import org.eclipse.collections.api.map.primitive.MutableCharIntMap;
import org.eclipse.collections.api.map.primitive.CharIntMap;
import org.eclipse.collections.impl.utility.Iterate;

/**
 * MutableCharIntMapFactoryImpl is a factory implementation which creates instances of type {@link MutableCharIntMap}.
 * This file was automatically generated from template file mutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 6.0.
 */
public class MutableCharIntMapFactoryImpl implements MutableCharIntMapFactory
{
    public static final MutableCharIntMapFactory INSTANCE = new MutableCharIntMapFactoryImpl();

    @Override
    public MutableCharIntMap empty()
    {
        return new CharIntHashMap(0);
    }

    @Override
    public MutableCharIntMap of()
    {
        return this.empty();
    }

    @Override
    public MutableCharIntMap with()
    {
        return this.empty();
    }

    @Override
    public MutableCharIntMap with(char key, int value)
    {
        return CharIntHashMap.newWithKeysValues(key, value);
    }

    @Override
    public MutableCharIntMap of(char key, int value)
    {
        return this.with(key, value);
    }

    @Override
    public MutableCharIntMap of(char key1, int value1, char key2, int value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    @Override
    public MutableCharIntMap with(char key1, int value1, char key2, int value2)
    {
        return CharIntHashMap.newWithKeysValues(key1, value1, key2, value2);
    }

    @Override
    public MutableCharIntMap of(char key1, int value1, char key2, int value2, char key3, int value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableCharIntMap with(char key1, int value1, char key2, int value2, char key3, int value3)
    {
        return CharIntHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableCharIntMap of(
            char key1, int value1, char key2, int value2, char key3, int value3, char key4, int value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableCharIntMap with(
            char key1, int value1, char key2, int value2, char key3, int value3, char key4, int value4)
    {
        return CharIntHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableCharIntMap ofInitialCapacity(int capacity)
    {
        return this.withInitialCapacity(capacity);
    }

    @Override
    public MutableCharIntMap withInitialCapacity(int capacity)
    {
        return new CharIntHashMap(capacity);
    }

    @Override
    public MutableCharIntMap ofAll(CharIntMap map)
    {
        return this.withAll(map);
    }

    @Override
    public MutableCharIntMap withAll(CharIntMap map)
    {
        if (map.isEmpty())
        {
            return this.empty();
        }
        return new CharIntHashMap(map);
    }

    @Override
    public <T> MutableCharIntMap from(Iterable<T> iterable, CharFunction<? super T> keyFunction, IntFunction<? super T> valueFunction)
    {
        MutableCharIntMap map = CharIntMaps.mutable.empty();
        Iterate.forEach(iterable, each -> map.put(keyFunction.charValueOf(each), valueFunction.intValueOf(each)));
        return map;
    }
}
