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
import jdk.m3.collections.eclipse.api.block.function.primitive.IntFunction;
import jdk.m3.collections.eclipse.api.factory.map.primitive.MutableShortIntMapFactory;
import jdk.m3.collections.eclipse.impl.factory.primitive.ShortIntMaps;
import jdk.m3.collections.eclipse.api.map.primitive.MutableShortIntMap;
import jdk.m3.collections.eclipse.api.map.primitive.ShortIntMap;
import jdk.m3.collections.eclipse.impl.utility.Iterate;

/**
 * MutableShortIntMapFactoryImpl is a factory implementation which creates instances of type {@link MutableShortIntMap}.
 * This file was automatically generated from template file mutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 6.0.
 */
@SuppressWarnings("missing-explicit-ctor")
public class MutableShortIntMapFactoryImpl implements MutableShortIntMapFactory
{
    public static final MutableShortIntMapFactory INSTANCE = new MutableShortIntMapFactoryImpl();

    @Override
    public MutableShortIntMap empty()
    {
        return new ShortIntHashMap(0);
    }

    @Override
    public MutableShortIntMap of()
    {
        return this.empty();
    }

    @Override
    public MutableShortIntMap with()
    {
        return this.empty();
    }

    @Override
    public MutableShortIntMap with(short key, int value)
    {
        return ShortIntHashMap.newWithKeysValues(key, value);
    }

    @Override
    public MutableShortIntMap of(short key, int value)
    {
        return this.with(key, value);
    }

    @Override
    public MutableShortIntMap of(short key1, int value1, short key2, int value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    @Override
    public MutableShortIntMap with(short key1, int value1, short key2, int value2)
    {
        return ShortIntHashMap.newWithKeysValues(key1, value1, key2, value2);
    }

    @Override
    public MutableShortIntMap of(short key1, int value1, short key2, int value2, short key3, int value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableShortIntMap with(short key1, int value1, short key2, int value2, short key3, int value3)
    {
        return ShortIntHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableShortIntMap of(
            short key1, int value1, short key2, int value2, short key3, int value3, short key4, int value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableShortIntMap with(
            short key1, int value1, short key2, int value2, short key3, int value3, short key4, int value4)
    {
        return ShortIntHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableShortIntMap ofInitialCapacity(int capacity)
    {
        return this.withInitialCapacity(capacity);
    }

    @Override
    public MutableShortIntMap withInitialCapacity(int capacity)
    {
        return new ShortIntHashMap(capacity);
    }

    @Override
    public MutableShortIntMap ofAll(ShortIntMap map)
    {
        return this.withAll(map);
    }

    @Override
    public MutableShortIntMap withAll(ShortIntMap map)
    {
        if (map.isEmpty())
        {
            return this.empty();
        }
        return new ShortIntHashMap(map);
    }

    @Override
    public <T> MutableShortIntMap from(Iterable<T> iterable, ShortFunction<? super T> keyFunction, IntFunction<? super T> valueFunction)
    {
        MutableShortIntMap map = ShortIntMaps.mutable.empty();
        Iterate.forEach(iterable, each -> map.put(keyFunction.shortValueOf(each), valueFunction.intValueOf(each)));
        return map;
    }
}
