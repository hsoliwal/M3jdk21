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
import org.eclipse.collections.api.factory.map.primitive.MutableIntIntMapFactory;
import org.eclipse.collections.impl.factory.primitive.IntIntMaps;
import org.eclipse.collections.api.map.primitive.MutableIntIntMap;
import org.eclipse.collections.api.map.primitive.IntIntMap;
import org.eclipse.collections.impl.utility.Iterate;

/**
 * MutableIntIntMapFactoryImpl is a factory implementation which creates instances of type {@link MutableIntIntMap}.
 * This file was automatically generated from template file mutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 6.0.
 */
public class MutableIntIntMapFactoryImpl implements MutableIntIntMapFactory
{
    public static final MutableIntIntMapFactory INSTANCE = new MutableIntIntMapFactoryImpl();

    @Override
    public MutableIntIntMap empty()
    {
        return new IntIntHashMap(0);
    }

    @Override
    public MutableIntIntMap of()
    {
        return this.empty();
    }

    @Override
    public MutableIntIntMap with()
    {
        return this.empty();
    }

    @Override
    public MutableIntIntMap with(int key, int value)
    {
        return IntIntHashMap.newWithKeysValues(key, value);
    }

    @Override
    public MutableIntIntMap of(int key, int value)
    {
        return this.with(key, value);
    }

    @Override
    public MutableIntIntMap of(int key1, int value1, int key2, int value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    @Override
    public MutableIntIntMap with(int key1, int value1, int key2, int value2)
    {
        return IntIntHashMap.newWithKeysValues(key1, value1, key2, value2);
    }

    @Override
    public MutableIntIntMap of(int key1, int value1, int key2, int value2, int key3, int value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableIntIntMap with(int key1, int value1, int key2, int value2, int key3, int value3)
    {
        return IntIntHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableIntIntMap of(
            int key1, int value1, int key2, int value2, int key3, int value3, int key4, int value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableIntIntMap with(
            int key1, int value1, int key2, int value2, int key3, int value3, int key4, int value4)
    {
        return IntIntHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableIntIntMap ofInitialCapacity(int capacity)
    {
        return this.withInitialCapacity(capacity);
    }

    @Override
    public MutableIntIntMap withInitialCapacity(int capacity)
    {
        return new IntIntHashMap(capacity);
    }

    @Override
    public MutableIntIntMap ofAll(IntIntMap map)
    {
        return this.withAll(map);
    }

    @Override
    public MutableIntIntMap withAll(IntIntMap map)
    {
        if (map.isEmpty())
        {
            return this.empty();
        }
        return new IntIntHashMap(map);
    }

    @Override
    public <T> MutableIntIntMap from(Iterable<T> iterable, IntFunction<? super T> keyFunction, IntFunction<? super T> valueFunction)
    {
        MutableIntIntMap map = IntIntMaps.mutable.empty();
        Iterate.forEach(iterable, each -> map.put(keyFunction.intValueOf(each), valueFunction.intValueOf(each)));
        return map;
    }
}
