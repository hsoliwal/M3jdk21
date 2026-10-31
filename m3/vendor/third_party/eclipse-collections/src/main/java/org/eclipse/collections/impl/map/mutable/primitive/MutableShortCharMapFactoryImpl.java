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
import org.eclipse.collections.api.block.function.primitive.CharFunction;
import org.eclipse.collections.api.factory.map.primitive.MutableShortCharMapFactory;
import org.eclipse.collections.impl.factory.primitive.ShortCharMaps;
import org.eclipse.collections.api.map.primitive.MutableShortCharMap;
import org.eclipse.collections.api.map.primitive.ShortCharMap;
import org.eclipse.collections.impl.utility.Iterate;

/**
 * MutableShortCharMapFactoryImpl is a factory implementation which creates instances of type {@link MutableShortCharMap}.
 * This file was automatically generated from template file mutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 6.0.
 */
public class MutableShortCharMapFactoryImpl implements MutableShortCharMapFactory
{
    public static final MutableShortCharMapFactory INSTANCE = new MutableShortCharMapFactoryImpl();

    @Override
    public MutableShortCharMap empty()
    {
        return new ShortCharHashMap(0);
    }

    @Override
    public MutableShortCharMap of()
    {
        return this.empty();
    }

    @Override
    public MutableShortCharMap with()
    {
        return this.empty();
    }

    @Override
    public MutableShortCharMap with(short key, char value)
    {
        return ShortCharHashMap.newWithKeysValues(key, value);
    }

    @Override
    public MutableShortCharMap of(short key, char value)
    {
        return this.with(key, value);
    }

    @Override
    public MutableShortCharMap of(short key1, char value1, short key2, char value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    @Override
    public MutableShortCharMap with(short key1, char value1, short key2, char value2)
    {
        return ShortCharHashMap.newWithKeysValues(key1, value1, key2, value2);
    }

    @Override
    public MutableShortCharMap of(short key1, char value1, short key2, char value2, short key3, char value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableShortCharMap with(short key1, char value1, short key2, char value2, short key3, char value3)
    {
        return ShortCharHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableShortCharMap of(
            short key1, char value1, short key2, char value2, short key3, char value3, short key4, char value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableShortCharMap with(
            short key1, char value1, short key2, char value2, short key3, char value3, short key4, char value4)
    {
        return ShortCharHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableShortCharMap ofInitialCapacity(int capacity)
    {
        return this.withInitialCapacity(capacity);
    }

    @Override
    public MutableShortCharMap withInitialCapacity(int capacity)
    {
        return new ShortCharHashMap(capacity);
    }

    @Override
    public MutableShortCharMap ofAll(ShortCharMap map)
    {
        return this.withAll(map);
    }

    @Override
    public MutableShortCharMap withAll(ShortCharMap map)
    {
        if (map.isEmpty())
        {
            return this.empty();
        }
        return new ShortCharHashMap(map);
    }

    @Override
    public <T> MutableShortCharMap from(Iterable<T> iterable, ShortFunction<? super T> keyFunction, CharFunction<? super T> valueFunction)
    {
        MutableShortCharMap map = ShortCharMaps.mutable.empty();
        Iterate.forEach(iterable, each -> map.put(keyFunction.shortValueOf(each), valueFunction.charValueOf(each)));
        return map;
    }
}
