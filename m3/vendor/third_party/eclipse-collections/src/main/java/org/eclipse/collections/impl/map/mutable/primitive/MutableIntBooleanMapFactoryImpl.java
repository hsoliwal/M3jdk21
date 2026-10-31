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
import org.eclipse.collections.api.block.function.primitive.BooleanFunction;
import org.eclipse.collections.api.factory.map.primitive.MutableIntBooleanMapFactory;
import org.eclipse.collections.impl.factory.primitive.IntBooleanMaps;
import org.eclipse.collections.api.map.primitive.MutableIntBooleanMap;
import org.eclipse.collections.api.map.primitive.IntBooleanMap;
import org.eclipse.collections.impl.utility.Iterate;

/**
 * MutableIntBooleanMapFactoryImpl is a factory implementation which creates instances of type {@link MutableIntBooleanMap}.
 * This file was automatically generated from template file mutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 6.0.
 */
public class MutableIntBooleanMapFactoryImpl implements MutableIntBooleanMapFactory
{
    public static final MutableIntBooleanMapFactory INSTANCE = new MutableIntBooleanMapFactoryImpl();

    @Override
    public MutableIntBooleanMap empty()
    {
        return new IntBooleanHashMap(0);
    }

    @Override
    public MutableIntBooleanMap of()
    {
        return this.empty();
    }

    @Override
    public MutableIntBooleanMap with()
    {
        return this.empty();
    }

    @Override
    public MutableIntBooleanMap with(int key, boolean value)
    {
        return IntBooleanHashMap.newWithKeysValues(key, value);
    }

    @Override
    public MutableIntBooleanMap of(int key, boolean value)
    {
        return this.with(key, value);
    }

    @Override
    public MutableIntBooleanMap of(int key1, boolean value1, int key2, boolean value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    @Override
    public MutableIntBooleanMap with(int key1, boolean value1, int key2, boolean value2)
    {
        return IntBooleanHashMap.newWithKeysValues(key1, value1, key2, value2);
    }

    @Override
    public MutableIntBooleanMap of(int key1, boolean value1, int key2, boolean value2, int key3, boolean value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableIntBooleanMap with(int key1, boolean value1, int key2, boolean value2, int key3, boolean value3)
    {
        return IntBooleanHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableIntBooleanMap of(
            int key1, boolean value1, int key2, boolean value2, int key3, boolean value3, int key4, boolean value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableIntBooleanMap with(
            int key1, boolean value1, int key2, boolean value2, int key3, boolean value3, int key4, boolean value4)
    {
        return IntBooleanHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableIntBooleanMap ofInitialCapacity(int capacity)
    {
        return this.withInitialCapacity(capacity);
    }

    @Override
    public MutableIntBooleanMap withInitialCapacity(int capacity)
    {
        return new IntBooleanHashMap(capacity);
    }

    @Override
    public MutableIntBooleanMap ofAll(IntBooleanMap map)
    {
        return this.withAll(map);
    }

    @Override
    public MutableIntBooleanMap withAll(IntBooleanMap map)
    {
        if (map.isEmpty())
        {
            return this.empty();
        }
        return new IntBooleanHashMap(map);
    }

    @Override
    public <T> MutableIntBooleanMap from(Iterable<T> iterable, IntFunction<? super T> keyFunction, BooleanFunction<? super T> valueFunction)
    {
        MutableIntBooleanMap map = IntBooleanMaps.mutable.empty();
        Iterate.forEach(iterable, each -> map.put(keyFunction.intValueOf(each), valueFunction.booleanValueOf(each)));
        return map;
    }
}
