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
import org.eclipse.collections.api.block.function.primitive.DoubleFunction;
import org.eclipse.collections.api.factory.map.primitive.MutableLongDoubleMapFactory;
import org.eclipse.collections.impl.factory.primitive.LongDoubleMaps;
import org.eclipse.collections.api.map.primitive.MutableLongDoubleMap;
import org.eclipse.collections.api.map.primitive.LongDoubleMap;
import org.eclipse.collections.impl.utility.Iterate;

/**
 * MutableLongDoubleMapFactoryImpl is a factory implementation which creates instances of type {@link MutableLongDoubleMap}.
 * This file was automatically generated from template file mutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 6.0.
 */
public class MutableLongDoubleMapFactoryImpl implements MutableLongDoubleMapFactory
{
    public static final MutableLongDoubleMapFactory INSTANCE = new MutableLongDoubleMapFactoryImpl();

    @Override
    public MutableLongDoubleMap empty()
    {
        return new LongDoubleHashMap(0);
    }

    @Override
    public MutableLongDoubleMap of()
    {
        return this.empty();
    }

    @Override
    public MutableLongDoubleMap with()
    {
        return this.empty();
    }

    @Override
    public MutableLongDoubleMap with(long key, double value)
    {
        return LongDoubleHashMap.newWithKeysValues(key, value);
    }

    @Override
    public MutableLongDoubleMap of(long key, double value)
    {
        return this.with(key, value);
    }

    @Override
    public MutableLongDoubleMap of(long key1, double value1, long key2, double value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    @Override
    public MutableLongDoubleMap with(long key1, double value1, long key2, double value2)
    {
        return LongDoubleHashMap.newWithKeysValues(key1, value1, key2, value2);
    }

    @Override
    public MutableLongDoubleMap of(long key1, double value1, long key2, double value2, long key3, double value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableLongDoubleMap with(long key1, double value1, long key2, double value2, long key3, double value3)
    {
        return LongDoubleHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableLongDoubleMap of(
            long key1, double value1, long key2, double value2, long key3, double value3, long key4, double value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableLongDoubleMap with(
            long key1, double value1, long key2, double value2, long key3, double value3, long key4, double value4)
    {
        return LongDoubleHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableLongDoubleMap ofInitialCapacity(int capacity)
    {
        return this.withInitialCapacity(capacity);
    }

    @Override
    public MutableLongDoubleMap withInitialCapacity(int capacity)
    {
        return new LongDoubleHashMap(capacity);
    }

    @Override
    public MutableLongDoubleMap ofAll(LongDoubleMap map)
    {
        return this.withAll(map);
    }

    @Override
    public MutableLongDoubleMap withAll(LongDoubleMap map)
    {
        if (map.isEmpty())
        {
            return this.empty();
        }
        return new LongDoubleHashMap(map);
    }

    @Override
    public <T> MutableLongDoubleMap from(Iterable<T> iterable, LongFunction<? super T> keyFunction, DoubleFunction<? super T> valueFunction)
    {
        MutableLongDoubleMap map = LongDoubleMaps.mutable.empty();
        Iterate.forEach(iterable, each -> map.put(keyFunction.longValueOf(each), valueFunction.doubleValueOf(each)));
        return map;
    }
}
