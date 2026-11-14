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
import org.eclipse.collections.api.block.function.primitive.BooleanFunction;
import org.eclipse.collections.api.factory.map.primitive.MutableShortBooleanMapFactory;
import org.eclipse.collections.impl.factory.primitive.ShortBooleanMaps;
import org.eclipse.collections.api.map.primitive.MutableShortBooleanMap;
import org.eclipse.collections.api.map.primitive.ShortBooleanMap;
import org.eclipse.collections.impl.utility.Iterate;

/**
 * MutableShortBooleanMapFactoryImpl is a factory implementation which creates instances of type {@link MutableShortBooleanMap}.
 * This file was automatically generated from template file mutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 6.0.
 */
public class MutableShortBooleanMapFactoryImpl implements MutableShortBooleanMapFactory
{
    public static final MutableShortBooleanMapFactory INSTANCE = new MutableShortBooleanMapFactoryImpl();

    @Override
    public MutableShortBooleanMap empty()
    {
        return new ShortBooleanHashMap(0);
    }

    @Override
    public MutableShortBooleanMap of()
    {
        return this.empty();
    }

    @Override
    public MutableShortBooleanMap with()
    {
        return this.empty();
    }

    @Override
    public MutableShortBooleanMap with(short key, boolean value)
    {
        return ShortBooleanHashMap.newWithKeysValues(key, value);
    }

    @Override
    public MutableShortBooleanMap of(short key, boolean value)
    {
        return this.with(key, value);
    }

    @Override
    public MutableShortBooleanMap of(short key1, boolean value1, short key2, boolean value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    @Override
    public MutableShortBooleanMap with(short key1, boolean value1, short key2, boolean value2)
    {
        return ShortBooleanHashMap.newWithKeysValues(key1, value1, key2, value2);
    }

    @Override
    public MutableShortBooleanMap of(short key1, boolean value1, short key2, boolean value2, short key3, boolean value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableShortBooleanMap with(short key1, boolean value1, short key2, boolean value2, short key3, boolean value3)
    {
        return ShortBooleanHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableShortBooleanMap of(
            short key1, boolean value1, short key2, boolean value2, short key3, boolean value3, short key4, boolean value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableShortBooleanMap with(
            short key1, boolean value1, short key2, boolean value2, short key3, boolean value3, short key4, boolean value4)
    {
        return ShortBooleanHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableShortBooleanMap ofInitialCapacity(int capacity)
    {
        return this.withInitialCapacity(capacity);
    }

    @Override
    public MutableShortBooleanMap withInitialCapacity(int capacity)
    {
        return new ShortBooleanHashMap(capacity);
    }

    @Override
    public MutableShortBooleanMap ofAll(ShortBooleanMap map)
    {
        return this.withAll(map);
    }

    @Override
    public MutableShortBooleanMap withAll(ShortBooleanMap map)
    {
        if (map.isEmpty())
        {
            return this.empty();
        }
        return new ShortBooleanHashMap(map);
    }

    @Override
    public <T> MutableShortBooleanMap from(Iterable<T> iterable, ShortFunction<? super T> keyFunction, BooleanFunction<? super T> valueFunction)
    {
        MutableShortBooleanMap map = ShortBooleanMaps.mutable.empty();
        Iterate.forEach(iterable, each -> map.put(keyFunction.shortValueOf(each), valueFunction.booleanValueOf(each)));
        return map;
    }
}
