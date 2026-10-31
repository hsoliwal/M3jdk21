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
import org.eclipse.collections.api.block.function.primitive.BooleanFunction;
import org.eclipse.collections.api.factory.map.primitive.MutableLongBooleanMapFactory;
import org.eclipse.collections.impl.factory.primitive.LongBooleanMaps;
import org.eclipse.collections.api.map.primitive.MutableLongBooleanMap;
import org.eclipse.collections.api.map.primitive.LongBooleanMap;
import org.eclipse.collections.impl.utility.Iterate;

/**
 * MutableLongBooleanMapFactoryImpl is a factory implementation which creates instances of type {@link MutableLongBooleanMap}.
 * This file was automatically generated from template file mutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 6.0.
 */
public class MutableLongBooleanMapFactoryImpl implements MutableLongBooleanMapFactory
{
    public static final MutableLongBooleanMapFactory INSTANCE = new MutableLongBooleanMapFactoryImpl();

    @Override
    public MutableLongBooleanMap empty()
    {
        return new LongBooleanHashMap(0);
    }

    @Override
    public MutableLongBooleanMap of()
    {
        return this.empty();
    }

    @Override
    public MutableLongBooleanMap with()
    {
        return this.empty();
    }

    @Override
    public MutableLongBooleanMap with(long key, boolean value)
    {
        return LongBooleanHashMap.newWithKeysValues(key, value);
    }

    @Override
    public MutableLongBooleanMap of(long key, boolean value)
    {
        return this.with(key, value);
    }

    @Override
    public MutableLongBooleanMap of(long key1, boolean value1, long key2, boolean value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    @Override
    public MutableLongBooleanMap with(long key1, boolean value1, long key2, boolean value2)
    {
        return LongBooleanHashMap.newWithKeysValues(key1, value1, key2, value2);
    }

    @Override
    public MutableLongBooleanMap of(long key1, boolean value1, long key2, boolean value2, long key3, boolean value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableLongBooleanMap with(long key1, boolean value1, long key2, boolean value2, long key3, boolean value3)
    {
        return LongBooleanHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableLongBooleanMap of(
            long key1, boolean value1, long key2, boolean value2, long key3, boolean value3, long key4, boolean value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableLongBooleanMap with(
            long key1, boolean value1, long key2, boolean value2, long key3, boolean value3, long key4, boolean value4)
    {
        return LongBooleanHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableLongBooleanMap ofInitialCapacity(int capacity)
    {
        return this.withInitialCapacity(capacity);
    }

    @Override
    public MutableLongBooleanMap withInitialCapacity(int capacity)
    {
        return new LongBooleanHashMap(capacity);
    }

    @Override
    public MutableLongBooleanMap ofAll(LongBooleanMap map)
    {
        return this.withAll(map);
    }

    @Override
    public MutableLongBooleanMap withAll(LongBooleanMap map)
    {
        if (map.isEmpty())
        {
            return this.empty();
        }
        return new LongBooleanHashMap(map);
    }

    @Override
    public <T> MutableLongBooleanMap from(Iterable<T> iterable, LongFunction<? super T> keyFunction, BooleanFunction<? super T> valueFunction)
    {
        MutableLongBooleanMap map = LongBooleanMaps.mutable.empty();
        Iterate.forEach(iterable, each -> map.put(keyFunction.longValueOf(each), valueFunction.booleanValueOf(each)));
        return map;
    }
}
