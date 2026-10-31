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
import org.eclipse.collections.api.factory.map.primitive.MutableShortShortMapFactory;
import org.eclipse.collections.impl.factory.primitive.ShortShortMaps;
import org.eclipse.collections.api.map.primitive.MutableShortShortMap;
import org.eclipse.collections.api.map.primitive.ShortShortMap;
import org.eclipse.collections.impl.utility.Iterate;

/**
 * MutableShortShortMapFactoryImpl is a factory implementation which creates instances of type {@link MutableShortShortMap}.
 * This file was automatically generated from template file mutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 6.0.
 */
public class MutableShortShortMapFactoryImpl implements MutableShortShortMapFactory
{
    public static final MutableShortShortMapFactory INSTANCE = new MutableShortShortMapFactoryImpl();

    @Override
    public MutableShortShortMap empty()
    {
        return new ShortShortHashMap(0);
    }

    @Override
    public MutableShortShortMap of()
    {
        return this.empty();
    }

    @Override
    public MutableShortShortMap with()
    {
        return this.empty();
    }

    @Override
    public MutableShortShortMap with(short key, short value)
    {
        return ShortShortHashMap.newWithKeysValues(key, value);
    }

    @Override
    public MutableShortShortMap of(short key, short value)
    {
        return this.with(key, value);
    }

    @Override
    public MutableShortShortMap of(short key1, short value1, short key2, short value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    @Override
    public MutableShortShortMap with(short key1, short value1, short key2, short value2)
    {
        return ShortShortHashMap.newWithKeysValues(key1, value1, key2, value2);
    }

    @Override
    public MutableShortShortMap of(short key1, short value1, short key2, short value2, short key3, short value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableShortShortMap with(short key1, short value1, short key2, short value2, short key3, short value3)
    {
        return ShortShortHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableShortShortMap of(
            short key1, short value1, short key2, short value2, short key3, short value3, short key4, short value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableShortShortMap with(
            short key1, short value1, short key2, short value2, short key3, short value3, short key4, short value4)
    {
        return ShortShortHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableShortShortMap ofInitialCapacity(int capacity)
    {
        return this.withInitialCapacity(capacity);
    }

    @Override
    public MutableShortShortMap withInitialCapacity(int capacity)
    {
        return new ShortShortHashMap(capacity);
    }

    @Override
    public MutableShortShortMap ofAll(ShortShortMap map)
    {
        return this.withAll(map);
    }

    @Override
    public MutableShortShortMap withAll(ShortShortMap map)
    {
        if (map.isEmpty())
        {
            return this.empty();
        }
        return new ShortShortHashMap(map);
    }

    @Override
    public <T> MutableShortShortMap from(Iterable<T> iterable, ShortFunction<? super T> keyFunction, ShortFunction<? super T> valueFunction)
    {
        MutableShortShortMap map = ShortShortMaps.mutable.empty();
        Iterate.forEach(iterable, each -> map.put(keyFunction.shortValueOf(each), valueFunction.shortValueOf(each)));
        return map;
    }
}
