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
import org.eclipse.collections.api.block.function.primitive.ShortFunction;
import org.eclipse.collections.api.factory.map.primitive.MutableCharShortMapFactory;
import org.eclipse.collections.impl.factory.primitive.CharShortMaps;
import org.eclipse.collections.api.map.primitive.MutableCharShortMap;
import org.eclipse.collections.api.map.primitive.CharShortMap;
import org.eclipse.collections.impl.utility.Iterate;

/**
 * MutableCharShortMapFactoryImpl is a factory implementation which creates instances of type {@link MutableCharShortMap}.
 * This file was automatically generated from template file mutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 6.0.
 */
public class MutableCharShortMapFactoryImpl implements MutableCharShortMapFactory
{
    public static final MutableCharShortMapFactory INSTANCE = new MutableCharShortMapFactoryImpl();

    @Override
    public MutableCharShortMap empty()
    {
        return new CharShortHashMap(0);
    }

    @Override
    public MutableCharShortMap of()
    {
        return this.empty();
    }

    @Override
    public MutableCharShortMap with()
    {
        return this.empty();
    }

    @Override
    public MutableCharShortMap with(char key, short value)
    {
        return CharShortHashMap.newWithKeysValues(key, value);
    }

    @Override
    public MutableCharShortMap of(char key, short value)
    {
        return this.with(key, value);
    }

    @Override
    public MutableCharShortMap of(char key1, short value1, char key2, short value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    @Override
    public MutableCharShortMap with(char key1, short value1, char key2, short value2)
    {
        return CharShortHashMap.newWithKeysValues(key1, value1, key2, value2);
    }

    @Override
    public MutableCharShortMap of(char key1, short value1, char key2, short value2, char key3, short value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableCharShortMap with(char key1, short value1, char key2, short value2, char key3, short value3)
    {
        return CharShortHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableCharShortMap of(
            char key1, short value1, char key2, short value2, char key3, short value3, char key4, short value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableCharShortMap with(
            char key1, short value1, char key2, short value2, char key3, short value3, char key4, short value4)
    {
        return CharShortHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableCharShortMap ofInitialCapacity(int capacity)
    {
        return this.withInitialCapacity(capacity);
    }

    @Override
    public MutableCharShortMap withInitialCapacity(int capacity)
    {
        return new CharShortHashMap(capacity);
    }

    @Override
    public MutableCharShortMap ofAll(CharShortMap map)
    {
        return this.withAll(map);
    }

    @Override
    public MutableCharShortMap withAll(CharShortMap map)
    {
        if (map.isEmpty())
        {
            return this.empty();
        }
        return new CharShortHashMap(map);
    }

    @Override
    public <T> MutableCharShortMap from(Iterable<T> iterable, CharFunction<? super T> keyFunction, ShortFunction<? super T> valueFunction)
    {
        MutableCharShortMap map = CharShortMaps.mutable.empty();
        Iterate.forEach(iterable, each -> map.put(keyFunction.charValueOf(each), valueFunction.shortValueOf(each)));
        return map;
    }
}
