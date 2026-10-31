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

import org.eclipse.collections.api.block.function.primitive.ShortFunction;
import org.eclipse.collections.api.block.function.primitive.ByteFunction;
import org.eclipse.collections.api.factory.map.primitive.MutableShortByteMapFactory;
import org.eclipse.collections.impl.factory.primitive.ShortByteMaps;
import org.eclipse.collections.api.map.primitive.MutableShortByteMap;
import org.eclipse.collections.api.map.primitive.ShortByteMap;
import org.eclipse.collections.impl.utility.Iterate;

/**
 * MutableShortByteMapFactoryImpl is a factory implementation which creates instances of type {@link MutableShortByteMap}.
 * This file was automatically generated from template file mutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 6.0.
 */
public class MutableShortByteMapFactoryImpl implements MutableShortByteMapFactory
{
    public static final MutableShortByteMapFactory INSTANCE = new MutableShortByteMapFactoryImpl();

    @Override
    public MutableShortByteMap empty()
    {
        return new ShortByteHashMap(0);
    }

    @Override
    public MutableShortByteMap of()
    {
        return this.empty();
    }

    @Override
    public MutableShortByteMap with()
    {
        return this.empty();
    }

    @Override
    public MutableShortByteMap with(short key, byte value)
    {
        return ShortByteHashMap.newWithKeysValues(key, value);
    }

    @Override
    public MutableShortByteMap of(short key, byte value)
    {
        return this.with(key, value);
    }

    @Override
    public MutableShortByteMap of(short key1, byte value1, short key2, byte value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    @Override
    public MutableShortByteMap with(short key1, byte value1, short key2, byte value2)
    {
        return ShortByteHashMap.newWithKeysValues(key1, value1, key2, value2);
    }

    @Override
    public MutableShortByteMap of(short key1, byte value1, short key2, byte value2, short key3, byte value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableShortByteMap with(short key1, byte value1, short key2, byte value2, short key3, byte value3)
    {
        return ShortByteHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableShortByteMap of(
            short key1, byte value1, short key2, byte value2, short key3, byte value3, short key4, byte value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableShortByteMap with(
            short key1, byte value1, short key2, byte value2, short key3, byte value3, short key4, byte value4)
    {
        return ShortByteHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableShortByteMap ofInitialCapacity(int capacity)
    {
        return this.withInitialCapacity(capacity);
    }

    @Override
    public MutableShortByteMap withInitialCapacity(int capacity)
    {
        return new ShortByteHashMap(capacity);
    }

    @Override
    public MutableShortByteMap ofAll(ShortByteMap map)
    {
        return this.withAll(map);
    }

    @Override
    public MutableShortByteMap withAll(ShortByteMap map)
    {
        if (map.isEmpty())
        {
            return this.empty();
        }
        return new ShortByteHashMap(map);
    }

    @Override
    public <T> MutableShortByteMap from(Iterable<T> iterable, ShortFunction<? super T> keyFunction, ByteFunction<? super T> valueFunction)
    {
        MutableShortByteMap map = ShortByteMaps.mutable.empty();
        Iterate.forEach(iterable, each -> map.put(keyFunction.shortValueOf(each), valueFunction.byteValueOf(each)));
        return map;
    }
}
