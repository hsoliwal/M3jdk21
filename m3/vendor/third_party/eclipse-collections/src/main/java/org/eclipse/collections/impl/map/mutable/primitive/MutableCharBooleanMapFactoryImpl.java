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
import org.eclipse.collections.api.block.function.primitive.BooleanFunction;
import org.eclipse.collections.api.factory.map.primitive.MutableCharBooleanMapFactory;
import org.eclipse.collections.impl.factory.primitive.CharBooleanMaps;
import org.eclipse.collections.api.map.primitive.MutableCharBooleanMap;
import org.eclipse.collections.api.map.primitive.CharBooleanMap;
import org.eclipse.collections.impl.utility.Iterate;

/**
 * MutableCharBooleanMapFactoryImpl is a factory implementation which creates instances of type {@link MutableCharBooleanMap}.
 * This file was automatically generated from template file mutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 6.0.
 */
public class MutableCharBooleanMapFactoryImpl implements MutableCharBooleanMapFactory
{
    public static final MutableCharBooleanMapFactory INSTANCE = new MutableCharBooleanMapFactoryImpl();

    @Override
    public MutableCharBooleanMap empty()
    {
        return new CharBooleanHashMap(0);
    }

    @Override
    public MutableCharBooleanMap of()
    {
        return this.empty();
    }

    @Override
    public MutableCharBooleanMap with()
    {
        return this.empty();
    }

    @Override
    public MutableCharBooleanMap with(char key, boolean value)
    {
        return CharBooleanHashMap.newWithKeysValues(key, value);
    }

    @Override
    public MutableCharBooleanMap of(char key, boolean value)
    {
        return this.with(key, value);
    }

    @Override
    public MutableCharBooleanMap of(char key1, boolean value1, char key2, boolean value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    @Override
    public MutableCharBooleanMap with(char key1, boolean value1, char key2, boolean value2)
    {
        return CharBooleanHashMap.newWithKeysValues(key1, value1, key2, value2);
    }

    @Override
    public MutableCharBooleanMap of(char key1, boolean value1, char key2, boolean value2, char key3, boolean value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableCharBooleanMap with(char key1, boolean value1, char key2, boolean value2, char key3, boolean value3)
    {
        return CharBooleanHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableCharBooleanMap of(
            char key1, boolean value1, char key2, boolean value2, char key3, boolean value3, char key4, boolean value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableCharBooleanMap with(
            char key1, boolean value1, char key2, boolean value2, char key3, boolean value3, char key4, boolean value4)
    {
        return CharBooleanHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableCharBooleanMap ofInitialCapacity(int capacity)
    {
        return this.withInitialCapacity(capacity);
    }

    @Override
    public MutableCharBooleanMap withInitialCapacity(int capacity)
    {
        return new CharBooleanHashMap(capacity);
    }

    @Override
    public MutableCharBooleanMap ofAll(CharBooleanMap map)
    {
        return this.withAll(map);
    }

    @Override
    public MutableCharBooleanMap withAll(CharBooleanMap map)
    {
        if (map.isEmpty())
        {
            return this.empty();
        }
        return new CharBooleanHashMap(map);
    }

    @Override
    public <T> MutableCharBooleanMap from(Iterable<T> iterable, CharFunction<? super T> keyFunction, BooleanFunction<? super T> valueFunction)
    {
        MutableCharBooleanMap map = CharBooleanMaps.mutable.empty();
        Iterate.forEach(iterable, each -> map.put(keyFunction.charValueOf(each), valueFunction.booleanValueOf(each)));
        return map;
    }
}
