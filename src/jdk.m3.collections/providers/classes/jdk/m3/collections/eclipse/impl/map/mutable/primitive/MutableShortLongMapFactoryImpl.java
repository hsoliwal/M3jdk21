/*
 * Copyright (c) 2026 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.impl.map.mutable.primitive;

import jdk.m3.collections.eclipse.api.block.function.primitive.ShortFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.LongFunction;
import jdk.m3.collections.eclipse.api.factory.map.primitive.MutableShortLongMapFactory;
import jdk.m3.collections.eclipse.impl.factory.primitive.ShortLongMaps;
import jdk.m3.collections.eclipse.api.map.primitive.MutableShortLongMap;
import jdk.m3.collections.eclipse.api.map.primitive.ShortLongMap;
import jdk.m3.collections.eclipse.impl.utility.Iterate;

/**
 * MutableShortLongMapFactoryImpl is a factory implementation which creates instances of type {@link MutableShortLongMap}.
 * This file was automatically generated from template file mutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 6.0.
 */
@SuppressWarnings("missing-explicit-ctor")
public class MutableShortLongMapFactoryImpl implements MutableShortLongMapFactory
{
    public static final MutableShortLongMapFactory INSTANCE = new MutableShortLongMapFactoryImpl();

    @Override
    public MutableShortLongMap empty()
    {
        return new ShortLongHashMap(0);
    }

    @Override
    public MutableShortLongMap of()
    {
        return this.empty();
    }

    @Override
    public MutableShortLongMap with()
    {
        return this.empty();
    }

    @Override
    public MutableShortLongMap with(short key, long value)
    {
        return ShortLongHashMap.newWithKeysValues(key, value);
    }

    @Override
    public MutableShortLongMap of(short key, long value)
    {
        return this.with(key, value);
    }

    @Override
    public MutableShortLongMap of(short key1, long value1, short key2, long value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    @Override
    public MutableShortLongMap with(short key1, long value1, short key2, long value2)
    {
        return ShortLongHashMap.newWithKeysValues(key1, value1, key2, value2);
    }

    @Override
    public MutableShortLongMap of(short key1, long value1, short key2, long value2, short key3, long value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableShortLongMap with(short key1, long value1, short key2, long value2, short key3, long value3)
    {
        return ShortLongHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableShortLongMap of(
            short key1, long value1, short key2, long value2, short key3, long value3, short key4, long value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableShortLongMap with(
            short key1, long value1, short key2, long value2, short key3, long value3, short key4, long value4)
    {
        return ShortLongHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableShortLongMap ofInitialCapacity(int capacity)
    {
        return this.withInitialCapacity(capacity);
    }

    @Override
    public MutableShortLongMap withInitialCapacity(int capacity)
    {
        return new ShortLongHashMap(capacity);
    }

    @Override
    public MutableShortLongMap ofAll(ShortLongMap map)
    {
        return this.withAll(map);
    }

    @Override
    public MutableShortLongMap withAll(ShortLongMap map)
    {
        if (map.isEmpty())
        {
            return this.empty();
        }
        return new ShortLongHashMap(map);
    }

    @Override
    public <T> MutableShortLongMap from(Iterable<T> iterable, ShortFunction<? super T> keyFunction, LongFunction<? super T> valueFunction)
    {
        MutableShortLongMap map = ShortLongMaps.mutable.empty();
        Iterate.forEach(iterable, each -> map.put(keyFunction.shortValueOf(each), valueFunction.longValueOf(each)));
        return map;
    }
}
