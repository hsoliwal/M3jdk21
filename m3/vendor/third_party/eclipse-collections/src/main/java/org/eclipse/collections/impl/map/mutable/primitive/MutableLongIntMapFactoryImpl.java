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

import org.eclipse.collections.api.block.function.primitive.LongFunction;
import org.eclipse.collections.api.block.function.primitive.IntFunction;
import org.eclipse.collections.api.factory.map.primitive.MutableLongIntMapFactory;
import org.eclipse.collections.impl.factory.primitive.LongIntMaps;
import org.eclipse.collections.api.map.primitive.MutableLongIntMap;
import org.eclipse.collections.api.map.primitive.LongIntMap;
import org.eclipse.collections.impl.utility.Iterate;

/**
 * MutableLongIntMapFactoryImpl is a factory implementation which creates instances of type {@link MutableLongIntMap}.
 * This file was automatically generated from template file mutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 6.0.
 */
public class MutableLongIntMapFactoryImpl implements MutableLongIntMapFactory
{
    public static final MutableLongIntMapFactory INSTANCE = new MutableLongIntMapFactoryImpl();

    @Override
    public MutableLongIntMap empty()
    {
        return new LongIntHashMap(0);
    }

    @Override
    public MutableLongIntMap of()
    {
        return this.empty();
    }

    @Override
    public MutableLongIntMap with()
    {
        return this.empty();
    }

    @Override
    public MutableLongIntMap with(long key, int value)
    {
        return LongIntHashMap.newWithKeysValues(key, value);
    }

    @Override
    public MutableLongIntMap of(long key, int value)
    {
        return this.with(key, value);
    }

    @Override
    public MutableLongIntMap of(long key1, int value1, long key2, int value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    @Override
    public MutableLongIntMap with(long key1, int value1, long key2, int value2)
    {
        return LongIntHashMap.newWithKeysValues(key1, value1, key2, value2);
    }

    @Override
    public MutableLongIntMap of(long key1, int value1, long key2, int value2, long key3, int value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableLongIntMap with(long key1, int value1, long key2, int value2, long key3, int value3)
    {
        return LongIntHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableLongIntMap of(
            long key1, int value1, long key2, int value2, long key3, int value3, long key4, int value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableLongIntMap with(
            long key1, int value1, long key2, int value2, long key3, int value3, long key4, int value4)
    {
        return LongIntHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableLongIntMap ofInitialCapacity(int capacity)
    {
        return this.withInitialCapacity(capacity);
    }

    @Override
    public MutableLongIntMap withInitialCapacity(int capacity)
    {
        return new LongIntHashMap(capacity);
    }

    @Override
    public MutableLongIntMap ofAll(LongIntMap map)
    {
        return this.withAll(map);
    }

    @Override
    public MutableLongIntMap withAll(LongIntMap map)
    {
        if (map.isEmpty())
        {
            return this.empty();
        }
        return new LongIntHashMap(map);
    }

    @Override
    public <T> MutableLongIntMap from(Iterable<T> iterable, LongFunction<? super T> keyFunction, IntFunction<? super T> valueFunction)
    {
        MutableLongIntMap map = LongIntMaps.mutable.empty();
        Iterate.forEach(iterable, each -> map.put(keyFunction.longValueOf(each), valueFunction.intValueOf(each)));
        return map;
    }
}
