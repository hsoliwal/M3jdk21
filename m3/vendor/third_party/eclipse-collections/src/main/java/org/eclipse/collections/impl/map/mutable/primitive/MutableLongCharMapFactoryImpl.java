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
import org.eclipse.collections.api.block.function.primitive.CharFunction;
import org.eclipse.collections.api.factory.map.primitive.MutableLongCharMapFactory;
import org.eclipse.collections.impl.factory.primitive.LongCharMaps;
import org.eclipse.collections.api.map.primitive.MutableLongCharMap;
import org.eclipse.collections.api.map.primitive.LongCharMap;
import org.eclipse.collections.impl.utility.Iterate;

/**
 * MutableLongCharMapFactoryImpl is a factory implementation which creates instances of type {@link MutableLongCharMap}.
 * This file was automatically generated from template file mutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 6.0.
 */
public class MutableLongCharMapFactoryImpl implements MutableLongCharMapFactory
{
    public static final MutableLongCharMapFactory INSTANCE = new MutableLongCharMapFactoryImpl();

    @Override
    public MutableLongCharMap empty()
    {
        return new LongCharHashMap(0);
    }

    @Override
    public MutableLongCharMap of()
    {
        return this.empty();
    }

    @Override
    public MutableLongCharMap with()
    {
        return this.empty();
    }

    @Override
    public MutableLongCharMap with(long key, char value)
    {
        return LongCharHashMap.newWithKeysValues(key, value);
    }

    @Override
    public MutableLongCharMap of(long key, char value)
    {
        return this.with(key, value);
    }

    @Override
    public MutableLongCharMap of(long key1, char value1, long key2, char value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    @Override
    public MutableLongCharMap with(long key1, char value1, long key2, char value2)
    {
        return LongCharHashMap.newWithKeysValues(key1, value1, key2, value2);
    }

    @Override
    public MutableLongCharMap of(long key1, char value1, long key2, char value2, long key3, char value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableLongCharMap with(long key1, char value1, long key2, char value2, long key3, char value3)
    {
        return LongCharHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableLongCharMap of(
            long key1, char value1, long key2, char value2, long key3, char value3, long key4, char value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableLongCharMap with(
            long key1, char value1, long key2, char value2, long key3, char value3, long key4, char value4)
    {
        return LongCharHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableLongCharMap ofInitialCapacity(int capacity)
    {
        return this.withInitialCapacity(capacity);
    }

    @Override
    public MutableLongCharMap withInitialCapacity(int capacity)
    {
        return new LongCharHashMap(capacity);
    }

    @Override
    public MutableLongCharMap ofAll(LongCharMap map)
    {
        return this.withAll(map);
    }

    @Override
    public MutableLongCharMap withAll(LongCharMap map)
    {
        if (map.isEmpty())
        {
            return this.empty();
        }
        return new LongCharHashMap(map);
    }

    @Override
    public <T> MutableLongCharMap from(Iterable<T> iterable, LongFunction<? super T> keyFunction, CharFunction<? super T> valueFunction)
    {
        MutableLongCharMap map = LongCharMaps.mutable.empty();
        Iterate.forEach(iterable, each -> map.put(keyFunction.longValueOf(each), valueFunction.charValueOf(each)));
        return map;
    }
}
