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
import org.eclipse.collections.api.factory.map.primitive.MutableCharCharMapFactory;
import org.eclipse.collections.impl.factory.primitive.CharCharMaps;
import org.eclipse.collections.api.map.primitive.MutableCharCharMap;
import org.eclipse.collections.api.map.primitive.CharCharMap;
import org.eclipse.collections.impl.utility.Iterate;

/**
 * MutableCharCharMapFactoryImpl is a factory implementation which creates instances of type {@link MutableCharCharMap}.
 * This file was automatically generated from template file mutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 6.0.
 */
public class MutableCharCharMapFactoryImpl implements MutableCharCharMapFactory
{
    public static final MutableCharCharMapFactory INSTANCE = new MutableCharCharMapFactoryImpl();

    @Override
    public MutableCharCharMap empty()
    {
        return new CharCharHashMap(0);
    }

    @Override
    public MutableCharCharMap of()
    {
        return this.empty();
    }

    @Override
    public MutableCharCharMap with()
    {
        return this.empty();
    }

    @Override
    public MutableCharCharMap with(char key, char value)
    {
        return CharCharHashMap.newWithKeysValues(key, value);
    }

    @Override
    public MutableCharCharMap of(char key, char value)
    {
        return this.with(key, value);
    }

    @Override
    public MutableCharCharMap of(char key1, char value1, char key2, char value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    @Override
    public MutableCharCharMap with(char key1, char value1, char key2, char value2)
    {
        return CharCharHashMap.newWithKeysValues(key1, value1, key2, value2);
    }

    @Override
    public MutableCharCharMap of(char key1, char value1, char key2, char value2, char key3, char value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableCharCharMap with(char key1, char value1, char key2, char value2, char key3, char value3)
    {
        return CharCharHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableCharCharMap of(
            char key1, char value1, char key2, char value2, char key3, char value3, char key4, char value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableCharCharMap with(
            char key1, char value1, char key2, char value2, char key3, char value3, char key4, char value4)
    {
        return CharCharHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableCharCharMap ofInitialCapacity(int capacity)
    {
        return this.withInitialCapacity(capacity);
    }

    @Override
    public MutableCharCharMap withInitialCapacity(int capacity)
    {
        return new CharCharHashMap(capacity);
    }

    @Override
    public MutableCharCharMap ofAll(CharCharMap map)
    {
        return this.withAll(map);
    }

    @Override
    public MutableCharCharMap withAll(CharCharMap map)
    {
        if (map.isEmpty())
        {
            return this.empty();
        }
        return new CharCharHashMap(map);
    }

    @Override
    public <T> MutableCharCharMap from(Iterable<T> iterable, CharFunction<? super T> keyFunction, CharFunction<? super T> valueFunction)
    {
        MutableCharCharMap map = CharCharMaps.mutable.empty();
        Iterate.forEach(iterable, each -> map.put(keyFunction.charValueOf(each), valueFunction.charValueOf(each)));
        return map;
    }
}
